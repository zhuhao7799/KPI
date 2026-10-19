-- KPI 业务菜单与按钮权限；可重复执行
INSERT IGNORE INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,active_menu,ext,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
(1761400000000000900,'绩效管理',0,5,'performance',NULL,'','N','Y','M','0','0','','chart','','',NULL,1761100000000000001,NOW(),NULL,NULL,'高校绩效考核业务目录'),
(1761400000000000901,'指标体系管理',1761400000000000900,1,'indicators','performance/indicators/index','','N','Y','C','0','0','kpi:indicator:list','list','','',NULL,1761100000000000001,NOW(),NULL,NULL,'指标版本和指标项管理'),
(1761400000000000902,'发布指标版本',1761400000000000901,1,'','','','N','Y','F','1','0','kpi:indicator:publish','#','','',NULL,1761100000000000001,NOW(),NULL,NULL,'发布指标版本'),
(1761400000000000903,'考核周期管理',1761400000000000900,2,'cycles','performance/cycles/index','','N','Y','C','0','0','kpi:cycle:list','date','','',NULL,1761100000000000001,NOW(),NULL,NULL,'考核周期管理'),
(1761400000000000904,'绩效方案管理',1761400000000000900,3,'schemes','performance/schemes/index','','N','Y','C','0','0','kpi:scheme:list','tree-table','','',NULL,1761100000000000001,NOW(),NULL,NULL,'绩效方案管理'),
(1761400000000000905,'新增方案',1761400000000000904,1,'','','','N','Y','F','1','0','kpi:scheme:add','#','','',NULL,1761100000000000001,NOW(),NULL,NULL,'新增绩效方案'),
(1761400000000000906,'编辑方案',1761400000000000904,2,'','','','N','Y','F','1','0','kpi:scheme:edit','#','','',NULL,1761100000000000001,NOW(),NULL,NULL,'编辑绩效方案'),
(1761400000000000907,'删除方案',1761400000000000904,3,'','','','N','Y','F','1','0','kpi:scheme:remove','#','','',NULL,1761100000000000001,NOW(),NULL,NULL,'删除绩效方案'),
(1761400000000000908,'发布方案',1761400000000000904,4,'','','','N','Y','F','1','0','kpi:scheme:publish','#','','',NULL,1761100000000000001,NOW(),NULL,NULL,'发布绩效方案');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT 1761300000000000001,menu_id FROM sys_menu WHERE menu_id BETWEEN 1761400000000000900 AND 1761400000000000908;
