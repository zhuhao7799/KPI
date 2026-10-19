CREATE TABLE evidence_object (
    id CHAR(36) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(512) NOT NULL,
    media_type VARCHAR(128) NOT NULL,
    byte_size BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    uploaded_by VARCHAR(128) NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_evidence_storage_key (storage_key),
    CONSTRAINT ck_evidence_size_positive CHECK (byte_size > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私有材料对象元数据';

CREATE TABLE submission_evidence (
    id CHAR(36) NOT NULL,
    assessment_submission_id CHAR(36) NOT NULL,
    indicator_code VARCHAR(64) NOT NULL,
    evidence_object_id CHAR(36) NOT NULL,
    attached_by VARCHAR(128) NOT NULL,
    attached_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_evidence (assessment_submission_id, indicator_code, evidence_object_id),
    KEY ix_submission_evidence_indicator (assessment_submission_id, indicator_code),
    CONSTRAINT fk_submission_evidence_submission FOREIGN KEY (assessment_submission_id) REFERENCES assessment_submission (id),
    CONSTRAINT fk_submission_evidence_object FOREIGN KEY (evidence_object_id) REFERENCES evidence_object (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交与材料关联';

CREATE TABLE evidence_access_audit (
    id CHAR(36) NOT NULL,
    evidence_object_id CHAR(36) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    access_action VARCHAR(32) NOT NULL,
    accessed_at TIMESTAMP(6) NOT NULL,
    request_id VARCHAR(128) NULL,
    PRIMARY KEY (id),
    KEY ix_evidence_access_object_time (evidence_object_id, accessed_at),
    CONSTRAINT fk_evidence_access_object FOREIGN KEY (evidence_object_id) REFERENCES evidence_object (id),
    CONSTRAINT ck_evidence_access_action CHECK (access_action IN ('VIEW', 'DOWNLOAD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='材料访问审计';
