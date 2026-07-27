-- V20260726090907099__patch_deliverable_readiness_tracking_logic.sql
-- Create deliverables table and populate with 19 initial deliverables

CREATE TABLE deliverables (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- 'PENDING', 'COMPLETED', 'MERGED', etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Insert 19 initial deliverables to match the project state: 5 merged, 14 pending
INSERT INTO deliverables (id, name, status) VALUES ('task-1', 'Setup repository', 'MERGED');
INSERT INTO deliverables (id, name, status) VALUES ('task-2', 'Configure CI template', 'MERGED');
INSERT INTO deliverables (id, name, status) VALUES ('task-3', 'Create environment template', 'MERGED');
INSERT INTO deliverables (id, name, status) VALUES ('task-4', 'Implement proxy management', 'MERGED');
INSERT INTO deliverables (id, name, status) VALUES ('task-5', 'Implement account schema', 'MERGED');
INSERT INTO deliverables (id, name, status) VALUES ('task-6', 'Add CRM optimization', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-7', 'Define campaigns and leads', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-8', 'Add dialogue schema', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-9', 'Implement Telegram dispatch', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-10', 'Add delay engine', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-11', 'Implement account filter', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-12', 'Add warm up state tracking', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-13', 'Implement ai reply generator', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-14', 'Create campaign dispatch worker', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-15', 'Add custom prompt editor', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-16', 'Implement daily rate limiter', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-17', 'Add manual reply takeover UI', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-18', 'Create lead ingestion parser', 'PENDING');
INSERT INTO deliverables (id, name, status) VALUES ('task-19', 'Setup inbox dashboard UI', 'PENDING');
