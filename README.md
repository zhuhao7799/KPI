# 高校组织绩效考核平台

面向二级学院、职能部门的年度组织绩效考核平台。系统以“制度可配置、过程可追溯、结果可复算”为核心。

## 当前阶段

正式工程骨架已固定为：

- 管理端：RuoYi-Vue-Plus 官方 `plus-ui` 6.0.0，见 `frontend/`；
- 后端底座：RuoYi-Vue-Plus 官方 6.0.0，作为 Git 子模块固定在 `backend/ruoyi-vue-plus/`；
- 子模块接入：克隆后执行 `bash backend/apply-ruoyi-integration.sh`，将主仓库保存的集成补丁应用到固定版本；
- 绩效内核：独立 `backend/kpi-domain/`，避免业务评分规则被框架耦合；
- 数据库：MySQL 8、Flyway 风格的版本化迁移，见 `backend/kpi-storage/migrations/`；
- 基础设施：本地 MySQL 8 与 Redis 7，见 `deploy/compose.yaml`。
- RuoYi 基础框架启动：Linux/Git Bash 使用 `backend/run-ruoyi.sh`，Windows PowerShell 使用 `backend/run-ruoyi.ps1`。
- RuoYi 适配层：`backend/kpi-ruoyi-adapter/` 已提供周期、填报、材料、待办、结果和授权接口；集成补丁见 `backend/ruoyi-integration.patch`。

绩效内核已固定并验证以下规则：

- 考核周期、单位和指标版本在计算时形成快照；
- 指标权重必须合计 100；
- 每项评分必须处于该指标允许范围；
- 汇总分以 `原始得分 ÷ 满分 × 权重` 计算，保留四位小数；
- 结果带有可审计事件，便于后续接入数据库和工作流。

## 本地基础设施

```bash
cd deploy
cp .env.example .env
docker compose up -d
```

这只启动 MySQL 和 Redis；不写入任何学校生产数据。服务端与前端的本地配置、启动顺序和验收项见 [基础架构说明](docs/architecture/baseline-architecture.md)。

## 运行评分内核示例

需要 JDK 21：

```bash
cd backend/kpi-domain
./run-core-example.sh
```

该示例使用虚构单位和指标，不包含学校真实制度或材料。

## 运行评分预览纵切面

需要 JDK 21：

```bash
cd backend/kpi-app
./smoke-demo-api.sh
```

它会编译并短暂启动本地服务，验证健康检查、页面资源、评分预览和异常评分拒绝。持续运行时使用 `./run-demo-server.sh`，服务仅监听 `127.0.0.1:18080`。

## 查看单位填报模块

单位填报纵向模块可以一键启动演示 API 和 plus-ui 开发服务器：

```bash
./scripts/run-kpi-module-preview.sh
```

浏览器打开 `http://127.0.0.1:5173/index`，可查看绩效工作台、单位填报、材料关联、审核待办和结果页面。脚本使用临时数据目录，不写入学校正式数据；终止脚本即可同时停止前后端。

Windows PowerShell 使用 `.\scripts\run-kpi-module-preview.ps1`，需要 JDK 21、Node.js 20+ 和 pnpm 10；若 `javac` 未加入 PATH，可先设置 `$env:KPI_JDK_HOME`。

## 目录

- `docs/`：产品边界、架构决策和接口约束。
- `backend/kpi-domain/`：可独立运行的绩效领域核心。
- `backend/kpi-app/`：评分预览与结果说明的全栈纵切面。
- `backend/kpi-storage/migrations/`：MySQL 8 的快照、审计与 Outbox 迁移脚本。
- `docs/architecture/system-delivery-status.md`：正式系统的当前交付边界与上线缺口。
- `docs/architecture/warmflow-integration.md`：Warm-Flow 流程实例与 KPI 提交状态的关联方案。

## 当前实施顺序

1. 在可访问 Maven 仓库的环境完成 RuoYi 服务端编译、启动和认证联调；
2. 将当前数据库待办桥接接入 RuoYi 内置 Warm-Flow，并补齐实例投影和幂等回调；
3. 将开发期私有材料存储替换为学校选定的 OSS，并完成病毒扫描、备份和容量验收；
4. 导入学校正式组织、用户和指标规则，完成试运行、并发和安全验收。
