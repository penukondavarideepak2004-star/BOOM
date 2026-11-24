# BOOM Production Deployment & Hardening Checklist

## 1. Android Client Release Readiness (`boom-android/`)

### A. Signing & Keystore Configuration
- [ ] Create dedicated production release keystore:
  ```bash
  keytool -genkey -v -keystore boom-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias boom-release-key
  ```
- [ ] Configure environment variable injection for keystore credentials in `app/build.gradle.kts`:
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
- [ ] Confirm Android Keystore (`EncryptedSharedPreferences`) uses MasterKey scheme `AES256_GCM`.

### B. ProGuard / R8 Obfuscation Rules
- [ ] Enable `isMinifyEnabled = true` and `isShrinkResources = true` in release build type.
- [ ] Confirm rules keep Kotlinx Serialization serializers in `proguard-rules.pro`:
  ```proguard
  -keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
  -keepclassmembers class * {
      @kotlinx.serialization.Serializable <fields>;
  }
  -keepclassmembers class * {
      @kotlinx.serialization.SerialName <fields>;
  }
  -dontwarn kotlinx.serialization.**
  ```

### C. Permissions & Manifest Compliance
- [ ] Ensure `targetSdk = 35` and `minSdk = 26` compliance.
- [ ] Confirm runtime permission requests are handled for:
  - `android.permission.BLUETOOTH_SCAN` (`usesPermissionFlags="neverForLocation"`)
  - `android.permission.BLUETOOTH_CONNECT`
  - `android.permission.NEARBY_WIFI_DEVICES` (`usesPermissionFlags="neverForLocation"`)
- [ ] Verify zero request for microphone or recording permissions (`RECORD_AUDIO` is NOT in the manifest).

---

## 2. Secure Backend & Database Readiness (`boom-backend/`)

### A. Environment Configuration
- [ ] Set `NODE_ENV=production`.
- [ ] Set cryptographically secure `JWT_SECRET` (minimum 256-bit entropy).
- [ ] Provide production PostgreSQL connection string (`DATABASE_URL`) with SSL mode required (`?sslmode=require`).
- [ ] Configure `CORS_ORIGIN` to restricted authorized origins.

### B. PostgreSQL Database Migrations & Tuning
- [ ] Run migration script:
  ```bash
  psql $DATABASE_URL -f src/db/migrations/001_initial_schema.sql
  ```
- [ ] Verify connection pooling parameters:
  - Max pool size: 20 connections per server instance.
  - Idle timeout: 30,000ms.
  - Connection timeout: 5,000ms.
- [ ] Confirm table indexes on foreign keys (`party_id`, `user_id`, `created_at`) are created.

### C. Reverse Proxy & Network Security
- [ ] Deploy behind an HTTPS reverse proxy (Nginx / AWS ALB / Cloudflare) terminating TLS 1.3.
- [ ] Verify Helmet security headers:
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
- [ ] Enforce sliding-window rate limiters on `/api/auth/register` (5 req / 10 min) and `/api/auth/verify` (5 req / 15 min).

---

## 3. Pre-Flight Verification Script
Run the automated acceptance suite prior to tag or release:
```bash
node --experimental-strip-types --test boom-backend/tests/api_security.test.ts
```
Ensure **100% tests pass (8 passed, 0 failed)** before deploying.
