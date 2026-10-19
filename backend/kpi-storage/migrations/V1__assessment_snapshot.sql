-- Core schema for organizational assessment snapshots. MySQL 8.0 baseline.
CREATE TABLE assessment_cycle (
    id CHAR(36) NOT NULL,
    cycle_code VARCHAR(64) NOT NULL,
    cycle_name VARCHAR(200) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    state VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_assessment_cycle_code (cycle_code),
    CONSTRAINT ck_assessment_cycle_dates CHECK (ends_on >= starts_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核周期';

CREATE TABLE organization_unit_snapshot (
    id CHAR(36) NOT NULL,
    assessment_cycle_id CHAR(36) NOT NULL,
    external_unit_id VARCHAR(128) NOT NULL,
    unit_name VARCHAR(200) NOT NULL,
    unit_category VARCHAR(64) NOT NULL,
    parent_external_unit_id VARCHAR(128) NULL,
    captured_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_unit_snapshot_cycle_external (assessment_cycle_id, external_unit_id),
    CONSTRAINT fk_unit_snapshot_cycle FOREIGN KEY (assessment_cycle_id) REFERENCES assessment_cycle (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核对象组织快照';

CREATE TABLE indicator_version (
    id CHAR(36) NOT NULL,
    version_code VARCHAR(64) NOT NULL,
    version_name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    formula_version VARCHAR(64) NOT NULL,
    score_scale SMALLINT NOT NULL,
    published_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_indicator_version_code (version_code),
    CONSTRAINT ck_indicator_version_scale CHECK (score_scale BETWEEN 0 AND 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标版本';

CREATE TABLE indicator_item_snapshot (
    id CHAR(36) NOT NULL,
    indicator_version_id CHAR(36) NOT NULL,
    indicator_code VARCHAR(64) NOT NULL,
    indicator_name VARCHAR(200) NOT NULL,
    weight DECIMAL(9,4) NOT NULL,
    maximum_score DECIMAL(12,4) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_indicator_item_version_code (indicator_version_id, indicator_code),
    CONSTRAINT ck_indicator_weight_positive CHECK (weight > 0),
    CONSTRAINT ck_indicator_maximum_positive CHECK (maximum_score > 0),
    CONSTRAINT fk_indicator_item_version FOREIGN KEY (indicator_version_id) REFERENCES indicator_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标项快照';

CREATE TABLE assessment_submission (
    id CHAR(36) NOT NULL,
    assessment_cycle_id CHAR(36) NOT NULL,
    organization_unit_snapshot_id CHAR(36) NOT NULL,
    indicator_version_id CHAR(36) NOT NULL,
    state VARCHAR(32) NOT NULL,
    formula_version VARCHAR(64) NOT NULL,
    score_scale SMALLINT NOT NULL,
    total_score DECIMAL(12,4) NOT NULL,
    submitted_by VARCHAR(128) NOT NULL,
    submitted_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_cycle_unit_version (assessment_cycle_id, organization_unit_snapshot_id, indicator_version_id),
    KEY ix_submission_state (state),
    CONSTRAINT ck_submission_scale CHECK (score_scale BETWEEN 0 AND 8),
    CONSTRAINT ck_submission_total_nonnegative CHECK (total_score >= 0),
    CONSTRAINT fk_submission_cycle FOREIGN KEY (assessment_cycle_id) REFERENCES assessment_cycle (id),
    CONSTRAINT fk_submission_unit FOREIGN KEY (organization_unit_snapshot_id) REFERENCES organization_unit_snapshot (id),
    CONSTRAINT fk_submission_version FOREIGN KEY (indicator_version_id) REFERENCES indicator_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核提交快照';

CREATE TABLE assessment_score_detail (
    id CHAR(36) NOT NULL,
    assessment_submission_id CHAR(36) NOT NULL,
    indicator_code VARCHAR(64) NOT NULL,
    indicator_name VARCHAR(200) NOT NULL,
    raw_score DECIMAL(12,4) NOT NULL,
    maximum_score DECIMAL(12,4) NOT NULL,
    weight DECIMAL(9,4) NOT NULL,
    weighted_score DECIMAL(12,4) NOT NULL,
    evidence_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_score_detail_submission_indicator (assessment_submission_id, indicator_code),
    CONSTRAINT ck_score_detail_raw_range CHECK (raw_score >= 0 AND raw_score <= maximum_score),
    CONSTRAINT ck_score_detail_weight_positive CHECK (weight > 0),
    CONSTRAINT ck_score_detail_evidence_count CHECK (evidence_count >= 0),
    CONSTRAINT fk_score_detail_submission FOREIGN KEY (assessment_submission_id) REFERENCES assessment_submission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分明细快照';

CREATE TABLE assessment_audit_event (
    id CHAR(36) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id CHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    payload_json JSON NOT NULL,
    PRIMARY KEY (id),
    KEY ix_audit_aggregate (aggregate_type, aggregate_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变审计事件';

CREATE TABLE outbox_event (
    id CHAR(36) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id CHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    payload_json JSON NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    publish_attempts INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_idempotency (idempotency_key),
    KEY ix_outbox_pending (published_at, occurred_at),
    CONSTRAINT ck_outbox_attempts CHECK (publish_attempts >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务外盒事件';
