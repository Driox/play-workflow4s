# Akka Persistence setup

# --- !Ups

CREATE TABLE IF NOT EXISTS workflow_journal(
  event_id    serial    PRIMARY KEY,                        -- Auto-incrementing primary key
  workflow_id bigint    NOT NULL,                           -- The workflow ID
  event_data  bytea     NOT NULL,                           -- The event data (binary)
  created_at  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  -- Timestamp for event creation
);

CREATE INDEX IF NOT EXISTS idx_workflow_id ON workflow_journal(workflow_id);

CREATE TABLE if not exists executing_workflows
(
    workflow_id   BIGINT    NOT NULL,
    workflow_type VARCHAR   NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    primary key (workflow_type, workflow_id)
);

# --- !Downs

DROP IF EXISTS INDEX idx_workflow_id;
DROP IF EXISTS TABLE executing_workflows;
DROP IF EXISTS TABLE workflow_journal;
