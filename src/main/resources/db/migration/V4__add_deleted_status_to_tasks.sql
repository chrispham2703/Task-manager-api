ALTER TABLE tasks DROP CONSTRAINT IF EXISTS ck_tasks_status;
ALTER TABLE tasks
    ADD CONSTRAINT ck_tasks_status
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'DELETED'));
CREATE INDEX IF NOT EXISTS ix_tasks_status ON tasks (status);