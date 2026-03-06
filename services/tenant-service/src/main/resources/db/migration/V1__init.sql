CREATE SCHEMA IF NOT EXISTS tenant;

CREATE TABLE IF NOT EXISTS tenant.tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    settings_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tenant.user_profiles (
    user_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_profile_tenant FOREIGN KEY (tenant_id) REFERENCES tenant.tenants(id),
    CONSTRAINT uq_user_profile_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_tenant ON tenant.user_profiles (tenant_id);
