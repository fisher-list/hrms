# Epic 01 — 组织与员工主数据（Organization & Employee Master Data）

> 对标：SAP OM + PA / PeopleSoft Workforce Admin
> Sprint：1（含审批流引擎骨架）
> 责任产品：`product` · 责任架构：`architect` · 责任研发：`dev-be` / `dev-fe` / `dev-db`
> 引用：[`docs/prd.md`](../prd.md) §6 Epic 01 · [`docs/data-model.md`](../data-model.md) §2 + §7 · [`docs/architecture.md`](../architecture.md) §4 §7 §8

## 业务目标
建立单公司下多级组织树（公司→部门→岗位→职位）、员工档案完整生命周期，并打通登录认证 / RBAC / 数据权限 / 审计 / 轻量审批 5 大横切骨架，为其余 Epic 奠基。

## Epic 范围（按依赖排序的 5 个 Story）

| Story | 标题 | 主要产物 | 依赖 |
|---|---|---|---|
| **[EP01-S01](../stories/EP01-S01-bootstrap-auth-skeleton.md)** | 工程骨架 + 登录认证 + 通用基建 | hrms-app 启动 / hrms-common / 登录页 / JWT / 全局异常 / R<T> / Liquibase 主入口 | — |
| **[EP01-S02](../stories/EP01-S02-rbac-data-permission.md)** | RBAC 角色权限 + 数据权限拦截器 | sys_user/role/permission 5 表 + `@HasPermission` + 数据权限切面 + 角色管理页 | S01 |
| **[EP01-S03](../stories/EP01-S03-approval-engine.md)** | 轻量审批流引擎（4 表骨架） | approval_definition/instance/task/history + ApprovalService + 审批待办页 + 事件总线 | S01, S02 |
| **[EP01-S04](../stories/EP01-S04-org-tree.md)** | 组织树（公司/部门/岗位/职位） | company/department/position/job CRUD + 部门树循环引用校验 + 树形 UI | S01, S02 |
| **[EP01-S05](../stories/EP01-S05-employee-master.md)** | 员工档案 + 入离调转状态机 | employee + 6 张子表 + HireForm/TransferForm/TerminationForm + 状态机 + 字段加密/脱敏 + 审计 + 自动建账号 | S01-S04 |

## Epic 验收（Done 判定）
- 所有 5 个 Story 均已 close（包含测试通过 + 审核 P0/P1 全消）
- 端到端可演示：Admin 登录 → 建部门/岗位 → 员工入职（自动建账号）→ 员工首登强制改密 → 提调岗审批 → 离职/账号禁用，全流程留痕
- Liquibase changelog 在 PostgreSQL 与 MySQL 均可正向初始化
- 审计日志可查、敏感字段（身份证/银行卡）列表脱敏 / 详情明文（按角色）

## Epic 内的硬边界（防蔓延，引自 PRD §6 Epic 01）
- 不做多公司/多法人合并；根节点 1 家公司常量化
- 不做调薪历史（薪资变化由 Epic 03 处理）
- 不做矩阵汇报、跨部门外派
- 不做状态可逆（误操作走反向单）
- 不做按日折算
- 不做家属信息独立审批流
