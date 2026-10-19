# Warm-Flow 流程接入方案

RuoYi-Vue-Plus 6.0.0 已内置 Warm-Flow 模块和流程待办接口。KPI 业务保留自己的提交状态、权限和审计记录，Warm-Flow 负责流程定义、节点分配和待办展示，二者通过提交编号关联。

## 业务关联

| KPI 事件 | Warm-Flow 业务字段 | 状态同步 |
| --- | --- | --- |
| 单位提交 | `businessId=submission.id` | 启动 `KPI_ASSESSMENT_REVIEW` 流程，提交保持 `SUBMITTED` |
| 审核通过 | `businessId=submission.id` | 完成当前任务，KPI 变为 `CONFIRMED` |
| 审核退回 | `businessId=submission.id` | 完成当前任务，KPI 变为 `RETURNED` |
| 结果归档 | `businessId=submission.id` | 完成归档节点，KPI 变为 `ARCHIVED` |

## 接入边界

- KPI 适配层先完成角色、单位数据范围和状态机校验，再调用 Warm-Flow 的启动或办理服务。
- Warm-Flow 回调只允许携带已存在的提交编号；未知编号、重复办理和过期任务都拒绝落库。
- KPI 的 `assessment_approval_task` 作为同步审计投影保留，记录 Warm-Flow 实例和任务编号，便于结果追溯和故障补偿。
- 流程定义、节点候选人和转办/加签规则由学校流程管理员配置，不能写死在指标代码中。

## 当前实现状态

当前代码已完成数据库待办桥接和事务化状态变更，RuoYi 适配层仍以该桥接作为默认实现。下一阶段在可完整构建 RuoYi 的环境中接入 `IFlwTaskService`，补充实例编号投影、重复回调幂等和流程异常补偿；在此之前，数据库待办实现可用于开发和接口验收。
