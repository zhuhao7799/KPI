CREATE TABLE kpi_role (
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    PRIMARY KEY (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效业务角色';

INSERT INTO kpi_role (role_code, role_name) VALUES
    ('SCHOOL_ADMIN', '校级系统管理员'),
    ('ASSESSMENT_OFFICE', '考核办'),
    ('UNIT_EDITOR', '单位填报人'),
    ('REVIEWER', '审核人'),
    ('EVALUATOR', '评委'),
    ('RESULT_VIEWER', '结果查询人');

CREATE TABLE kpi_user_role_binding (
    id CHAR(36) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_kpi_user_role_period (user_id, role_code, starts_at),
    CONSTRAINT fk_kpi_user_role FOREIGN KEY (role_code) REFERENCES kpi_role (role_code),
    CONSTRAINT ck_kpi_user_role_dates CHECK (ends_at IS NULL OR ends_at >= starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户绩效角色绑定';

CREATE TABLE kpi_scope_grant (
    id CHAR(36) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    assessment_cycle_id CHAR(36) NOT NULL,
    organization_unit_snapshot_id CHAR(36) NULL,
    permission_code VARCHAR(64) NOT NULL,
    granted_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_kpi_scope_grant (user_id, assessment_cycle_id, organization_unit_snapshot_id, permission_code),
    KEY ix_kpi_scope_user_cycle (user_id, assessment_cycle_id),
    CONSTRAINT fk_kpi_scope_cycle FOREIGN KEY (assessment_cycle_id) REFERENCES assessment_cycle (id),
    CONSTRAINT fk_kpi_scope_unit FOREIGN KEY (organization_unit_snapshot_id) REFERENCES organization_unit_snapshot (id),
    CONSTRAINT ck_kpi_scope_dates CHECK (expires_at IS NULL OR expires_at >= granted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效数据范围授权';
