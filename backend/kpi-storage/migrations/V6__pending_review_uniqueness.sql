-- Only one pending review task may exist for each submission. Completed tasks remain historical.
ALTER TABLE assessment_approval_task
    ADD COLUMN pending_review_submission_id CHAR(36)
        GENERATED ALWAYS AS (CASE WHEN state = 'PENDING' AND task_type = 'REVIEW'
            THEN assessment_submission_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_pending_review_submission (pending_review_submission_id);
