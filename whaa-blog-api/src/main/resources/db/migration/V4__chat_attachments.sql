CREATE TABLE chat_message_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    message_id BIGINT NULL,
    uploaded_by BIGINT NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL,
    CONSTRAINT uk_chat_message_attachment_object_key UNIQUE (object_key),
    CONSTRAINT fk_chat_attachment_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id),
    CONSTRAINT fk_chat_attachment_message FOREIGN KEY (message_id) REFERENCES chat_message(id),
    CONSTRAINT fk_chat_attachment_uploader FOREIGN KEY (uploaded_by) REFERENCES sys_user(id),
    INDEX idx_chat_attachment_message (message_id, id),
    INDEX idx_chat_attachment_pending (message_id, expires_at),
    INDEX idx_chat_attachment_uploader_pending (uploaded_by, message_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
