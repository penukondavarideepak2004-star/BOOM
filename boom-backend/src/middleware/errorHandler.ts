import { Request, Response, NextFunction } from "express";

export function errorHandler(
  err: any,
  req: Request,
  res: Response,
  next: NextFunction
): void {
  // Generic error response: NEVER expose internal SQL errors or stack traces to client
  console.error(`[Error] ${req.method} ${req.url}:`, err.message || err);

  const statusCode = err.status || err.statusCode || 500;
  const errorCode = err.code || "INTERNAL_ERROR";
  const errorMessage = statusCode === 500
    ? "An unexpected internal server error occurred"
    : err.message || "An error occurred";

  res.status(statusCode).json({
    success: false,
    error: {
      code: errorCode,
      message: errorMessage
    },
    timestampEpochMs: Date.now()
  });
}
