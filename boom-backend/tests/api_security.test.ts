import { describe, it, before } from "node:test";
import assert from "node:assert";
import { createServer, BoomDatabase } from "../src/server.ts";
import { createInProcessCaller } from "./in_process_caller.ts";
import type { MockResponse } from "./in_process_caller.ts";

describe("BOOM Security & API Acceptance Tests", () => {
  let db: BoomDatabase;
  let request: (method: string, path: string, body?: any, headers?: Record<string, string>) => Promise<MockResponse>;

  before(() => {
    db = new BoomDatabase();
    const result = createServer(db, {
      port: 8080,
      jwtSecret: "test-secret-key-32-chars-long-boom-test",
      corsOrigin: "*"
    });
    request = createInProcessCaller(result.requestListener);
  });

  // 1. Health check & security headers
  it("GET /health returns 200 and security headers", async () => {
    const res = await request("GET", "/health");
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.status, "UP");
    assert.strictEqual(res.headers["x-content-type-options"], "nosniff");
    assert.strictEqual(res.headers["x-frame-options"], "DENY");
  });

  // 2. Registration and unverified login prevention (AUTH-01, AUTH-02, AUTH-03)
  it("AUTH: Registration creates pending user and prevents login before verification", async () => {
    const regRes = await request("POST", "/api/v1/auth/register", {
      email: "dj.host@boom.party",
      phoneNumber: "+15551234567",
      displayName: "DJ Apex",
      password: "SuperSecurePassword123!"
    });

    assert.strictEqual(regRes.status, 201);
    assert.strictEqual(regRes.body.success, true);
    assert.strictEqual(regRes.body.data.accountStatus, "PENDING_VERIFICATION");

    // Attempt login before verifying -> Must return 403 Forbidden
    const loginRes = await request("POST", "/api/v1/auth/login", {
      identifier: "dj.host@boom.party",
      password: "SuperSecurePassword123!",
      deviceReference: "pixel-8-pro"
    });

    assert.strictEqual(loginRes.status, 403);
    assert.strictEqual(loginRes.body.error.code, "VERIFICATION_REQUIRED");
  });

  // 3. Contact verification activates account (AUTH-02, AUTH-03)
  it("AUTH: Validating email and phone OTPs activates account and permits sign-in", async () => {
    const emailToken = db.verificationTokens.get("dj.host@boom.party:EMAIL");
    const phoneToken = db.verificationTokens.get("+15551234567:PHONE");

    assert.ok(emailToken, "Email token must be generated");
    assert.ok(phoneToken, "Phone token must be generated");

    // Verify Email
    const vEmail = await request("POST", "/api/v1/auth/verify-contact", {
      identifier: "dj.host@boom.party",
      contactType: "EMAIL",
      code: emailToken.code
    });
    assert.strictEqual(vEmail.status, 200);

    // Verify Phone
    const vPhone = await request("POST", "/api/v1/auth/verify-contact", {
      identifier: "+15551234567",
      contactType: "PHONE",
      code: phoneToken.code
    });
    assert.strictEqual(vPhone.status, 200);

    // Login after verification -> Succeeds with JWT token
    const loginRes = await request("POST", "/api/v1/auth/login", {
      identifier: "dj.host@boom.party",
      password: "SuperSecurePassword123!",
      deviceReference: "pixel-8-pro"
    });

    assert.strictEqual(loginRes.status, 200);
    assert.ok(loginRes.body.data.accessToken);
    assert.ok(loginRes.body.data.refreshToken);
  });

  // 4. Generic error messages on invalid authentication (AUTH-08)
  it("AUTH-08: Generic error on nonexistent account prevents enumeration", async () => {
    const res = await request("POST", "/api/v1/auth/login", {
      identifier: "unknown.stranger@random.com",
      password: "SomePassword123!",
      deviceReference: "unknown-device"
    });

    assert.strictEqual(res.status, 401);
    assert.strictEqual(res.body.error.code, "INVALID_CREDENTIALS");
    assert.strictEqual(res.body.error.message, "Invalid credentials");
  });

  // 4b. AUTH-04: Throttling after 5 invalid attempts
  it("AUTH-04: Verification codes are rate-limited and throttled after 5 failed attempts", async () => {
    await request("POST", "/api/v1/auth/register", {
      email: "victim@boom.party",
      phoneNumber: "+15559990000",
      displayName: "Victim",
      password: "Password123!"
    });

    // Make 5 failed attempts with wrong code "000000"
    for (let i = 0; i < 5; i++) {
      const res = await request("POST", "/api/v1/auth/verify-contact", {
        identifier: "victim@boom.party",
        contactType: "EMAIL",
        code: "000000"
      });
      assert.strictEqual(res.status, 400);
      assert.strictEqual(res.body.error.code, "INVALID_OTP");
    }

    // 6th attempt must return 429 OTP_THROTTLED
    const throttledRes = await request("POST", "/api/v1/auth/verify-contact", {
      identifier: "victim@boom.party",
      contactType: "EMAIL",
      code: "000000"
    });
    assert.strictEqual(throttledRes.status, 429);
    assert.strictEqual(throttledRes.body.error.code, "OTP_THROTTLED");
  });

  // 5. Party creation and join workflow (HOST-02, JOIN-01, JOIN-02)
  it("PARTY: Host can create party and approve guest participant", async () => {
    // 1. Host logs in
    const hostLogin = await request("POST", "/api/v1/auth/login", {
      identifier: "dj.host@boom.party",
      password: "SuperSecurePassword123!",
      deviceReference: "pixel-8-pro"
    });
    const hostToken = hostLogin.body.data.accessToken;

    // 2. Host creates party
    const createPartyRes = await request(
      "POST",
      "/api/v1/parties",
      { title: "Friday Night Bass", maxParticipants: 5, requireHostApproval: true },
      { Authorization: `Bearer ${hostToken}` }
    );
    assert.strictEqual(createPartyRes.status, 201);
    const party = createPartyRes.body.data;
    assert.ok(party.partyId);
    assert.ok(party.shortCode);

    // 3. Register and verify Participant Alice
    await request("POST", "/api/v1/auth/register", {
      email: "alice@boom.party",
      phoneNumber: "+15559876543",
      displayName: "Alice",
      password: "AlicePassword123!"
    });
    const aliceEmailToken = db.verificationTokens.get("alice@boom.party:EMAIL");
    const alicePhoneToken = db.verificationTokens.get("+15559876543:PHONE");
    await request("POST", "/api/v1/auth/verify-contact", { identifier: "alice@boom.party", contactType: "EMAIL", code: aliceEmailToken.code });
    await request("POST", "/api/v1/auth/verify-contact", { identifier: "+15559876543", contactType: "PHONE", code: alicePhoneToken.code });

    const aliceLogin = await request("POST", "/api/v1/auth/login", {
      identifier: "alice@boom.party",
      password: "AlicePassword123!",
      deviceReference: "galaxy-s24"
    });
    const aliceToken = aliceLogin.body.data.accessToken;
    const aliceUserId = aliceLogin.body.data.userId;

    // 4. Alice requests to join party
    const joinRes = await request(
      "POST",
      `/api/v1/parties/${party.partyId}/join`,
      { shortCode: party.shortCode, userDisplayName: "Alice" },
      { Authorization: `Bearer ${aliceToken}` }
    );
    assert.strictEqual(joinRes.status, 200);
    assert.strictEqual(joinRes.body.data.status, "PENDING_APPROVAL");

    // 5. Host approves Alice
    const approveRes = await request(
      "POST",
      `/api/v1/parties/${party.partyId}/approve`,
      { participantUserId: aliceUserId },
      { Authorization: `Bearer ${hostToken}` }
    );
    assert.strictEqual(approveRes.status, 200);
    assert.strictEqual(approveRes.body.data.approved, true);
  });

  // 6. Object-level chat authorization and privacy test (CHAT-04, CHAT-07, CHAT-08)
  it("CHAT PRIVACY: Participant Bob cannot see Alice's private message to Host", async () => {
    // Register and approve Participant Bob
    await request("POST", "/api/v1/auth/register", {
      email: "bob@boom.party",
      phoneNumber: "+15554443322",
      displayName: "Bob",
      password: "BobPassword123!"
    });
    const bEmail = db.verificationTokens.get("bob@boom.party:EMAIL");
    const bPhone = db.verificationTokens.get("+15554443322:PHONE");
    await request("POST", "/api/v1/auth/verify-contact", { identifier: "bob@boom.party", contactType: "EMAIL", code: bEmail.code });
    await request("POST", "/api/v1/auth/verify-contact", { identifier: "+15554443322", contactType: "PHONE", code: bPhone.code });

    const bobLogin = await request("POST", "/api/v1/auth/login", {
      identifier: "bob@boom.party",
      password: "BobPassword123!",
      deviceReference: "moto-g"
    });
    const bobToken = bobLogin.body.data.accessToken;
    const bobUserId = bobLogin.body.data.userId;

    const hostLogin = await request("POST", "/api/v1/auth/login", {
      identifier: "dj.host@boom.party",
      password: "SuperSecurePassword123!",
      deviceReference: "pixel-8-pro"
    });
    const hostToken = hostLogin.body.data.accessToken;

    const aliceLogin = await request("POST", "/api/v1/auth/login", {
      identifier: "alice@boom.party",
      password: "AlicePassword123!",
      deviceReference: "galaxy-s24"
    });
    const aliceToken = aliceLogin.body.data.accessToken;

    const partyId = Array.from(db.parties.keys())[0];
    const party = db.parties.get(partyId);

    // Approve Bob
    const bobJoinRes = await request("POST", `/api/v1/parties/${partyId}/join`, { shortCode: party.shortCode }, { Authorization: `Bearer ${bobToken}` });
    assert.strictEqual(bobJoinRes.status, 200);
    const bobApproveRes = await request("POST", `/api/v1/parties/${partyId}/approve`, { participantUserId: bobUserId }, { Authorization: `Bearer ${hostToken}` });
    assert.strictEqual(bobApproveRes.status, 200);

    // 1. Alice sends public message
    await request(
      "POST",
      `/api/v1/parties/${partyId}/chat`,
      { content: "Hey everyone! Great party!", visibility: "EVERYONE" },
      { Authorization: `Bearer ${aliceToken}` }
    );

    // 2. Alice sends PRIVATE message to Host (HOST_ONLY)
    await request(
      "POST",
      `/api/v1/parties/${partyId}/chat`,
      { content: "Hey DJ, my friend is arriving in 5 mins", visibility: "HOST_ONLY" },
      { Authorization: `Bearer ${aliceToken}` }
    );

    // 3. Bob retrieves chat history
    const bobChatRes = await request(
      "GET",
      `/api/v1/parties/${partyId}/chat`,
      undefined,
      { Authorization: `Bearer ${bobToken}` }
    );
    assert.strictEqual(bobChatRes.status, 200);
    const bobMessages = bobChatRes.body.data;

    // Bob MUST only see the public message
    assert.strictEqual(bobMessages.length, 1);
    assert.strictEqual(bobMessages[0].content, "Hey everyone! Great party!");

    // Verify Bob does NOT see Alice's private message
    const hasAlicePrivateMsg = bobMessages.some((m: any) => m.content.includes("friend is arriving"));
    assert.strictEqual(hasAlicePrivateMsg, false, "Bob must NEVER receive Alice's private message to the Host!");

    // 4. Host retrieves chat history
    const hostChatRes = await request(
      "GET",
      `/api/v1/parties/${partyId}/chat`,
      undefined,
      { Authorization: `Bearer ${hostToken}` }
    );
    assert.strictEqual(hostChatRes.status, 200);
    const hostMessages = hostChatRes.body.data;

    // Host sees BOTH public message and Alice's private message
    assert.strictEqual(hostMessages.length, 2);
    const hostSawPrivateMsg = hostMessages.some((m: any) => m.content.includes("friend is arriving"));
    assert.strictEqual(hostSawPrivateMsg, true, "Host MUST receive participant private messages!");
  });

  // 7. BOOM Guardian Security Events Ingestion & Privacy (SEC-01, SEC-03)
  it("GUARDIAN: Security events are ingested and strictly redact sensitive credentials", async () => {
    const rawAlert = {
      eventType: "AUTH_FAILURE",
      severity: "MEDIUM",
      actorReference: "anon-device-19",
      result: "BLOCKED",
      actionTaken: "RATE_LIMITED",
      details: "Attempted password: MySecretPassword123 with token: eyJhbGciOiJIUzI1NiJ9"
    };

    const res = await request("POST", "/api/v1/guardian/events", rawAlert);
    assert.strictEqual(res.status, 201);
    assert.strictEqual(res.body.success, true);

    const savedEvent = Array.from(db.securityEvents.values())[0];
    assert.ok(savedEvent);
    assert.strictEqual(savedEvent.details.includes("MySecretPassword123"), false, "Password must be redacted!");
    assert.strictEqual(savedEvent.details.includes("password:[REDACTED]"), true);
  });
});
