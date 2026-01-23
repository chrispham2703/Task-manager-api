CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(72)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Case-insensitive unique email
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users (lower(email));

-- Status constraint for soft-delete
ALTER TABLE users
    ADD CONSTRAINT ck_users_status
    CHECK (status IN ('ACTIVE', 'LOCKED', 'DELETED'));

-- Index for filtering by status
CREATE INDEX IF NOT EXISTS ix_users_status
    ON users (status);
