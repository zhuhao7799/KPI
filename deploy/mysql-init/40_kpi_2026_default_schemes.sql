-- 2026 示例绩效方案。脚本可重复执行，适用于已执行 V8、V9、V10 的数据库。
INSERT INTO performance_scheme
    (id, scheme_code, scheme_name, organization_category, indicator_version_id, status, formula_version, created_by)
SELECT UUID(), 'COLLEGE-2026', '2026年度二级学院绩效方案', '二级学院', v.id, 'DRAFT', v.formula_version, 'system'
FROM indicator_version v
WHERE v.version_code = '2026-v1' AND v.status = 'PUBLISHED'
  AND NOT EXISTS (SELECT 1 FROM performance_scheme WHERE scheme_code = 'COLLEGE-2026');

INSERT INTO performance_scheme
    (id, scheme_code, scheme_name, organization_category, indicator_version_id, status, formula_version, created_by)
SELECT UUID(), 'OFFICE-2026', '2026年度职能部门绩效方案', '职能部门', v.id, 'DRAFT', v.formula_version, 'system'
FROM indicator_version v
WHERE v.version_code = '2026-v1' AND v.status = 'PUBLISHED'
  AND NOT EXISTS (SELECT 1 FROM performance_scheme WHERE scheme_code = 'OFFICE-2026');

UPDATE performance_scheme s
JOIN indicator_version v ON v.version_code = '2026-v1' AND v.status = 'PUBLISHED'
SET s.indicator_version_id = v.id
WHERE s.scheme_code IN ('COLLEGE-2026', 'OFFICE-2026')
  AND s.indicator_version_id IS NULL;

INSERT IGNORE INTO performance_scheme_indicator
    (id, scheme_id, indicator_code, indicator_name, weight, maximum_score, sort_order)
SELECT UUID(), s.id, i.indicator_code, i.indicator_name, i.weight, i.maximum_score, i.sort_order
FROM performance_scheme s
JOIN indicator_version v ON v.id = s.indicator_version_id AND v.version_code = '2026-v1'
JOIN indicator_item_snapshot i ON i.indicator_version_id = v.id
WHERE s.scheme_code IN ('COLLEGE-2026', 'OFFICE-2026');
