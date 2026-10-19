ALTER TABLE performance_scheme
    ADD COLUMN indicator_version_id CHAR(36) NULL AFTER organization_category,
    ADD KEY ix_performance_scheme_indicator_version (indicator_version_id),
    ADD CONSTRAINT fk_performance_scheme_indicator_version
        FOREIGN KEY (indicator_version_id) REFERENCES indicator_version(id);
