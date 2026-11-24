import dotenv from "dotenv";

dotenv.config();

export const config = {
  port: parseInt(process.env.PORT || "8080", 10),
  nodeEnv: process.env.NODE_ENV || "development",
  databaseUrl: process.env.DATABASE_URL || "postgresql://boom_user:boom_password@localhost:5432/boom_db",
  jwtSecret: process.env.JWT_SECRET || "boom-development-secret-key-32-chars-long!",
  refreshTokenSecret: process.env.REFRESH_TOKEN_SECRET || "boom-refresh-development-secret-key-32-chars!",
  accessTokenTtlSeconds: 15 * 60, // 15 minutes short-lived access token
  refreshTokenTtlSeconds: 7 * 24 * 3600, // 7 days refresh token
  otpTtlSeconds: 5 * 60, // 5 minutes OTP expiry
  corsOrigin: process.env.CORS_ORIGIN || "*"
};
