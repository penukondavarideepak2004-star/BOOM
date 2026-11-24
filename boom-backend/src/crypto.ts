import crypto from "node:crypto";

/**
 * High-entropy cryptographic utilities for BOOM
 */
export class SecurityCrypto {
  /**
   * Hashes a password using scrypt with a random 16-byte salt (memory-hard).
   */
  static async hashPassword(password: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const salt = crypto.randomBytes(16).toString("hex");
      crypto.scrypt(password, salt, 64, (err, derivedKey) => {
        if (err) return reject(err);
        resolve(`${salt}:${derivedKey.toString("hex")}`);
      });
    });
  }

  /**
   * Verifies a password against a stored salt:hash using constant-time comparison.
   */
  static async verifyPassword(password: string, storedHash: string): Promise<boolean> {
    return new Promise((resolve) => {
      const parts = storedHash.split(":");
      if (parts.length !== 2) return resolve(false);

      const [salt, key] = parts;
      crypto.scrypt(password, salt, 64, (err, derivedKey) => {
        if (err) return resolve(false);
        const keyBuffer = Buffer.from(key, "hex");
        const match = crypto.timingSafeEqual(derivedKey, keyBuffer);
        resolve(match);
      });
    });
  }

  /**
   * Generates a single-use 6-digit numeric OTP with cryptographically secure randomness.
   */
  static generateOtp(): string {
    const num = crypto.randomInt(100000, 999999);
    return num.toString();
  }

  /**
   * Generates a 6-character alphanumeric Short Code for parties (e.g. "BOOM42").
   */
  static generatePartyShortCode(): string {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Removed ambiguous 0/O, 1/I
    let code = "";
    const bytes = crypto.randomBytes(6);
    for (let i = 0; i < 6; i++) {
      code += chars[bytes[i] % chars.length];
    }
    return code;
  }

  /**
   * Creates a signed HMAC-SHA256 token (JWT format) with payload and expiration.
   */
  static signToken(payload: Record<string, any>, secret: string, expiresInSeconds: number): string {
    const header = Buffer.from(JSON.stringify({ alg: "HS256", typ: "JWT" })).toString("base64url");
    const exp = Math.floor(Date.now() / 1000) + expiresInSeconds;
    const body = Buffer.from(JSON.stringify({ ...payload, exp })).toString("base64url");
    const signature = crypto
      .createHmac("sha256", secret)
      .update(`${header}.${body}`)
      .digest("base64url");
    return `${header}.${body}.${signature}`;
  }

  /**
   * Verifies an HMAC-SHA256 token and returns the payload if valid and not expired.
   */
  static verifyToken(token: string, secret: string): Record<string, any> | null {
    try {
      const parts = token.split(".");
      if (parts.length !== 3) return null;
      const [header, body, signature] = parts;

      const expectedSignature = crypto
        .createHmac("sha256", secret)
        .update(`${header}.${body}`)
        .digest("base64url");

      if (
        !crypto.timingSafeEqual(
          Buffer.from(signature),
          Buffer.from(expectedSignature)
        )
      ) {
        return null;
      }

      const decoded = JSON.parse(Buffer.from(body, "base64url").toString("utf-8"));
      if (decoded.exp && decoded.exp < Math.floor(Date.now() / 1000)) {
        return null; // Expired
      }
      return decoded;
    } catch {
      return null;
    }
  }
}
