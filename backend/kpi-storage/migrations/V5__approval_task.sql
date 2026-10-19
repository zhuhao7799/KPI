CREATE TABLE assessment_approval_task (
    id CHAR(36) NOT NULL,
    assessment_submission_id CHAR(36) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    assignee_id VARCHAR(128) NOT NULL,
    state VARCHAR(32) NOT NULL,
    comment_text VARCHAR(2000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY ix_approval_task_assignee_state (assignee_id, state, created_at),
    CONSTRAINT fk_approval_task_submission FOREIGN KEY (assessment_submission_id) REFERENCES assessment_submission (id),
    CONSTRAINT ck_approval_task_type CHECK (task_type IN ('REVIEW', 'CONFIRM', 'ARCHIVE')),
    CONSTRAINT ck_approval_task_state CHECK (state IN ('PENDING', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效审核待办';
