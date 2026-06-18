# EP01-S03 — 轻量审批流引擎（4 表骨架）

> Status: **Ready for Dev** · Sprint 1 · Owner: `dev-be` (主) + `dev-fe` (待办页)
> 依赖：EP01-S01, EP01-S02
> 引用：[`docs/architecture.md`](../architecture.md) §8 · [`docs/prd.md`](../prd.md) §7.7 · [`docs/data-model.md`](../data-model.md) §7.5 · [`.bmad/decisions.md`](../../.bmad/decisions.md) ADR-003

## 业务背景
HRMS 至少 6 类业务（请假/加班/Offer/工资 run/调岗/离职）共用一套审批闸口。ADR-003 决定自研 4 表 + Spring Event 事件总线模型。本 Story 仅交付通用引擎与待办 UI，不承载业务，验证用 mock "请休假" 流程。

## 验收标准（AC）
1. 4 张表（`approval_definition` / `approval_instance` / `approval_task` / `approval_history`）通过 Liquibase 建好。
2. 提供 `ApprovalService` 接口：
   - `start(definitionCode, businessKey, businessType, payload, applicantId)` → 返回 instanceId 并生成第一节点 task
   - `approve(taskId, approverId, comment)` / `reject(taskId, approverId, comment)` / `revoke(taskId, approverId)`（回退到上一节点）
   - 支持最多 **3 级串行**；节点定义存 `approval_definition.nodes` JSON
3. **审批人离职**自动顺延：节点 `approverRule` 配 `ROLE:HR_MANAGER` 时，任一审批人离职后系统选另一在岗 HR_MANAGER；无可选人则挂起 + Admin 收通知（PRD §7.7）。
4. 审批完成后发布 `ApprovalCompletedEvent(businessType, businessKey, result)` Spring Event；业务模块通过 `@EventListener` 接收联动（本 Story 内 mock 监听器只 log）。
5. 待办接口 `GET /api/approval/todo`：返回当前用户作为审批人的所有 PENDING task；前端"我的待办"页可看到、可点开详情、可点同意/拒绝/回退。
6. `approval_history` 完整记录每一步动作（taskId/approverId/action/comment/timestamp），不可修改不可删除。
7. 状态机：instance.status 在 `PENDING` / `APPROVED` / `REJECTED` / `REVOKED` / `SUSPENDED` 间合法转移，非法转移抛 BizException。
8. 单元测试覆盖：3 级串行通过、3 级中间拒绝、回退到上一节点、审批人离职顺延、挂起场景。

## 技术上下文

### 数据模型（详见 data-model.md §7.5）
- `approval_definition`：`code` UK / `name` / `business_type`（如 LEAVE/OFFER/HIRE_FORM）/ `version` / `nodes`（JSON 字符串：`[{seq, name, approverRule}]`）/ `enabled`
- `approval_instance`：`id` / `definition_id` / `business_type` / `business_key`（关联业务单 id 字符串）/ `applicant_id` / `current_node_seq` / `status` / `payload`（JSON 字符串）/ `started_at` / `finished_at`
- `approval_task`：`id` / `instance_id` / `node_seq` / `assignee_id` / `status`(PENDING/APPROVED/REJECTED/REVOKED) / `assigned_at` / `acted_at` / `parallel_group`（预留并行用，MVP 始终 null）
- `approval_history`：`id` / `instance_id` / `task_id` / `node_seq` / `actor_id` / `action`(START/APPROVE/REJECT/REVOKE/AUTO_REASSIGN/SUSPEND) / `comment` / `acted_at`

### 节点配置 JSON 示例
```json
[
  {"seq": 1, "name": "直属经理", "approverRule": "EMP_MANAGER"},
  {"seq": 2, "name": "HR 审核", "approverRule": "ROLE:HR_MANAGER"},
  {"seq": 3, "name": "Admin 终审", "approverRule": "ROLE:ADMIN"}
]
```
`approverRule` 取值：`EMP_MANAGER`（取申请人 manager_id）/ `ROLE:<roleCode>`（任意持该角色在岗用户）/ `USER:<userId>`（指定人）。

### 涉及的关键代码契约
- `ApprovalService.start(...)`：写 instance + history(START) + 第一节点 task；触发审批人解析
- `ApprovalService.approve/reject/revoke(...)`：写 history、更新 task & instance、若到末节点发 `ApprovalCompletedEvent`
- `ApproverResolver`：把 `approverRule` 解析成具体 `assignee_id`（含离职顺延逻辑）
- `EmployeeStatusListener`（监听员工 TERMINATED 事件）：扫所有 PENDING task，被 reassign 的写 `AUTO_REASSIGN` history，无可选人则 SUSPEND + 通知 Admin
- 测试用 mock 业务类型 `MOCK_LEAVE`，不真接请假表

## Tasks
### 后端（dev-be）
- [ ] T1 4 张表的 Mapper / Service
- [ ] T2 `ApprovalService` 主流程（start/approve/reject/revoke）+ 状态机校验
- [ ] T3 `ApproverResolver`（3 种 approverRule + 离职顺延 + 挂起场景）
- [ ] T4 `EmployeeStatusListener` 监听员工离职事件 reassign
- [ ] T5 `ApprovalCompletedEvent` 定义 + 发布
- [ ] T6 `ApprovalController`：`GET /api/approval/todo` / `POST /api/approval/tasks/{id}/approve|reject|revoke`
- [ ] T7 单测覆盖 8 个 AC 场景

### 数据库（dev-db）
- [ ] T8 写 `changelog-00-system-approval.yaml`：4 张表 + 索引（重点 `idx_task_assignee_status` `idx_instance_business`）
- [ ] T9 种子 1 条 mock 审批定义 `MOCK_LEAVE`，3 节点串行

### 前端（dev-fe）
- [ ] T10 `views/approval/Todo.vue`：el-table 待办列表 + 详情抽屉（展示 payload + 历史轨迹）+ 同意/拒绝/回退按钮
- [ ] T11 详情中展示完整 history 时间线（element timeline）

### 联调
- [ ] T12 mock：手工 `start("MOCK_LEAVE", "biz-1", "MOCK_LEAVE", "{}", admin)` → 第一审批人登录看到待办 → 同意 → 第二审批人看到 → 同意 → 第三 → 同意 → 完成事件 log 出现

## 测试要点
- 3 级全通过
- 中间节点拒绝 → instance=REJECTED
- 第二节点回退到第一 → 重新走第一节点
- 第二节点审批人离职 → 自动顺延到下一在岗 HR_MANAGER（写 AUTO_REASSIGN history）
- 第二节点 ROLE:HR_MANAGER 全员离职 → instance=SUSPENDED + Admin 收通知

## Definition of Done
- [ ] Tasks 全做完
- [ ] 单测覆盖 8 AC 场景
- [ ] qa-lead 用例 100% 通过
- [ ] reviewer P0/P1 全消（重点查：状态机非法转移是否拦、history 是否禁止改/删、`approverRule` JSON 是否安全解析）
- [ ] AC 1-8 演示通过

## Defects
（待填）

## Review Findings
（待填）
