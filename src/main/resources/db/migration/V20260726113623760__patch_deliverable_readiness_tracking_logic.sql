-- V20260726113623760__patch_deliverable_readiness_tracking_logic.sql
-- Create deliverables table and populate with 19 initial deliverables if not already present

CREATE TABLE IF NOT EXISTS deliverables (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- 'PENDING', 'COMPLETED', 'MERGED', etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Insert 19 initial deliverables to match the project state: 5 merged, 14 pending
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-1', 'Setup repository', 'MERGED');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-2', 'Configure CI template', 'MERGED');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-3', 'Create environment template', 'MERGED');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-4', 'Implement proxy management', 'MERGED');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-5', 'Implement account schema', 'MERGED');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-6', 'Add CRM optimization', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-7', 'Define campaigns and leads', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-8', 'Add dialogue schema', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-9', 'Implement Telegram dispatch', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-10', 'Add delay engine', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-11', 'Implement account filter', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-12', 'Add warm up state tracking', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-13', 'Implement ai reply generator', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-14', 'Create campaign dispatch worker', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-15', 'Add custom prompt editor', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-16', 'Implement daily rate limiter', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-17', 'Add manual reply takeover UI', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-18', 'Create lead ingestion parser', 'PENDING');
MERGE INTO deliverables (id, name, status) KEY(id) VALUES ('task-19', 'Setup inbox dashboard UI', 'PENDING');
