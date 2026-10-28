CREATE TABLE chat_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    low_user_id BIGINT NOT NULL,
    high_user_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    last_message_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_conversation_users UNIQUE (low_user_id, high_user_id),
    CONSTRAINT fk_chat_conversation_low_user FOREIGN KEY (low_user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_chat_conversation_high_user FOREIGN KEY (high_user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_chat_conversation_created_by FOREIGN KEY (created_by) REFERENCES sys_user(id),
    INDEX idx_chat_conversation_low_updated (low_user_id, updated_at),
    INDEX idx_chat_conversation_high_updated (high_user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id),
    INDEX idx_chat_message_conversation_id (conversation_id, id),
    INDEX idx_chat_message_sender (sender_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_read_state (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_chat_read_state_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id),
    CONSTRAINT fk_chat_read_state_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
