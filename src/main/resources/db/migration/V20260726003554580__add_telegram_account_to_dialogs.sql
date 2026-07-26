-- V20260726003554580__add_telegram_account_to_dialogs.sql
-- Add telegram_account_id column to dialogs table to link a dialog with its assigned Telegram session

ALTER TABLE dialogs ADD COLUMN telegram_account_id BIGINT;
