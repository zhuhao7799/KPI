ALTER TABLE assessment_submission
    ADD CONSTRAINT ck_submission_state
    CHECK (state IN ('DRAFT', 'SUBMITTED', 'RETURNED', 'CONFIRMED', 'ARCHIVED', 'VOID'));
