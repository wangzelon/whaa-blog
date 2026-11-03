ALTER TABLE sys_user
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER role,
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN deleted_at DATETIME NULL AFTER updated_at,
    ADD INDEX idx_sys_user_role_status_id (role, status, id);
