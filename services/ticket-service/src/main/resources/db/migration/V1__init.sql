CREATE SCHEMA IF NOT EXISTS ticket;

CREATE TABLE IF NOT EXISTS ticket.tickets (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_by_email VARCHAR(255) NOT NULL,
    origin VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    question VARCHAR(2000) NOT NULL,
    answer VARCHAR(8000),
    confidence DOUBLE PRECISION,
    source_count INT NOT NULL DEFAULT 0,
    notes VARCHAR(2000),
    created_request_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tickets_tenant_created_at ON ticket.tickets (tenant_id, created_at DESC);
