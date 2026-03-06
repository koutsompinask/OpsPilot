CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.auth_users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS auth.refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    metadata VARCHAR(255) NULL,
    CONSTRAINT fk_refresh_session_user FOREIGN KEY (user_id) REFERENCES auth.auth_users(id)
);

CREATE INDEX IF NOT EXISTS idx_auth_users_tenant_id ON auth.auth_users (tenant_id);
CREATE INDEX IF NOT EXISTS idx_refresh_sessions_user_id ON auth.refresh_sessions (user_id);
