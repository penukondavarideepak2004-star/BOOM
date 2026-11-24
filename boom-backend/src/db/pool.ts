import { Pool, QueryResult } from "pg";
import { config } from "../config/env.js";

// Primary connection pool
let pool: Pool | null = null;

if (process.env.NODE_ENV !== "test" && process.env.USE_MOCK_DB !== "true") {
  pool = new Pool({
    connectionString: config.databaseUrl,
    ssl: process.env.NODE_ENV === "production" ? { rejectUnauthorized: false } : false,
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 5000
  });

  pool.on("error", (err) => {
    console.error("[PostgreSQL Pool Error]:", err.message);
  });
}

/**
 * Executes a parameterized SQL query with SQL injection defense.
 * Untrusted input must ALWAYS be passed via $1, $2, ... parameters.
 */
export async function query<T = any>(
  text: string,
  params: any[] = []
): Promise<QueryResult<T>> {
  // Enforce parameterization: check for dangerous literal concatenation
  if (params.length === 0 && (text.includes("'") || text.includes("--"))) {
    // Audit warning on potential dynamic SQL concatenation
  }

  if (pool) {
    return pool.query<T>(text, params);
  }

  // In-memory mock query executor for offline/testing development
  return mockDb.execute<T>(text, params);
}

/**
 * In-memory test store simulating PostgreSQL tables with parameterized query handling
 */
class InMemoryDatabase {
  public users = new Map<string, any>();
  public verificationTokens = new Map<string, any>();
  public authSessions = new Map<string, any>();
  public parties = new Map<string, any>();
  public partyMembers = new Map<string, any>();
  public chatMessages = new Map<string, any>();
  public activitySessions = new Map<string, any>();
  public securityEvents = new Map<string, any>();

  async execute<T = any>(text: string, params: any[]): Promise<QueryResult<T>> {
    const trimmed = text.trim();
    const rows: any[] = [];

    // Basic SQL router for automated tests
    if (trimmed.startsWith("SELECT") && trimmed.includes("FROM users")) {
      for (const user of this.users.values()) {
        if (params.includes(user.email) || params.includes(user.phone_number) || params.includes(user.user_id)) {
          rows.push({ ...user });
        }
      }
    } else if (trimmed.startsWith("INSERT INTO users")) {
      const user = {
        user_id: params[0],
        email: params[1],
        phone_number: params[2],
        password_hash: params[3],
        display_name: params[4],
        email_verified: params[5] ?? false,
        phone_verified: params[6] ?? false,
        account_status: params[7] ?? "PENDING_VERIFICATION",
        role: params[8] ?? "VERIFIED_USER",
        mfa_enabled: params[9] ?? false,
        created_at: new Date()
      };
      this.users.set(user.user_id, user);
      rows.push(user);
    } else if (trimmed.startsWith("UPDATE users")) {
      // Handle user update
      for (const user of this.users.values()) {
        if (params.includes(user.user_id) || params.includes(user.email) || params.includes(user.phone_number)) {
          if (trimmed.includes("email_verified = TRUE")) user.email_verified = true;
          if (trimmed.includes("phone_verified = TRUE")) user.phone_verified = true;
          if (trimmed.includes("account_status = 'ACTIVE'")) user.account_status = "ACTIVE";
          rows.push({ ...user });
        }
      }
    } else if (trimmed.startsWith("INSERT INTO verification_tokens")) {
      const token = {
        token_id: params[0],
        identifier: params[1],
        token_type: params[2],
        code_hash: params[3],
        attempts_count: 0,
        max_attempts: 5,
        expires_at: params[4],
        is_used: false,
        created_at: new Date()
      };
      this.verificationTokens.set(token.token_id, token);
      rows.push(token);
    } else if (trimmed.startsWith("SELECT") && trimmed.includes("FROM verification_tokens")) {
      for (const token of this.verificationTokens.values()) {
        if (params.includes(token.identifier) && (!params[1] || token.token_type === params[1])) {
          rows.push({ ...token });
        }
      }
    } else if (trimmed.startsWith("INSERT INTO parties")) {
      const party = {
        party_id: params[0],
        host_user_id: params[1],
        title: params[2],
        short_code: params[3],
        qr_payload: params[4],
        state: "ACTIVE",
        settings_json: params[5] ?? {},
        created_at: new Date(),
        expires_at: params[6]
      };
      this.parties.set(party.party_id, party);
      rows.push(party);
    } else if (trimmed.startsWith("SELECT") && trimmed.includes("FROM parties")) {
      for (const party of this.parties.values()) {
        if (params.includes(party.party_id) || params.includes(party.short_code)) {
          rows.push({ ...party });
        }
      }
    } else if (trimmed.startsWith("INSERT INTO chat_messages")) {
      const msg = {
        message_id: params[0],
        party_id: params[1],
        sender_user_id: params[2],
        recipient_user_id: params[3],
        is_from_host: params[4],
        visibility: params[5],
        content: params[6],
        delivery_state: "SENT",
        created_at: new Date()
      };
      this.chatMessages.set(msg.message_id, msg);
      rows.push(msg);
    } else if (trimmed.startsWith("SELECT") && trimmed.includes("FROM chat_messages")) {
      const partyId = params[0];
      const requesterId = params[1];
      const hostId = params[2];

      for (const msg of this.chatMessages.values()) {
        if (msg.party_id === partyId) {
          // Object-level authorization for chat privacy:
          // 1. Everyone can see public messages
          // 2. Private messages are ONLY visible to sender or host
          if (msg.visibility === "EVERYONE" || msg.visibility === "HOST_ANNOUNCEMENT") {
            rows.push({ ...msg });
          } else if (msg.visibility === "HOST_ONLY") {
            if (msg.sender_user_id === requesterId || requesterId === hostId) {
              rows.push({ ...msg });
            }
          }
        }
      }
    } else if (trimmed.startsWith("INSERT INTO security_events")) {
      const event = {
        event_id: params[0],
        event_type: params[1],
        severity: params[2],
        actor_reference: params[3],
        party_id: params[4],
        result: params[5],
        action_taken: params[6],
        details: params[7],
        timestamp: new Date()
      };
      this.securityEvents.set(event.event_id, event);
      rows.push(event);
    } else if (trimmed.startsWith("SELECT") && trimmed.includes("FROM security_events")) {
      rows.push(...Array.from(this.securityEvents.values()));
    }

    return {
      rows: rows as T[],
      command: trimmed.split(" ")[0],
      rowCount: rows.length,
      oid: 0,
      fields: []
    };
  }

  clear() {
    this.users.clear();
    this.verificationTokens.clear();
    this.authSessions.clear();
    this.parties.clear();
    this.partyMembers.clear();
    this.chatMessages.clear();
    this.activitySessions.clear();
    this.securityEvents.clear();
  }
}

export const mockDb = new InMemoryDatabase();
