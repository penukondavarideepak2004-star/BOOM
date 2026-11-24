-- BOOM PostgreSQL Database Schema
-- Version: 001_initial_schema.sql

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(32) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    role VARCHAR(32) NOT NULL DEFAULT 'VERIFIED_USER',
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);

-- 2. Verification Tokens (Email verification & SMS OTP)
CREATE TABLE IF NOT EXISTS verification_tokens (
    token_id VARCHAR(64) PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL, -- email or phone
    token_type VARCHAR(32) NOT NULL, -- 'EMAIL' or 'PHONE'
    code_hash VARCHAR(255) NOT NULL,
    attempts_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_verif_tokens_id ON verification_tokens(identifier, token_type);

-- 3. Authenticated Sessions & Refresh Tokens
CREATE TABLE IF NOT EXISTS auth_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    device_reference VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON auth_sessions(user_id);

-- 4. Parties Table
CREATE TABLE IF NOT EXISTS parties (
    party_id VARCHAR(64) PRIMARY KEY,
    host_user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    short_code VARCHAR(16) NOT NULL UNIQUE,
    qr_payload TEXT NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    settings_json JSONB NOT NULL DEFAULT '{"maxParticipants": 10, "requireHostApproval": true}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_parties_code ON parties(short_code);
CREATE INDEX IF NOT EXISTS idx_parties_host ON parties(host_user_id);

-- 5. Party Memberships (Participants)
CREATE TABLE IF NOT EXISTS party_members (
    membership_id VARCHAR(64) PRIMARY KEY,
    party_id VARCHAR(64) NOT NULL REFERENCES parties(party_id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'PARTICIPANT',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL',
    audio_route VARCHAR(64) DEFAULT 'Built-in Speaker',
    clock_offset_ms BIGINT DEFAULT 0,
    rtt_ms BIGINT DEFAULT 0,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(party_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_members_party ON party_members(party_id);

-- 6. Chat Messages (Public & Host-Private)
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id VARCHAR(64) PRIMARY KEY,
    party_id VARCHAR(64) NOT NULL REFERENCES parties(party_id) ON DELETE CASCADE,
    sender_user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    recipient_user_id VARCHAR(64) REFERENCES users(user_id) ON DELETE SET NULL, -- Target when Host-Private
    is_from_host BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(32) NOT NULL DEFAULT 'EVERYONE', -- 'EVERYONE', 'HOST_ONLY', 'HOST_ANNOUNCEMENT'
    content TEXT NOT NULL,
    delivery_state VARCHAR(32) NOT NULL DEFAULT 'SENT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_party ON chat_messages(party_id);
CREATE INDEX IF NOT EXISTS idx_chat_visibility ON chat_messages(visibility);

-- 7. Activities (Music, Voting, Games)
CREATE TABLE IF NOT EXISTS activity_sessions (
    activity_id VARCHAR(64) PRIMARY KEY,
    party_id VARCHAR(64) NOT NULL REFERENCES parties(party_id) ON DELETE CASCADE,
    activity_type VARCHAR(32) NOT NULL DEFAULT 'MUSIC_PLAYER',
    state_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activities_party ON activity_sessions(party_id);

-- 8. BOOM Guardian Security Events
CREATE TABLE IF NOT EXISTS security_events (
    event_id VARCHAR(64) PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    actor_reference VARCHAR(255) NOT NULL,
    party_id VARCHAR(64) REFERENCES parties(party_id) ON DELETE SET NULL,
    result VARCHAR(32) NOT NULL,
    action_taken VARCHAR(64) NOT NULL,
    details TEXT
);

CREATE INDEX IF NOT EXISTS idx_sec_events_type ON security_events(event_type);
CREATE INDEX IF NOT EXISTS idx_sec_events_party ON security_events(party_id);
