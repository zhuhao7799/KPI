CREATE TABLE IF NOT EXISTS indicator_system (
    id CHAR(36) NOT NULL,
    system_code VARCHAR(64) NOT NULL,
    system_name VARCHAR(200) NOT NULL,
    scope VARCHAR(200) NULL,
    owner_id VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    description VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_indicator_system_code (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标体系';

INSERT INTO indicator_system (id, system_code, system_name, status, description)
SELECT UUID(), 'DEFAULT', '默认指标体系', 'NORMAL', '历史指标版本迁移归属'
WHERE NOT EXISTS (SELECT 1 FROM indicator_system WHERE system_code = 'DEFAULT');

SET @indicator_system_id = (SELECT id FROM indicator_system WHERE system_code = 'DEFAULT' LIMIT 1);

ALTER TABLE indicator_version
    ADD COLUMN indicator_system_id CHAR(36) NULL AFTER id;

UPDATE indicator_version
SET indicator_system_id = @indicator_system_id
WHERE indicator_system_id IS NULL;

ALTER TABLE indicator_version
    ADD KEY ix_indicator_version_system (indicator_system_id);

ALTER TABLE indicator_version
    ADD CONSTRAINT fk_indicator_version_system
    FOREIGN KEY (indicator_system_id) REFERENCES indicator_system(id);
