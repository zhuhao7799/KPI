-- RuoYi 官方演示基线中有一条昵称超过严格模式字段长度。
-- 仅对这次导入会话放宽模式；后续绩效迁移仍以 MySQL 默认严格模式执行。
SET SESSION sql_mode = '';
SOURCE /seed/ry_vue.sql;
