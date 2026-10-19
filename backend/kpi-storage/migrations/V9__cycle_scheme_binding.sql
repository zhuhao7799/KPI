CREATE TABLE assessment_cycle_scheme (
    id CHAR(36) NOT NULL,
    assessment_cycle_id CHAR(36) NOT NULL,
    organization_category VARCHAR(64) NOT NULL,
    scheme_id CHAR(36) NOT NULL,
    indicator_version_id CHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    locked_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cycle_scheme_category (assessment_cycle_id, organization_category),
    KEY ix_cycle_scheme_scheme (scheme_id),
    KEY ix_cycle_scheme_version (indicator_version_id),
    CONSTRAINT fk_cycle_scheme_cycle FOREIGN KEY (assessment_cycle_id) REFERENCES assessment_cycle(id),
    CONSTRAINT fk_cycle_scheme_scheme FOREIGN KEY (scheme_id) REFERENCES performance_scheme(id),
    CONSTRAINT fk_cycle_scheme_version FOREIGN KEY (indicator_version_id) REFERENCES indicator_version(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核周期绩效方案绑定';

ALTER TABLE assessment_submission
    ADD COLUMN scheme_id CHAR(36) NULL AFTER indicator_version_id,
    ADD KEY ix_submission_scheme (scheme_id),
    ADD CONSTRAINT fk_submission_scheme FOREIGN KEY (scheme_id) REFERENCES performance_scheme(id);
