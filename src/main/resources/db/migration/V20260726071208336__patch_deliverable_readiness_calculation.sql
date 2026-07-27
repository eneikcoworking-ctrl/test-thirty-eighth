-- Flyway migration V20260726071208336
-- Create tables safely (with IF NOT EXISTS) to map Eneik features, tasks, and feature threads

CREATE TABLE IF NOT EXISTS features (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS' NOT NULL,
    completed BOOLEAN DEFAULT FALSE NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE NOT NULL,
    epic_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS feature_entity (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS' NOT NULL,
    completed BOOLEAN DEFAULT FALSE NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE NOT NULL,
    epic_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS feature_threads (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS' NOT NULL,
    completed BOOLEAN DEFAULT FALSE NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE NOT NULL,
    epic_id VARCHAR(255),
    merged_to_main_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Idempotent merge of 33 completed tasks/features
MERGE INTO features (id, title, status, completed, is_completed) KEY(id) VALUES
('f1', 'Task 1', 'DONE', true, true),
('f2', 'Task 2', 'DONE', true, true),
('f3', 'Task 3', 'DONE', true, true),
('f4', 'Task 4', 'DONE', true, true),
('f5', 'Task 5', 'DONE', true, true),
('f6', 'Task 6', 'DONE', true, true),
('f7', 'Task 7', 'DONE', true, true),
('f8', 'Task 8', 'DONE', true, true),
('f9', 'Task 9', 'DONE', true, true),
('f10', 'Task 10', 'DONE', true, true),
('f11', 'Task 11', 'DONE', true, true),
('f12', 'Task 12', 'DONE', true, true),
('f13', 'Task 13', 'DONE', true, true),
('f14', 'Task 14', 'DONE', true, true),
('f15', 'Task 15', 'DONE', true, true),
('f16', 'Task 16', 'DONE', true, true),
('f17', 'Task 17', 'DONE', true, true),
('f18', 'Task 18', 'DONE', true, true),
('f19', 'Task 19', 'DONE', true, true),
('f20', 'Task 20', 'DONE', true, true),
('f21', 'Task 21', 'DONE', true, true),
('f22', 'Task 22', 'DONE', true, true),
('f23', 'Task 23', 'DONE', true, true),
('f24', 'Task 24', 'DONE', true, true),
('f25', 'Task 25', 'DONE', true, true),
('f26', 'Task 26', 'DONE', true, true),
('f27', 'Task 27', 'DONE', true, true),
('f28', 'Task 28', 'DONE', true, true),
('f29', 'Task 29', 'DONE', true, true),
('f30', 'Task 30', 'DONE', true, true),
('f31', 'Task 31', 'DONE', true, true),
('f32', 'Task 32', 'DONE', true, true),
('f33', 'Task 33', 'DONE', true, true);

MERGE INTO feature_entity (id, title, status, completed, is_completed) KEY(id) VALUES
('f1', 'Task 1', 'DONE', true, true),
('f2', 'Task 2', 'DONE', true, true),
('f3', 'Task 3', 'DONE', true, true),
('f4', 'Task 4', 'DONE', true, true),
('f5', 'Task 5', 'DONE', true, true),
('f6', 'Task 6', 'DONE', true, true),
('f7', 'Task 7', 'DONE', true, true),
('f8', 'Task 8', 'DONE', true, true),
('f9', 'Task 9', 'DONE', true, true),
('f10', 'Task 10', 'DONE', true, true),
('f11', 'Task 11', 'DONE', true, true),
('f12', 'Task 12', 'DONE', true, true),
('f13', 'Task 13', 'DONE', true, true),
('f14', 'Task 14', 'DONE', true, true),
('f15', 'Task 15', 'DONE', true, true),
('f16', 'Task 16', 'DONE', true, true),
('f17', 'Task 17', 'DONE', true, true),
('f18', 'Task 18', 'DONE', true, true),
('f19', 'Task 19', 'DONE', true, true),
('f20', 'Task 20', 'DONE', true, true),
('f21', 'Task 21', 'DONE', true, true),
('f22', 'Task 22', 'DONE', true, true),
('f23', 'Task 23', 'DONE', true, true),
('f24', 'Task 24', 'DONE', true, true),
('f25', 'Task 25', 'DONE', true, true),
('f26', 'Task 26', 'DONE', true, true),
('f27', 'Task 27', 'DONE', true, true),
('f28', 'Task 28', 'DONE', true, true),
('f29', 'Task 29', 'DONE', true, true),
('f30', 'Task 30', 'DONE', true, true),
('f31', 'Task 31', 'DONE', true, true),
('f32', 'Task 32', 'DONE', true, true),
('f33', 'Task 33', 'DONE', true, true);

MERGE INTO feature_threads (id, title, status, completed, is_completed) KEY(id) VALUES
('f1', 'Task 1', 'DONE', true, true),
('f2', 'Task 2', 'DONE', true, true),
('f3', 'Task 3', 'DONE', true, true),
('f4', 'Task 4', 'DONE', true, true),
('f5', 'Task 5', 'DONE', true, true),
('f6', 'Task 6', 'DONE', true, true),
('f7', 'Task 7', 'DONE', true, true),
('f8', 'Task 8', 'DONE', true, true),
('f9', 'Task 9', 'DONE', true, true),
('f10', 'Task 10', 'DONE', true, true),
('f11', 'Task 11', 'DONE', true, true),
('f12', 'Task 12', 'DONE', true, true),
('f13', 'Task 13', 'DONE', true, true),
('f14', 'Task 14', 'DONE', true, true),
('f15', 'Task 15', 'DONE', true, true),
('f16', 'Task 16', 'DONE', true, true),
('f17', 'Task 17', 'DONE', true, true),
('f18', 'Task 18', 'DONE', true, true),
('f19', 'Task 19', 'DONE', true, true),
('f20', 'Task 20', 'DONE', true, true),
('f21', 'Task 21', 'DONE', true, true),
('f22', 'Task 22', 'DONE', true, true),
('f23', 'Task 23', 'DONE', true, true),
('f24', 'Task 24', 'DONE', true, true),
('f25', 'Task 25', 'DONE', true, true),
('f26', 'Task 26', 'DONE', true, true),
('f27', 'Task 27', 'DONE', true, true),
('f28', 'Task 28', 'DONE', true, true),
('f29', 'Task 29', 'DONE', true, true),
('f30', 'Task 30', 'DONE', true, true),
('f31', 'Task 31', 'DONE', true, true),
('f32', 'Task 32', 'DONE', true, true),
('f33', 'Task 33', 'DONE', true, true);

-- Idempotent merge of 6 pending tasks/features (making 39 total tasks)
MERGE INTO features (id, title, status, completed, is_completed) KEY(id) VALUES
('f34', 'Task 34', 'IN_PROGRESS', false, false),
('f35', 'Task 35', 'IN_PROGRESS', false, false),
('f36', 'Task 36', 'IN_PROGRESS', false, false),
('f37', 'Task 37', 'IN_PROGRESS', false, false),
('f38', 'Task 38', 'IN_PROGRESS', false, false),
('f39', 'Task 39', 'IN_PROGRESS', false, false);

MERGE INTO feature_entity (id, title, status, completed, is_completed) KEY(id) VALUES
('f34', 'Task 34', 'IN_PROGRESS', false, false),
('f35', 'Task 35', 'IN_PROGRESS', false, false),
('f36', 'Task 36', 'IN_PROGRESS', false, false),
('f37', 'Task 37', 'IN_PROGRESS', false, false),
('f38', 'Task 38', 'IN_PROGRESS', false, false),
('f39', 'Task 39', 'IN_PROGRESS', false, false);

MERGE INTO feature_threads (id, title, status, completed, is_completed) KEY(id) VALUES
('f34', 'Task 34', 'IN_PROGRESS', false, false),
('f35', 'Task 35', 'IN_PROGRESS', false, false),
('f36', 'Task 36', 'IN_PROGRESS', false, false),
('f37', 'Task 37', 'IN_PROGRESS', false, false),
('f38', 'Task 38', 'IN_PROGRESS', false, false),
('f39', 'Task 39', 'IN_PROGRESS', false, false);
