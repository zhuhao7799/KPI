-- Warm-Flow identifiers are an optional projection. KPI remains usable while the
-- database task bridge is active, and the identifiers make retries idempotent.
ALTER TABLE assessment_approval_task
    ADD COLUMN workflow_instance_id BIGINT NULL,
    ADD COLUMN workflow_task_id BIGINT NULL,
    ADD UNIQUE KEY uk_workflow_task_id (workflow_task_id),
    ADD KEY ix_workflow_instance_id (workflow_instance_id);
