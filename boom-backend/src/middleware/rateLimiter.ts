import { Request, Response, NextFunction } from "express";

interface RateLimitRecord {
  count: number;
  resetTimeEpochMs: Long;
}

const rateLimitStore = new Map<string, { count: number; resetTime: number }>();

/**
 * In-memory sliding window rate limiter
 */
export function rateLimit(options: { windowMs: number; max: number; message?: string }) {
  const { windowMs, max, message = "Too many requests, please try again later." } = options;

  return (req: Request, res: Response, next: NextFunction): void => {
    const key = req.ip || req.headers["x-forwarded-for"]?.toString() || "anonymous";
    const now = Date.now();

    const record = rateLimitStore.get(key);

    if (!record || now > record.resetTime) {
      rateLimitStore.set(key, { count: 1, resetTime: now + windowMs });
      next();
      return;
    }

    record.count++;

    if (record.count > max) {
      res.status(429).json({
        success: false,
        error: {
          code: "RATE_LIMITED",
          message,
          retryAfterSeconds: Math.ceil((record.resetTime - now) / 1000)
        }
      });
      return;
    }

    next();
  };
}
