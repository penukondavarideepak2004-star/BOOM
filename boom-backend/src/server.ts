import http from "node:http";
import fs from "node:fs";
import pathModule from "node:path";
import { SecurityCrypto } from "./crypto.ts";

export interface Config {
  port: number;
  jwtSecret: string;
  corsOrigin: string;
}

export const defaultConfig: Config = {
  port: parseInt(process.env.PORT || "8080", 10),
  jwtSecret: process.env.JWT_SECRET || "boom-development-secret-key-32-chars-long!",
  corsOrigin: process.env.CORS_ORIGIN || "*"
};

// State Store (In-Memory representation of PostgreSQL tables with relational integrity)
export class BoomDatabase {
  users = new Map<string, any>();
  verificationTokens = new Map<string, any>();
  parties = new Map<string, any>();
  partyMembers = new Map<string, any>();
  chatMessages = new Map<string, any>();
  activitySessions = new Map<string, any>();
  securityEvents = new Map<string, any>();
  rateLimitMap = new Map<string, { count: number; resetAt: number }>();

  clear() {
    this.users.clear();
    this.verificationTokens.clear();
    this.parties.clear();
    this.partyMembers.clear();
    this.chatMessages.clear();
    this.activitySessions.clear();
    this.securityEvents.clear();
    this.rateLimitMap.clear();
  }

  isRateLimited(key: string, max: number, windowMs: number): boolean {
    const now = Date.now();
    const entry = this.rateLimitMap.get(key);
    if (!entry || now > entry.resetAt) {
      this.rateLimitMap.set(key, { count: 1, resetAt: now + windowMs });
      return false;
    }
    entry.count++;
    return entry.count > max;
  }
}

export function createServer(customDb?: BoomDatabase, config: Config = defaultConfig) {
  const db = customDb || new BoomDatabase();

  const requestListener = async (req: any, res: any) => {
    // 1. Security Headers
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    res.setHeader("Access-Control-Allow-Origin", config.corsOrigin);
    res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Device-Reference");

    if (req.method === "OPTIONS") {
      res.writeHead(204);
      res.end();
      return;
    }

    const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);
    const path = url.pathname;

    // Helper to send JSON
    const sendJson = (status: number, data: any) => {
      res.writeHead(status, { "Content-Type": "application/json" });
      res.end(JSON.stringify(data));
    };

    // Helper to parse body
    const parseBody = async (): Promise<any> => {
      return new Promise((resolve, reject) => {
        let body = "";
        req.on("data", (chunk) => {
          body += chunk;
          if (body.length > 1024 * 1024) {
            // 1MB limit
            reject(new Error("PAYLOAD_TOO_LARGE"));
          }
        });
        req.on("end", () => {
          if (!body) return resolve({});
          try {
            resolve(JSON.parse(body));
          } catch {
            reject(new Error("INVALID_JSON"));
          }
        });
      });
    };

    // Helper for auth validation
    const getAuthenticatedUser = (): any => {
      const auth = req.headers.authorization;
      if (!auth || !auth.startsWith("Bearer ")) return null;
      const token = auth.substring(7);
      return SecurityCrypto.verifyToken(token, config.jwtSecret);
    };

    try {
      // 2. Health & Web Experience endpoints
      if (req.method === "GET" && (path === "/" || path === "/index.html" || path === "/app")) {
        const potentialPaths = [
          pathModule.resolve(import.meta.dirname || "", "../public/index.html"),
          "/Users/penukondavarideepak/Documents/antigravity/sharp-kepler/boom-backend/public/index.html",
          pathModule.resolve(process.cwd(), "boom-backend/public/index.html"),
          pathModule.resolve(process.cwd(), "public/index.html"),
          "/Users/penukondavarideepak/.gemini/antigravity/brain/f650f466-2c56-49f1-bf03-ab518f2660c5/boom_experience.html"
        ];
        for (const p of potentialPaths) {
          try {
            if (p && fs.existsSync(p)) {
              const html = fs.readFileSync(p, "utf8");
              res.writeHead(200, {
                "Content-Type": "text/html; charset=utf-8",
                "X-Content-Type-Options": "nosniff"
              });
              res.end(html);
              return;
            }
          } catch (_) {}
        }
      }

      if (req.method === "GET" && path === "/health") {
        sendJson(200, {
          status: "UP",
          service: "boom-backend",
          version: "1.0.0",
          timestamp: new Date().toISOString()
        });
        return;
      }

      // 3. AUTH ROUTES
      // POST /api/v1/auth/register
      if (req.method === "POST" && path === "/api/v1/auth/register") {
        const body = await parseBody();
        const { email, phoneNumber, displayName, password } = body;

        if (!email || !phoneNumber || !displayName || !password) {
          sendJson(400, { success: false, error: { code: "MISSING_FIELDS", message: "All registration fields required" } });
          return;
        }

        // Check rate limiting on registration IP
        if (db.isRateLimited(req.socket.remoteAddress || "ip", 10, 60000)) {
          sendJson(429, { success: false, error: { code: "RATE_LIMITED", message: "Too many registration attempts" } });
          return;
        }

        // Generic conflict message to avoid account harvesting per AUTH-08
        for (const user of db.users.values()) {
          if (user.email === email || user.phoneNumber === phoneNumber) {
            sendJson(409, { success: false, error: { code: "REGISTRATION_FAILED", message: "Registration could not be completed" } });
            return;
          }
        }

        const userId = "usr-" + Math.random().toString(36).substring(2, 10);
        const passwordHash = await SecurityCrypto.hashPassword(password);

        const newUser = {
          userId,
          email,
          phoneNumber,
          displayName,
          passwordHash,
          emailVerified: false,
          phoneVerified: false,
          accountStatus: "PENDING_VERIFICATION",
          role: "VERIFIED_USER",
          mfaEnabled: false,
          createdAt: new Date().toISOString()
        };
        db.users.set(userId, newUser);

        // Issue single-use OTP for email & phone
        const emailOtp = SecurityCrypto.generateOtp();
        const phoneOtp = SecurityCrypto.generateOtp();
        db.verificationTokens.set(`${email}:EMAIL`, { code: emailOtp, expiresAt: Date.now() + 300000, attempts: 0 });
        db.verificationTokens.set(`${phoneNumber}:PHONE`, { code: phoneOtp, expiresAt: Date.now() + 300000, attempts: 0 });

        sendJson(201, {
          success: true,
          data: {
            userId: newUser.userId,
            email: newUser.email,
            phoneNumber: newUser.phoneNumber,
            displayName: newUser.displayName,
            accountStatus: newUser.accountStatus
          }
        });
        return;
      }

      // POST /api/v1/auth/verify-contact
      if (req.method === "POST" && path === "/api/v1/auth/verify-contact") {
        const body = await parseBody();
        const { identifier, contactType, code } = body;

        const tokenEntry = db.verificationTokens.get(`${identifier}:${contactType}`);
        if (!tokenEntry || Date.now() > tokenEntry.expiresAt) {
          sendJson(400, { success: false, error: { code: "OTP_EXPIRED", message: "Verification code expired or invalid" } });
          return;
        }

        if (tokenEntry.attempts >= 5) {
          sendJson(429, { success: false, error: { code: "OTP_THROTTLED", message: "Maximum verification attempts exceeded" } });
          return;
        }

        tokenEntry.attempts++;

        if (tokenEntry.code !== code) {
          sendJson(400, { success: false, error: { code: "INVALID_OTP", message: "Invalid verification code" } });
          return;
        }

        // Code valid -> mark verified
        db.verificationTokens.delete(`${identifier}:${contactType}`);
        for (const user of db.users.values()) {
          if (user.email === identifier) user.emailVerified = true;
          if (user.phoneNumber === identifier) user.phoneVerified = true;
          if (user.emailVerified && user.phoneVerified) {
            user.accountStatus = "ACTIVE";
          }
        }

        sendJson(200, { success: true, data: { verified: true } });
        return;
      }

      // POST /api/v1/auth/login
      if (req.method === "POST" && path === "/api/v1/auth/login") {
        const body = await parseBody();
        const { identifier, password, deviceReference } = body;

        // Rate limit by identifier to stop brute-forcing
        if (db.isRateLimited(`login:${identifier}`, 5, 300000)) {
          sendJson(429, { success: false, error: { code: "RATE_LIMITED", message: "Account temporarily locked. Please wait 5 minutes." } });
          return;
        }

        let foundUser: any = null;
        for (const user of db.users.values()) {
          if (user.email === identifier || user.phoneNumber === identifier) {
            foundUser = user;
            break;
          }
        }

        // Generic error message per AUTH-08
        if (!foundUser) {
          sendJson(401, { success: false, error: { code: "INVALID_CREDENTIALS", message: "Invalid credentials" } });
          return;
        }

        const passwordMatches = await SecurityCrypto.verifyPassword(password, foundUser.passwordHash);
        if (!passwordMatches) {
          sendJson(401, { success: false, error: { code: "INVALID_CREDENTIALS", message: "Invalid credentials" } });
          return;
        }

        // Disallow dashboard access if unverified
        if (foundUser.accountStatus === "PENDING_VERIFICATION") {
          sendJson(403, { success: false, error: { code: "VERIFICATION_REQUIRED", message: "Please verify your email and phone number first" } });
          return;
        }

        const accessToken = SecurityCrypto.signToken(
          { userId: foundUser.userId, email: foundUser.email, role: foundUser.role },
          config.jwtSecret,
          900 // 15 minutes
        );
        const refreshToken = SecurityCrypto.signToken(
          { userId: foundUser.userId, deviceReference },
          config.jwtSecret,
          7 * 24 * 3600 // 7 days
        );

        sendJson(200, {
          success: true,
          data: {
            sessionId: "sess-" + Math.random().toString(36).substring(2, 10),
            userId: foundUser.userId,
            accessToken,
            refreshToken,
            expiresInSeconds: 900
          }
        });
        return;
      }

      // 4. PARTY LIFECYCLE ROUTES (require authentication)
      // POST /api/v1/parties
      if (req.method === "POST" && path === "/api/v1/parties") {
        const user = getAuthenticatedUser();
        if (!user) {
          sendJson(401, { success: false, error: { code: "UNAUTHORIZED", message: "Authentication required" } });
          return;
        }

        const body = await parseBody();
        const partyId = "pty-" + Math.random().toString(36).substring(2, 10);
        const shortCode = SecurityCrypto.generatePartyShortCode();
        const qrPayload = `boom://party?id=${partyId}&code=${shortCode}`;

        const party = {
          partyId,
          hostUserId: user.userId,
          title: body.title || "BOOM Session",
          shortCode,
          qrPayload,
          state: "ACTIVE",
          settings: {
            maxParticipants: body.maxParticipants || 10,
            requireHostApproval: body.requireHostApproval ?? true
          },
          createdAt: Date.now(),
          expiresAt: Date.now() + (4 * 3600 * 1000)
        };
        db.parties.set(partyId, party);

        // Host is automatically added as host participant
        db.partyMembers.set(`${partyId}:${user.userId}`, {
          partyId,
          userId: user.userId,
          role: "HOST",
          status: "CONNECTED"
        });

        sendJson(201, { success: true, data: party });
        return;
      }

      // POST /api/v1/parties/:partyId/join
      if (req.method === "POST" && path.includes("/join")) {
        const user = getAuthenticatedUser();
        if (!user) {
          sendJson(401, { success: false, error: { code: "UNAUTHORIZED", message: "Authentication required" } });
          return;
        }

        const partyId = path.split("/")[4];
        const party = db.parties.get(partyId);
        if (!party || party.state !== "ACTIVE") {
          sendJson(404, { success: false, error: { code: "PARTY_NOT_FOUND", message: "Party does not exist or has ended" } });
          return;
        }

        const body = await parseBody();
        if (body.shortCode && body.shortCode !== party.shortCode) {
          sendJson(403, { success: false, error: { code: "INVALID_SHORT_CODE", message: "Incorrect party join code" } });
          return;
        }

        const status = party.settings.requireHostApproval ? "PENDING_APPROVAL" : "CONNECTED";
        const member = {
          partyId,
          userId: user.userId,
          displayName: body.userDisplayName || user.email,
          role: "PARTICIPANT",
          status
        };
        db.partyMembers.set(`${partyId}:${user.userId}`, member);

        sendJson(200, { success: true, data: { status, partyId } });
        return;
      }

      // POST /api/v1/parties/:partyId/approve (Host only)
      if (req.method === "POST" && path.includes("/approve")) {
        const user = getAuthenticatedUser();
        if (!user) {
          sendJson(401, { success: false, error: { code: "UNAUTHORIZED", message: "Authentication required" } });
          return;
        }

        const partyId = path.split("/")[4];
        const party = db.parties.get(partyId);
        if (!party || party.hostUserId !== user.userId) {
          sendJson(403, { success: false, error: { code: "FORBIDDEN", message: "Only the party host can approve participants" } });
          return;
        }

        const body = await parseBody();
        const memberKey = `${partyId}:${body.participantUserId}`;
        const member = db.partyMembers.get(memberKey);
        if (!member) {
          sendJson(404, { success: false, error: { code: "MEMBER_NOT_FOUND", message: "Participant request not found" } });
          return;
        }

        member.status = "CONNECTED";
        sendJson(200, { success: true, data: { approved: true } });
        return;
      }

      // 5. CHAT WITH STRICT OBJECT-LEVEL VISIBILITY ENFORCEMENT
      // GET /api/v1/parties/:partyId/chat
      if (req.method === "GET" && path.includes("/chat")) {
        const user = getAuthenticatedUser();
        if (!user) {
          sendJson(401, { success: false, error: { code: "UNAUTHORIZED", message: "Authentication required" } });
          return;
        }

        const partyId = path.split("/")[4];
        const party = db.parties.get(partyId);
        if (!party) {
          sendJson(404, { success: false, error: { code: "PARTY_NOT_FOUND", message: "Party not found" } });
          return;
        }

        // Verify membership
        const member = db.partyMembers.get(`${partyId}:${user.userId}`);
        if (!member || member.status !== "CONNECTED") {
          sendJson(403, { success: false, error: { code: "FORBIDDEN", message: "You are not an active member of this party" } });
          return;
        }

        const isHost = party.hostUserId === user.userId;
        const visibleMessages: any[] = [];

        // STRICT CHAT PRIVACY FILTER (CHAT-04, CHAT-08)
        for (const msg of db.chatMessages.values()) {
          if (msg.partyId === partyId) {
            if (msg.visibility === "EVERYONE" || msg.visibility === "HOST_ANNOUNCEMENT") {
              visibleMessages.push(msg);
            } else if (msg.visibility === "HOST_ONLY") {
              // Participant only sees their OWN private conversation with host.
              // Host sees all private conversations directed to the host.
              if (msg.senderUserId === user.userId || isHost || msg.recipientUserId === user.userId) {
                visibleMessages.push(msg);
              }
            }
          }
        }

        sendJson(200, { success: true, data: visibleMessages });
        return;
      }

      // POST /api/v1/parties/:partyId/chat
      if (req.method === "POST" && path.includes("/chat")) {
        const user = getAuthenticatedUser();
        if (!user) {
          sendJson(401, { success: false, error: { code: "UNAUTHORIZED", message: "Authentication required" } });
          return;
        }

        const partyId = path.split("/")[4];
        const party = db.parties.get(partyId);
        const member = db.partyMembers.get(`${partyId}:${user.userId}`);
        if (!party || !member || member.status !== "CONNECTED") {
          sendJson(403, { success: false, error: { code: "FORBIDDEN", message: "Not permitted to send messages" } });
          return;
        }

        const body = await parseBody();
        const isHost = party.hostUserId === user.userId;
        const visibility = body.visibility === "HOST_ONLY" ? "HOST_ONLY" : "EVERYONE";

        const messageId = "msg-" + Math.random().toString(36).substring(2, 10);
        const msg = {
          messageId,
          partyId,
          senderUserId: user.userId,
          recipientUserId: visibility === "HOST_ONLY" ? (isHost ? body.recipientUserId : party.hostUserId) : null,
          isFromHost: isHost,
          visibility,
          content: body.content,
          deliveryState: "SENT",
          timestampEpochMs: Date.now()
        };

        db.chatMessages.set(messageId, msg);
        sendJson(201, { success: true, data: msg });
        return;
      }

      // 6. BOOM GUARDIAN SECURITY EVENT INGESTION (SEC-01)
      if (req.method === "POST" && path === "/api/v1/guardian/events") {
        const body = await parseBody();
        const eventId = "evt-" + Math.random().toString(36).substring(2, 10);

        // Enforce privacy filtering: never store raw passwords or tokens
        const sanitizedDetails = (body.details || "")
          .replace(/(password|otp|token|bearer|secret)\s*[:=]\s*[^\s,;]+/gi, "$1:[REDACTED]")
          .replace(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}/g, "[REDACTED_EMAIL]");

        const event = {
          eventId,
          timestampEpochMs: Date.now(),
          eventType: body.eventType,
          severity: body.severity || "INFO",
          actorReference: body.actorReference || "anonymous",
          partyId: body.partyId || null,
          result: body.result || "RECORDED",
          actionTaken: body.actionTaken || "LOGGED",
          details: sanitizedDetails
        };
        db.securityEvents.set(eventId, event);

        sendJson(201, { success: true, data: { recorded: true, eventId } });
        return;
      }

      // Not Found
      sendJson(404, { success: false, error: { code: "NOT_FOUND", message: `Route ${path} not found` } });
    } catch (err: any) {
      console.error("[Server Error]:", err);
      sendJson(500, {
        success: false,
        error: { code: "INTERNAL_ERROR", message: "An internal server error occurred" }
      });
    }
  };

  const server = http.createServer(requestListener);

  return { server, db, requestListener };
}

// Standalone start if run directly
if (process.argv[1]?.endsWith("server.ts") || process.argv[1]?.endsWith("server.js")) {
  const { server } = createServer();
  server.listen(defaultConfig.port, () => {
    console.log(`[BOOM Backend] Server active on port ${defaultConfig.port}`);
  });
}
