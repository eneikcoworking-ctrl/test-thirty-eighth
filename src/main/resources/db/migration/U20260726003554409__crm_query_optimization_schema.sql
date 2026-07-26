-- MANUAL UNDO INSTRUCTION FOR V20260726003554409__crm_query_optimization_schema.sql
-- Note: This is an additive schema migration; however, for full clean recovery,
-- the following manual rollback steps are defined.
-- Since the Flyway Community edition does not automatically run undo scripts,
-- this file is intended as a validated reference for operators.

DROP VIEW IF EXISTS unified_inbox_view;
DROP INDEX IF EXISTS idx_chats_account_status_last_msg;
DROP TABLE IF EXISTS crm_messages;
DROP TABLE IF EXISTS crm_chats;
DROP TABLE IF EXISTS crm_leads;
DROP TABLE IF EXISTS crm_telegram_accounts;
