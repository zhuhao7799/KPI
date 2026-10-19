ALTER TABLE indicator_item_snapshot
    ADD COLUMN indicator_category VARCHAR(64) NOT NULL DEFAULT '通用指标' AFTER indicator_name,
    ADD COLUMN indicator_type VARCHAR(32) NOT NULL DEFAULT '定量' AFTER indicator_category,
    ADD COLUMN measurement_unit VARCHAR(64) NOT NULL DEFAULT '分' AFTER indicator_type,
    ADD COLUMN scoring_method VARCHAR(64) NOT NULL DEFAULT '加权评分' AFTER measurement_unit,
    ADD COLUMN evidence_required TINYINT(1) NOT NULL DEFAULT 0 AFTER scoring_method,
    ADD COLUMN description VARCHAR(1000) NULL AFTER evidence_required;
