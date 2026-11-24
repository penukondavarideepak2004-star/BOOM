import express from "express";
import cors from "cors";
import helmet from "helmet";
import { config } from "./config/env.js";
import { errorHandler } from "./middleware/errorHandler.js";

export function createApp() {
  const app = express();

  // Standard Security Headers
  app.use(helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'"],
        connectSrc: ["'self'"]
      }
    },
    hidePoweredBy: true,
    hsts: { maxAge: 31536000, includeSubDomains: true }
  }));

  // CORS Configuration
  app.use(cors({
    origin: config.corsOrigin,
    methods: ["GET", "POST", "PUT", "DELETE"],
    allowedHeaders: ["Content-Type", "Authorization", "X-Device-Reference"],
    credentials: true
  }));

  app.use(express.json({ limit: "1mb" }));
  app.use(express.urlencoded({ extended: true, limit: "1mb" }));

  // Liveness & Readiness Probes
  app.get("/health", (req, res) => {
    res.json({
      status: "UP",
      service: "boom-backend",
      version: "1.0.0",
      timestamp: new Date().toISOString()
    });
  });

  // Global Error Handler
  app.use(errorHandler);

  return app;
}
