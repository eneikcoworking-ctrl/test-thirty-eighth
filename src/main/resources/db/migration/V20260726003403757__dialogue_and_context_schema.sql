CREATE TABLE dialogs (
    id VARCHAR(36) PRIMARY KEY,
    telegram_chat_id BIGINT NOT NULL,
    lead_username VARCHAR(255),
    lead_phone VARCHAR(50),
    ai_state TEXT,
    status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    dialog_id VARCHAR(36) NOT NULL,
    sender_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    telegram_message_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_dialog_id FOREIGN KEY (dialog_id) REFERENCES dialogs(id) ON DELETE CASCADE
);
