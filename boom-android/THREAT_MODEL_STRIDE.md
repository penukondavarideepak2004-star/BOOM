# BOOM Security Threat Model & STRIDE Analysis

## 1. System Overview & Trust Boundaries

**BOOM** is a multi-device social party platform operating in a hybrid cloud & local peer-to-peer topology:
- **Cloud Trust Boundary**: HTTPS/TLS 1.3 REST API with PostgreSQL backend, enforcing authentication, authorization, and rate limiting.
- **Local Peer-to-Peer Trust Boundary**: Google Nearby Connections / Wi-Fi Direct mesh where a host device coordinates synchronized audio and activities across up to 10 participant devices.
- **Client Storage Trust Boundary**: Android Keystore hardware-backed encryption (`EncryptedSharedPreferences`) protecting auth sessions.
- **Explicit Non-Goal / Out-of-Scope Boundary**: **Voice calling is strictly prohibited and absent from the codebase**. BOOM does not inspect private device files or operate background audio surveillance.

```
+-----------------------------------------------------------------------------------+
|                              CLOUD BACKEND (HTTPS / TLS 1.3)                      |
|  - Helmet Security Headers                                                        |
|  - Sliding-Window Rate Limiting (5-attempt OTP throttle)                          |
|  - Scrypt Password Hashing + Constant-Time Comparison                             |
|  - Object-Level Chat Authorization (Host-Only Isolation)                          |
+------------------------------------------+----------------------------------------+
                                           |
                                      REST Requests
                                           |
+------------------------------------------v----------------------------------------+
|                                 HOST ANDROID DEVICE                               |
|  - Keystore AES-GCM-256 Session Storage                                           |
|  - BOOM Guardian Local Audit Logger (Zero Credential Leakage)                     |
|  - Master Clock Reference & Audio Sync Packet Dispatch                            |
|  - Host Approval Gate (HOST-03) & Revocation (HOST-05)                            |
+------------------------------------------+----------------------------------------+
                                           |
                        Local Wi-Fi Direct / Nearby Mesh
                                           |
+------------------------------------------v----------------------------------------+
|                             PARTICIPANT ANDROID DEVICE                            |
|  - Independent Local Volume Slider & Mute (FR-06)                                 |
|  - Sub-millisecond Clock Filter (RTT/2 + EMA) & Dynamic Drift Speed Control       |
|  - Defense-in-Depth Chat Privacy (Drops unauthorized third-party private notes)  |
+-----------------------------------------------------------------------------------+
```

---

## 2. STRIDE Threat Assessment & Mitigations

| STRIDE Category | Threat Description | Attack Vector | BOOM Technical Mitigation |
| :--- | :--- | :--- | :--- |
| **Spoofing** | Attacker impersonates the Host or creates rogue party beacons. | Broadcasting spoofed Nearby advertise frames with fake short codes. | • Mutual ephemeral SAS pairing token comparison.<br>• Host-side explicit approval queue (`HOST-03`).<br>• Cryptographically random 6-character short codes with short session lifespans. |
| **Spoofing** | Attacker creates fake accounts or impersonates existing users. | Credential brute-force, account enumeration. | • Dual email & SMS OTP verification (`AUTH-02`, `AUTH-03`).<br>• Generic login error messages preventing account enumeration (`AUTH-08`).<br>• Memory-hard `scrypt` salted password hashing. |
| **Tampering** | Rogue participant tampers with synchronized playback timing to disrupt the room. | Injecting falsified `AudioSyncPacket.PlaybackControl` or drift packets. | • Only Host broadcasts playback schedule and seek commands.<br>• `AudioSyncEngine` applies low-pass EMA filter and strictly caps speed adjustments to $0.995\times - 1.005\times$, discarding outliers >120ms. |
| **Tampering** | Participant attempts to alter vote counts or submit multiple votes in Vibe Vote. | Replaying or spamming vote packets. | • Single-vote state constraint tracked per `userId`. Changing vote decrements prior option automatically. |
| **Repudiation** | Host denies kicking a participant or a participant denies abusive activity. | Disputed administrative actions. | • `BoomGuardian` logs tamper-evident audit events (`PARTICIPANT_REMOVED`, `UNAUTHORIZED_ACCESS_ATTEMPT`) with monotonic timestamps and pseudonymous references. |
| **Information Disclosure** | Participant B snoops on Participant A's private messages to the Host. | Sniffing local packets or querying API chat endpoints. | • Strict server-side object authorization (`403 FORBIDDEN` if reader is neither sender nor host).<br>• Client-side defense-in-depth filter drops non-matching incoming packets and records a Guardian security event. |
| **Information Disclosure** | User credentials or tokens leak into crash reports or logs. | Android logcat, unhandled exception stack traces. | • Automated `PrivacyFilter` regex redaction stripping passwords, OTPs, bearer tokens, and emails before logging or transmission. |
| **Denial of Service** | Attacker floods OTP endpoint or brute-forces 6-digit codes. | Automated script calling `/api/auth/verify`. | • 5-attempt sliding window threshold per identifier triggers HTTP `429 OTP_THROTTLED`.<br>• Global IP rate limiting. |
| **Denial of Service** | Malicious device floods Host with join requests to crash the session. | Rapid pairing connection requests. | • Party membership hard-capped to `maxParticipants` (default 10). Excess requests rejected immediately. |
| **Elevation of Privilege** | Participant attempts to trigger Host commands (Play/Pause, Kick, End Party). | Tampered payload or API calls targeting host endpoints. | • Server validates `party.host_user_id == session.user_id` on all mutating routes.<br>• Jetpack Compose ViewModels ignore host-only triggers if `!uiState.isHost`. |

---

## 3. Privacy & Non-Intrusive Security Guarantees

In strict adherence to PRD Section 1 and Section 2.3:
1. **Zero Voice Calling / Audio Conferencing**: The BOOM codebase contains zero WebRTC, Bluetooth voice, HFP/HSP, push-to-talk, or audio recording libraries.
2. **Zero Antivirus / File System Scanning**: BOOM never inspects external storage, photos, or documents outside its isolated sandbox (`/data/data/com.boom.app`).
3. **Zero Third-Party Private Messaging**: Participants can only communicate in the public party channel or send private notes to the Host. No peer-to-peer private channels exist.
4. **Hardware-Backed Cryptography**: All persistent auth credentials utilize Android Keystore hardware keys with `EncryptedSharedPreferences`.
