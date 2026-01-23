CREATE TABLE IF NOT EXISTS tasks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id    UUID         NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    due_date    DATE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_tasks_owner
        FOREIGN KEY (owner_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_tasks_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_tasks_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);

-- Index for owner lookup (most common query)
CREATE INDEX IF NOT EXISTS ix_tasks_owner_id
    ON tasks (owner_id);

-- Composite index for filtering by owner and status
CREATE INDEX IF NOT EXISTS ix_tasks_owner_status
    ON tasks (owner_id, status);

-- Composite index for filtering by owner and due_date
CREATE INDEX IF NOT EXISTS ix_tasks_owner_due_date
    ON tasks (owner_id, due_date);
