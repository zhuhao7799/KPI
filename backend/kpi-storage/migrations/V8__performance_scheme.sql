CREATE TABLE performance_scheme (
    id CHAR(36) NOT NULL,
    scheme_code VARCHAR(64) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    organization_category VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    formula_version VARCHAR(64) NOT NULL DEFAULT 'weighted-score-v1',
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_performance_scheme_code (scheme_code),
    KEY ix_performance_scheme_category_status (organization_category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效方案';

CREATE TABLE performance_scheme_indicator (
    id CHAR(36) NOT NULL,
    scheme_id CHAR(36) NOT NULL,
    indicator_code VARCHAR(64) NOT NULL,
    indicator_name VARCHAR(200) NOT NULL,
    weight DECIMAL(9,4) NOT NULL,
    maximum_score DECIMAL(12,4) NOT NULL DEFAULT 100,
    sort_order INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scheme_indicator (scheme_id, indicator_code),
    CONSTRAINT fk_scheme_indicator_scheme FOREIGN KEY (scheme_id) REFERENCES performance_scheme(id),
    CONSTRAINT ck_scheme_indicator_weight CHECK (weight > 0),
    CONSTRAINT ck_scheme_indicator_maximum CHECK (maximum_score > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效方案指标';
