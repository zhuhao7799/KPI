# 指标体系管理详细设计

## 1. 设计目标

建立可审计、可发布、可引用的指标管理流程，明确指标体系、指标版本、指标明细和绩效方案之间的边界，避免页面入口、接口行为和数据库约束不一致。

本模块采用“状态提示、变更校验、引用影响可追踪”的生命周期模型。状态用于提示当前使用情况，不作为不必要的操作阻断；系统重点保证数据一致性、影响可追踪和变更可审计。

## 2. 业务对象

### 2.1 指标体系

指标体系是长期业务对象，用于表达一套指标的业务主题。

字段：体系编码、体系名称、适用范围、负责人、状态、说明、创建时间、更新时间。

指标体系不直接保存可评分的指标明细，指标明细必须归属于某个指标版本。

### 2.2 指标版本

指标版本是可编辑、可发布、可引用的配置快照。

字段：体系 ID、版本编码、版本名称、评分公式、评分精度、状态、发布人、发布时间、备注。

状态（用于提示和流程记录，不作为普通编辑的绝对限制）：

```text
DRAFT      草稿，允许维护
PUBLISHED  已发布，仍允许维护；保存前执行引用和锁定一致性检查
ARCHIVED   已归档，仍可维护；是否作为默认版本由业务选择
```

约束：同一指标体系下版本编码唯一；状态允许根据业务流程调整；已发布或已引用版本允许修改，但修改前必须检查引用影响，并记录变更前后内容和影响范围。

### 2.3 指标明细

指标明细只属于一个指标版本。

字段：指标编码、指标名称、分类、指标性质、计量单位、权重、满分、评分方式、是否需要佐证材料、指标说明、排序号。

所有未归档版本均可新增、编辑、删除和排序。已被引用的版本仍允许修改，但必须执行一致性校验，并向用户展示受影响的方案、周期和填报数据。

### 2.4 绩效方案

绩效方案引用已发布的指标版本，不复制或修改版本内容。方案有自己的业务状态和使用范围。

指标版本被绩效方案、考核周期或填报记录引用后，仍可修改，但必须先完成一致性检查并记录影响范围；存在历史结果时不得物理删除。

## 3. 生命周期

### 3.1 指标体系

```text
新建 → 正常 → 停用
```

停用体系不能创建新版本，但保留历史版本和审计记录。

### 3.2 指标版本

```text
创建草稿 → 编辑基本信息和明细 → 发布 → 归档
                         ↓
                       删除
```

删除前必须检查版本是否存在引用、是否存在锁定业务数据以及是否会破坏历史结果。存在引用时默认不物理删除，改为停用或归档；确认没有一致性风险时才允许删除。

发布和修改前校验：版本基本信息完整、指标编码唯一、每项权重和满分为正、评分公式对应的规则通过、引用对象不会产生无法解释的数据差异。权重总和是否必须为 100% 由评分公式定义，不在通用实体中硬编码。

## 4. 页面结构

页面必须遵循系统现有“单表”模板：筛选卡片、列表卡片、顶部操作、表格、行级操作。

### 4.1 指标体系列表

筛选：体系编码、体系名称、适用范围、状态。

顶部操作：新增、批量停用、刷新。

表格：体系编码、体系名称、适用范围、负责人、版本数量、当前版本、状态、更新时间、操作。

行操作：查看版本、编辑、停用。

### 4.2 指标版本列表

进入某个指标体系后显示版本列表。

顶部操作：新建版本、版本对比、刷新。

表格：版本编码、版本名称、评分公式、指标数量、状态、发布时间、引用数量、操作。

行操作：查看明细、编辑基本信息、发布、归档、删除草稿、复制版本。

### 4.3 指标版本明细

页面顶部显示版本信息和状态。

草稿操作：编辑基本信息、新增指标、导入模板、导出、发布、删除、操作记录。

表格：指标编码、指标名称、分类、性质、单位、权重、满分、排序、操作。

行操作：编辑、删除、上移、下移。

已发布或已归档版本仍显示编辑、导入、导出等操作，但提交前必须展示一致性检查结果和影响范围。

### 4.4 指标编辑页面

一个页面只编辑一个指标，字段按业务分组：基本信息、评分配置、材料要求、说明。

保存后返回版本明细；取消不保存；编码在同一版本内唯一。

### 4.5 导入导出

版本明细页面直接提供：下载模板、导入、导出。

模板字段：

```text
指标编码,指标名称,指标分类,指标性质,计量单位,权重,满分,评分方式,需要佐证材料,指标说明
```

导入只允许草稿版本，导入前预览错误行，确认后整体替换草稿明细；导入失败不得产生部分数据。导出包含当前版本全部明细和版本元数据。

## 5. 接口设计

### 5.1 指标体系

```text
GET    /kpi/indicator-systems
POST   /kpi/indicator-systems
PUT    /kpi/indicator-systems/{code}
POST   /kpi/indicator-systems/{code}/disable
```

### 5.2 指标版本

```text
GET    /kpi/indicator-systems/{systemCode}/versions
GET    /kpi/indicator-versions/{version}
POST   /kpi/indicator-versions/draft
PUT    /kpi/indicator-versions/{version}/metadata
POST   /kpi/indicator-versions/{version}/publish
POST   /kpi/indicator-versions/{version}/archive
DELETE /kpi/indicator-versions/{version}/draft
POST   /kpi/indicator-versions/{version}/clone
GET    /kpi/indicator-versions/{version}/references
GET    /kpi/indicator-versions/{version}/audit
```

### 5.3 指标明细

```text
GET    /kpi/indicator-versions/{version}/items
POST   /kpi/indicator-versions/{version}/items
PUT    /kpi/indicator-versions/{version}/items/{code}
DELETE /kpi/indicator-versions/{version}/items/{code}
POST   /kpi/indicator-versions/{version}/items/import
GET    /kpi/indicator-versions/{version}/items/export
POST   /kpi/indicator-versions/{version}/items/reorder
```

所有写接口都必须校验当前用户权限、数据一致性、引用影响和数据归属；状态用于提示和流程记录，不直接阻断普通编辑；错误响应返回稳定的业务错误码和可读消息。

## 6. 数据库设计

核心表：

```text
kpi_indicator_system
kpi_indicator_version
kpi_indicator_item
kpi_indicator_reference
kpi_indicator_audit
```

关键索引和约束：

- 体系编码唯一
- 体系内版本编码唯一
- 版本内指标编码唯一
- 明细排序号按版本建立索引
- 引用表记录引用类型、引用对象、引用状态
- 审计表记录操作人、操作类型、操作前后摘要、时间

发布、修改和删除时在同一事务中完成一致性检查、数据变更和审计记录；已产生历史结果的版本采用归档或停用，不做物理删除。

## 7. 权限设计

```text
kpi:indicator-system:list
kpi:indicator-system:create
kpi:indicator-system:update
kpi:indicator-system:disable
kpi:indicator-version:list
kpi:indicator-version:create
kpi:indicator-version:update
kpi:indicator-version:publish
kpi:indicator-version:archive
kpi:indicator-version:delete
kpi:indicator-version:import
kpi:indicator-version:export
```

按钮权限只负责显示控制，后端接口必须再次校验权限和业务状态。

## 8. 错误处理

错误码必须区分：

```text
KPI_VERSION_NOT_FOUND
KPI_VERSION_IMMUTABLE
KPI_VERSION_REFERENCED
KPI_VERSION_LOCKED
KPI_VERSION_EMPTY_ITEMS
KPI_ITEM_DUPLICATE_CODE
KPI_IMPORT_INVALID_FORMAT
KPI_IMPORT_VALIDATION_FAILED
KPI_SCORE_RULE_INVALID
```

禁止把所有删除异常统一返回 `cannot delete indicator draft`，前端必须展示可执行的业务原因。

## 9. 验收流程

1. 新建指标体系，只填写体系基本信息。
2. 在体系下创建空草稿版本，只填写版本基本信息。
3. 进入版本明细，新增、编辑、删除、排序单个指标。
4. 下载模板，导入合法模板并预览成功。
5. 导入错误模板，显示错误行且数据库不产生部分更新。
6. 导出当前版本并核对字段和数据。
7. 发布版本，确认页面展示已发布状态和引用影响提示。
8. 将已发布版本绑定绩效方案和考核周期。
9. 尝试删除已引用版本，显示明确引用原因并阻止删除。
10. 复制已发布版本，生成新的草稿版本并保持原版本不变。
11. 检查所有写操作均有审计记录。

## 10. 实施原则

先完成领域模型、状态机、数据库约束和接口契约，再实现页面。前端页面必须严格对应接口和状态，不在前端单独定义与后端冲突的业务规则。每个功能完成后按本设计的验收流程验证，未通过前不标记为完成。
