# EP01-S05 — 员工档案 + 入离调转状态机

> Status: **Ready for Dev** · Sprint 1 · Owner: `dev-be` + `dev-fe` + `dev-db`
> 依赖：EP01-S01, EP01-S02, EP01-S03, EP01-S04
> 引用：[`docs/architecture.md`](../architecture.md) §7 §9.4 · [`docs/prd.md`](../prd.md) §6 Epic 01 · [`docs/data-model.md`](../data-model.md) §2.2.5~2.2.11 §2.3 §7.4

## 业务背景
本 Story 是 Epic 01 的核心收官：员工主表 + 6 张子表 CRUD、入离调转 3 张变更单（HireForm / TransferForm / TerminationForm）走 S03 审批流、employee.status 状态机不可逆、敏感字段 AES 加密 + 列表脱敏、自动建账号、字段级审计。完成后整个 Epic 01 端到端可演示。

## 验收标准（AC）
1. 7 张表（`employee` + 6 张子表）通过 Liquibase 建好；演示种子 20 名员工。
2. 员工档案完整 CRUD：基本信息 / 合同 / 教育 / 工作经历 / 紧急联系人 / 银行卡 / 家属，子表均挂在员工详情页 tab 上。
3. **身份证查重**：创建员工时若 `id_card_enc` 解密后命中已有记录（含 deleted=1 / status=TERMINATED），拒绝并提示"已存在 [姓名]，请走重新雇佣流程或联系 Admin 合并"（PRD §6 AC 2）。
4. **入职单 HireForm**：HR_MANAGER 录入候选员工信息 + 关联岗位 + 生效日 → 提交触发 S03 审批流（1 级 ADMIN 审批）→ 通过后系统**自动**：a) 创建 employee（status=PROBATION 或 ACTIVE 视试用期）b) 调用 `PositionService.incrOccupied` c) 创建 sys_user（密码=身份证后 6 位 BCrypt，强制改密标记）d) 关联角色 EMPLOYEE，e) 写审计。
5. **调岗单 TransferForm**：HR_MANAGER 选员工 + 新岗位 + 生效日 → 提交触发 1～2 级审批 → 通过后：a) 旧 position.occupied -1，新 position.occupied +1 b) employee.dept_id / position_id 更新 c) 写审计 d) 老岗位若 occupied 变 0 不影响岗位本身。
6. **离职单 TerminationForm**：HR_MANAGER 选员工 + 末日 + 原因 → 提交触发 1 级 HR_MANAGER 审批 → 通过后：a) employee.status=TERMINATED 且 termination_date 写入 b) sys_user.enabled=false c) 该员工的所有 PENDING 待办触发 S03 的 reassign d) position.occupied -1 e) 发布 `EmployeeTerminatedEvent`。
7. **状态机不可逆**：直接 PUT `/api/employees/{id}/status` 接口**不开放**；状态变更只能通过 3 种变更单审批后由系统改写；任何反向需求（误离职）只能新建反向 HireForm 单（系统提示并记录 reverse_of_form_id）。
8. **敏感字段加密**：`id_card_enc` / `account_no_enc` 用 AES-256-GCM 存储（密钥 `${HRMS_AES_KEY}` 环境变量），存查走 `@Sensitive` 注解 + TypeHandler。
9. **脱敏展示**：列表页 `id_card`→`110**********1234`、`phone`→`138****1234`、`account_no`→`****1234`；详情页 Admin/HR_MANAGER 可点"查看明文"按钮显示原值（再次审计）。
10. **字段级审计**：员工字段编辑（特别是 dept_id/position_id/manager_id/employment_type/banking）写 `sys_audit_log` 含 `before` / `after` JSON；薪资字段（Epic 03 接管）此处仅留扩展点。
11. **Line Manager 数据权限**（沿用 S02）：Line Manager 调 `/api/employees` 仅看本部门 + 子树员工；Admin/HR_MANAGER 全公司。
12. 删除/改部门触发 S04 的"是否仍有 active 员工"校验（依赖 S04）。
13. 演示路径打通：登录 admin → 建 1 部门/岗位（S04）→ 提交入职单 → admin 审批 → 员工自动建账号 → 切到员工登录强制改密 → admin 提调岗 → 审批 → admin 提离职 → 审批 → 账号禁用 ✅

## 技术上下文

### 数据模型（详见 data-model.md §2.2.5~2.2.11）
- `employee`：含 status 5 状态机（PENDING_HIRE / PROBATION / ACTIVE / ON_LEAVE / TERMINATED）
- 6 张子表：contract / education / work_experience / emergency_contact / bank_account / family
- 3 张变更单表（本 Story 新增）：
  - `hr_hire_form`：`id` / `name` / `id_card_enc` / `dept_id` / `position_id` / `effective_date` / `employment_type` / `probation_months` / `contract_info`(JSON) / `applicant_id` / `approval_instance_id` / `status`(DRAFT/PENDING/APPROVED/REJECTED/REVOKED) / `created_employee_id`（成功后回写）
  - `hr_transfer_form`：`id` / `employee_id` / `from_dept_id` / `from_position_id` / `to_dept_id` / `to_position_id` / `effective_date` / `reason` / `applicant_id` / `approval_instance_id` / `status`
  - `hr_termination_form`：`id` / `employee_id` / `termination_date` / `reason_code` / `reason_text` / `handover_to_id` / `applicant_id` / `approval_instance_id` / `reverse_of_form_id`（误操作反向单关联）/ `status`

### 状态机非法转移
合法转移（详 data-model.md §2.3）：
- `PENDING_HIRE → PROBATION`（入职完成）
- `PROBATION → ACTIVE`（转正，本 Story 不做转正流程，留 Service 钩子）
- `ACTIVE ↔ ON_LEAVE`（长期请假，由 Epic 02 触发）
- `PROBATION/ACTIVE → TERMINATED`（终态）

### 审批联动（依赖 S03）
- `HR_HIRE_FORM` 定义：1 节点，`approverRule=ROLE:ADMIN`
- `HR_TRANSFER_FORM` 定义：2 节点，`EMP_MANAGER` → `ROLE:HR_MANAGER`（同岗位序列内可配 1 节点，跨序列 2 节点；MVP 简化恒 2 节点）
- `HR_TERMINATION_FORM` 定义：1 节点，`approverRule=ROLE:HR_MANAGER`
- 监听 `ApprovalCompletedEvent`：根据 businessType 路由到对应 Service 的 `onApproved(formId)` 回调

### AES 加密
- 工具类 `AesGcmCipher`：256-bit key，IV 随机 12 字节前置在密文，base64 编码
- TypeHandler `AesGcmTypeHandler` 自动加解密；字段标 `@TableField(typeHandler=AesGcmTypeHandler.class)`
- 密钥从 `${HRMS_AES_KEY}` 读，启动时若未配置直接 fail-fast

### 涉及的关键代码契约
- `EmployeeService` / `EmployeeMapper`（含查重接口 `findByIdCard(plain)` 内部加密查询）
- `HireFormService.submit() / onApproved()` / `TransferFormService` / `TerminationFormService`
- `EmployeeStateMachine.transition(employee, newStatus, reason)` —— 非法转移抛 BizException
- `IdCardSensitiveValidator`（Bean Validation 自定义注解，校验 18 位身份证 + 校验位）
- 前端：员工列表/详情/新建/调岗/离职页 + Tab 子表

## Tasks
### 后端（dev-be）
- [ ] T1 7 张主子表 Mapper / Service / Controller
- [ ] T2 `AesGcmCipher` + `AesGcmTypeHandler` + `@Sensitive` 字段标注
- [ ] T3 列表脱敏切面（按 `@Sensitive` 自动脱敏 DTO 输出）+ 详情明文接口（带审计）
- [ ] T4 `EmployeeStateMachine` + 状态变更必经审批保护
- [ ] T5 3 张变更单的 Service：submit / cancel（DRAFT 才能撤回）/ `onApproved` 回调
- [ ] T6 监听 `ApprovalCompletedEvent` 路由到 3 种 Service
- [ ] T7 身份证查重逻辑（注意：要解密后 in 内存比对 or 加密后等值查询）
- [ ] T8 集成 S04 的 `PositionService.incrOccupied/decrOccupied` 在入职/调岗/离职时调用
- [ ] T9 与 S01 的 sys_user 集成：入职审批通过自动建账号 + 默认 EMPLOYEE 角色
- [ ] T10 字段级审计切面（`@AuditField` 标注关键字段，编辑前后写 sys_audit_log）
- [ ] T11 单测覆盖：身份证查重、状态机非法转移、入职完整链路、调岗 occupied 维护、离职 reassign

### 数据库（dev-db）
- [ ] T12 `changelog-02-employee.yaml`：7 张主子表 + 3 张变更单表 + 唯一索引 + 业务索引（详见 data-model §8.1）
- [ ] T13 种子审批定义 3 条（HR_HIRE_FORM / HR_TRANSFER_FORM / HR_TERMINATION_FORM）
- [ ] T14 演示种子：20 名员工（覆盖 5 部门 6 岗位的合理分布）
- [ ] T15 在 PG + MySQL 各跑一遍 update + rollback

### 前端（dev-fe）
- [ ] T16 员工列表页（数据权限自动生效）+ 创建/编辑表单（多 tab 子表）
- [ ] T17 员工详情页 + "查看明文"按钮（Admin/HR_MANAGER 可见）
- [ ] T18 入职/调岗/离职 3 个变更单页 + 提交后跳转待办
- [ ] T19 表格脱敏展示（前端二次脱敏作为防护，主防线在后端）

### 联调
- [ ] T20 串完整 13 条 AC 演示路径，特别确认密码首登强改 + Line Manager 数据权限

## 测试要点
- 入职完整链路（提交→审批→自动建账号 + 改密 + 加角色）
- 重复身份证（含已离职员工）→ 拒绝
- 调岗：旧岗位 occupied -1、新岗位 occupied +1
- 离职：账号禁用 + 待办 reassign（关联 S03）
- Line Manager 仅看本部门子树
- 列表脱敏 / 详情明文（带审计记录）
- AES 密钥未配 → 启动失败（fail-fast 验证）
- 状态机：直接调 PUT status 接口被拒（接口本就不存在）

## Definition of Done
- [ ] Tasks 全做完
- [ ] 单测 ≥60%
- [ ] qa-lead 13 条 AC 全部用例通过
- [ ] reviewer P0/P1 全消（重点查：身份证/银行卡是否真加密、列表是否脱敏、Controller `@HasPermission` 是否齐、状态机非法转移是否拦、删除前校验是否在事务、字段级审计是否覆盖）
- [ ] Epic 01 演示路径端到端走通

## Defects
（待填）

## Review Findings
（待填）
