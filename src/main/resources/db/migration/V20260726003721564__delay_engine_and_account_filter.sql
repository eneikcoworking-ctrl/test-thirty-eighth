-- Flyway migration V20260726003721564
-- Adds delay engine properties to tg_accounts table

ALTER TABLE tg_accounts ADD COLUMN consecutive_actions INT DEFAULT 0 NOT NULL;
ALTER TABLE tg_accounts ADD COLUMN last_action_at TIMESTAMP WITH TIME ZONE;
