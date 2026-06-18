# HRMS 系统功能清单与实现状态分析

> **生成日期**：2026-06-19
> **项目路径**：/Users/apple/hrms
> **版本**：MVP v0.1.0
> **目的**：全面梳理 HRMS 已实现功能，为后续与 SAP HCM 对比做准备

---

## 一、系统概览

| 维度 | 数据 |
|---|---|
| 6 大 Epic | 组织员工 / 考勤假期 / 薪酬基础 / 招聘入职 / 绩效 / ESS-MSS |
| 14 个 Story | EP01-S01~S05 / EP02-S06~S08 / EP03-S09~S10 / EP04-S11~S12 / EP05-S13~S14 / EP06-E01~E04 |
| 后端 Controller 数量 | **34 个** |
| 前端 Vue 页面数 | **25 个** |
| 数据库表数量 | **~49 张**（11 个 changelog 文件） |
| 单元测试 | 104 个（95 通过，9 个 JDK 25 兼容性问题） |
| 后端模块 | hrms-common / hrms-org / hrms-employee / hrms-attendance / hrms-payroll / hrms-recruit / hrms-performance / hrms-portal-api / hrms-app |
| 前端技术栈 | Vue 3 + Vite + TypeScript + Element Plus + Pinia |
| 后端技术栈 | Spring Boot 3.x + MyBatis-Plus + Liquibase + JWT |
| 数据库支持 | PostgreSQL(默认) / MySQL 8 / Oracle / SQL Server / 达梦 |

---

## 二、按模块功能清单

### 模块 1：认证与系统基础（Auth / System）

| 功能点 | 后端 Controller | 前端页面 | 状态 |
|---|---|---|---|
| 用户名+密码登录 | `AuthController` → `POST /api/auth/login` | `Login.vue` | ✅ 已实现 |
| Token 刷新 | `AuthController` → `POST /api/auth/refresh` | — | ✅ 已实现 |
| 登出/注销 | `AuthController` → `POST /api/auth/logout` | — | ✅ 已实现 |
| 修改密码 | `AuthController` → `POST /api/auth/change-password` | `ChangePassword.vue` | ✅ 已实现 |
| 获取当前用户信息 | `AuthController` → `GET /api/me` | — | ✅ 已实现 |
| 获取权限码列表 | `AuthController` → `GET /api/me/permissions` | — | ✅ 已实现 |
| 角色管理 CRUD | `RoleController` → `/api/roles/**` | `Role.vue` | ✅ 已实现 |
| 审批待办列表 | `ApprovalController` → `GET /api/approval/todo` | `ApprovalList.vue` | ✅ 已实现 |
| 审批通过 | `ApprovalController` → `POST /api/approval/tasks/{id}/approve` | — | ✅ 已实现 |
| 审批驳回 | `ApprovalController` → `POST /api/approval/tasks/{id}/reject` | — | ✅ 已实现 |
| 审批撤回 | `ApprovalController` → `POST /api/approval/tasks/{id}/revoke` | — | ✅ 已实现 |
| 审批历史查询 | `ApprovalController` → `GET /api/approval/instances/{id}/history` | — | ✅ 已实现 |
| 仪表盘首页 | — | `Dashboard.vue` | ✅ 已实现 |
| 403 无权限页 | — | `Forbidden.vue` | ✅ 已实现 |

**横切能力（无独立 Controller，内置于 hrms-common）**：
- ✅ JWT 双 Token（access 30min + refresh 7d），BCrypt 密码哈希
- ✅ RBAC：`@HasPermission` AOP + Caffeine 5min 缓存
- ✅ 数据权限：`@DataScope` 注解，4 档（ALL/OWN_DEPT/SUBORDINATE_TREE/SELF_ONLY）
- ✅ 敏感字段 AES-256-GCM 加密（身份证、手机、银行卡）
- ✅ 审计日志（动作级 + 薪资字段级前后值）
- ✅ 字典管理（sys_dict + sys_dict_item，Caffeine 缓存）
- ✅ 附件管理（本地文件存储，SHA-256 去重）
- ✅ 轻量审批引擎（4 表：definition/instance/task/history）
- ✅ 雪花 ID（hutool）跨方言一致
- ✅ Liquibase YAML 跨方言 DDL

**数据库表**：14 张横切表
- sys_user / sys_role / sys_permission / sys_user_role / sys_role_permission
- sys_dict / sys_dict_item
- sys_attachment / sys_audit_log / sys_user_token
- approval_definition / approval_instance / approval_task / approval_history

---

### 模块 2：组织管理（Organization Management / OM）

| 功能点 | 后端 Controller | API 端点 | 前端页面 | 状态 |
|---|---|---|---|---|
| 公司信息查询 | `CompanyController` | `GET /api/company` | — | ✅ 已实现 |
| 公司信息更新 | `CompanyController` | `PUT /api/company` | — | ✅ 已实现 |
| 部门树形查询 | `DepartmentController` | `GET /api/departments/tree` | `OrgTree.vue` | ✅ 已实现 |
| 创建部门 | `DepartmentController` | `POST /api/departments` | `DepartmentManage.vue` | ✅ 已实现 |
| 更新部门 | `DepartmentController` | `PUT /api/departments/{id}` | `DepartmentManage.vue` | ✅ 已实现 |
| 移动部门 | `DepartmentController` | `PUT /api/departments/{id}/move` | — | ✅ 已实现 |
| 删除部门 | `DepartmentController` | `DELETE /api/departments/{id}` | — | ✅ 已实现 |
| 岗位列表查询 | `PositionController` | `GET /api/positions` | `PositionManage.vue` | ✅ 已实现 |
| 岗位详情 | `PositionController` | `GET /api/positions/{id}` | — | ✅ 已实现 |
| 创建岗位 | `PositionController` | `POST /api/positions` | `PositionManage.vue` | ✅ 已实现 |
| 更新岗位 | `PositionController` | `PUT /api/positions/{id}` | — | ✅ 已实现 |
| 删除岗位 | `PositionController` | `DELETE /api/positions/{id}` | — | ✅ 已实现 |
| 职位/职级列表 | `JobController` | `GET /api/jobs` | — | ✅ 已实现 |
| 职位详情 | `JobController` | `GET /api/jobs/{id}` | — | ✅ 已实现 |
| 创建职位 | `JobController` | `POST /api/jobs` | — | ✅ 已实现 |
| 更新职位 | `JobController` | `PUT /api/jobs/{id}` | — | ✅ 已实现 |
| 删除职位 | `JobController` | `DELETE /api/jobs/{id}` | — | ✅ 已实现 |

**对应后端模块**：`hrms-org`
**数据库表**：4 张（company / department / position / job）
**前端覆盖**：✅ 完整覆盖

---

### 模块 3：员工管理（Employee Administration / PA）

| 功能点 | 后端 Controller | API 端点 | 前端页面 | 状态 |
|---|---|---|---|---|
| 员工列表查询 | `EmployeeController` | `GET /api/hr/employees` | `EmployeeList.vue` | ✅ 已实现 |
| 员工详情查询 | `EmployeeController` | `GET /api/hr/employees/{id}` | — | ✅ 已实现 |
| 创建员工 | `EmployeeController` | `POST /api/hr/employees` | — | ✅ 已实现 |
| 更新员工 | `EmployeeController` | `PUT /api/hr/employees/{id}` | — | ✅ 已实现 |
| 员工离职操作 | `EmployeeController` | `PUT /api/hr/employees/{id}/termination` | — | ✅ 已实现 |
| 入职单创建 | `HireFormController` | `POST /api/hr/hire-forms` | — | ✅ 已实现 |
| 入职单提交审批 | `HireFormController` | `POST /api/hr/hire-forms/{id}/submit` | — | ✅ 已实现 |
| 入职单列表 | `HireFormController` | `GET /api/hr/hire-forms` | — | ✅ 已实现 |
| 入职单详情 | `HireFormController` | `GET /api/hr/hire-forms/{id}` | — | ✅ 已实现 |
| 调岗单创建 | `TransferFormController` | `POST /api/hr/transfer-forms` | — | ✅ 已实现 |
| 调岗单提交审批 | `TransferFormController` | `POST /api/hr/transfer-forms/{id}/submit` | — | ✅ 已实现 |
| 调岗单列表 | `TransferFormController` | `GET /api/hr/transfer-forms` | — | ✅ 已实现 |
| 调岗单详情 | `TransferFormController` | `GET /api/hr/transfer-forms/{id}` | — | ✅ 已实现 |
| 离职单创建 | `TerminationFormController` | `POST /api/hr/termination-forms` | — | ✅ 已实现 |
| 离职单提交审批 | `TerminationFormController` | `POST /api/hr/termination-forms/{id}/submit` | — | ✅ 已实现 |
| 离职单列表 | `TerminationFormController` | `GET /api/hr/termination-forms` | — | ✅ 已实现 |
| 离职单详情 | `TerminationFormController` | `GET /api/hr/termination-forms/{id}` | — | ✅ 已实现 |

**员工档案包含的子表**（6 张 + 3 张变更单）：
- employee_contract（劳动合同）
- employee_education（教育经历）
- employee_work_experience（工作经历）
- employee_emergency_contact（紧急联系人）
- employee_bank_account（银行卡）
- employee_family（家庭成员）

**状态机**：PENDING_HIRE → PROBATION → ACTIVE ↔ ON_LEAVE → TERMINATED

**对应后端模块**：`hrms-employee`
**数据库表**：10 张（employee + 6 子表 + 3 变更单）
**前端覆盖**：⚠️ 部分（EmployeeList.vue 已实现，但入职/调岗/离职的独立前端页面缺失，仅有后端 API）

---

### 模块 4：考勤与假期（Attendance & Leave / PT）

| 功能点 | 后端 Controller | API 端点 | 前端页面 | 状态 |
|---|---|---|---|---|
| 班次管理 | `ShiftController` | `POST/GET/PUT/DELETE /api/attendance/shifts` | `ShiftSchedule.vue` | ✅ 已实现 |
| 排班管理 | `ScheduleController` | `POST/GET /api/attendance/schedules` | `ShiftSchedule.vue` | ✅ 已实现 |
| 打卡记录录入 | `TimePunchController` | `POST /api/attendance/punches` | `TimePunch.vue` | ✅ 已实现 |
| 打卡 CSV 批量导入 | `TimePunchController` | `POST /api/attendance/punches/import` | — | ✅ 已实现 |
| 打卡记录查询 | `TimePunchController` | `GET /api/attendance/punches` | — | ✅ 已实现 |
| 考勤日汇总生成 | `AttendanceSummaryController` | `POST /api/attendance/summaries/generate` | — | ✅ 已实现 |
| 考勤汇总查询 | `AttendanceSummaryController` | `GET /api/attendance/summaries` | — | ✅ 已实现 |
| 假类管理 | `LeaveTypeController` | `GET/POST /api/attendance/leave-types` | — | ✅ 已实现 |
| 假期余额查询 | `LeaveBalanceController` | `GET /api/attendance/leave-balances` | — | ✅ 已实现 |
| 请假申请创建 | `LeaveRequestController` | `POST /api/attendance/leave-requests` | `LeaveCreate.vue` | ✅ 已实现 |
| 请假申请提交审批 | `LeaveRequestController` | `POST /api/attendance/leave-requests/{id}/submit` | — | ✅ 已实现 |
| 请假申请列表 | `LeaveRequestController` | `GET /api/attendance/leave-requests` | `LeaveRequest.vue` | ✅ 已实现 |
| 请假申请详情 | `LeaveRequestController` | `GET /api/attendance/leave-requests/{id}` | — | ✅ 已实现 |
| 加班申请创建 | `OvertimeRequestController` | `POST /api/attendance/overtime-requests` | `OvertimeRequest.vue` | ✅ 已实现 |
| 加班申请提交审批 | `OvertimeRequestController` | `POST /api/attendance/overtime-requests/{id}/submit` | — | ✅ 已实现 |
| 加班申请列表 | `OvertimeRequestController` | `GET /api/attendance/overtime-requests` | — | ✅ 已实现 |
| 假期年度结算 | `SettlementController` | `POST /api/attendance/settlements` | `LeaveSettlement.vue` | ✅ 已实现 |
| 结算批次列表 | `SettlementController` | `GET /api/attendance/settlements` | — | ✅ 已实现 |
| 结算批次详情 | `SettlementController` | `GET /api/attendance/settlements/{id}` | — | ✅ 已实现 |

**对应后端模块**：`hrms-attendance`
**数据库表**：8 张（work_calendar / shift / schedule / attendance_record / leave_type / leave_balance / leave_application / overtime_application + settlement_batch）
**前端覆盖**：✅ 完整覆盖（ShiftSchedule / TimePunch / LeaveCreate / LeaveRequest / OvertimeRequest / LeaveSettlement）

---

### 模块 5：薪酬基础（Payroll Basic / PY）

| 功能点 | 后端 Controller | API 端点 | 前端页面 | 状态 |
|---|---|---|---|---|
| 薪资档案创建/更新 | `CompensationController` | `POST /api/payroll/compensations` | `Compensation.vue` | ✅ 已实现 |
| 薪资档案列表 | `CompensationController` | `GET /api/payroll/compensations` | — | ✅ 已实现 |
| 薪资档案详情 | `CompensationController` | `GET /api/payroll/compensations/{employeeId}` | — | ✅ 已实现 |
| 工资周期创建 | `PayrollPeriodController` | `POST /api/payroll/periods` | — | ✅ 已实现 |
| 工资周期列表 | `PayrollPeriodController` | `GET /api/payroll/periods` | — | ✅ 已实现 |
| 工资 Run 创建 | `PayrollRunController` | `POST /api/payroll/runs` | `PayrollRuns.vue` | ✅ 已实现 |
| 工资 Run 计算执行 | `PayrollRunController` | `POST /api/payroll/runs/{id}/calculate` | — | ✅ 已实现 |
| 工资 Run 锁定 | `PayrollRunController` | `POST /api/payroll/runs/{id}/lock` | — | ✅ 已实现 |
| 工资 Run 红冲 | `PayrollRunController` | `POST /api/payroll/runs/{id}/reverse` | — | ✅ 已实现 |
| 工资 Run 列表 | `PayrollRunController` | `GET /api/payroll/runs` | — | ✅ 已实现 |
| 工资 Run 详情 | `PayrollRunController` | `GET /api/payroll/runs/{id}` | — | ✅ 已实现 |
| 工资条列表查询 | `PayslipController` | `GET /api/payroll/payslips` | `Payslip.vue` | ✅ 已实现 |
| 工资条详情查询 | `PayslipController` | `GET /api/payroll/payslips/{runId}` | — | ✅ 已实现 |

**工资计算公式**：
- 应发 = 基本工资 + 岗位工资 + 绩效 + 补贴
- 社保扣款 = 应发 × 个人社保费率
- 公积金扣款 = 应发 × 个人公积金费率
- 累计应纳税所得额 = 累计应发 - 累计社保 - 累计公积金 - 累计起征点
- 个税 = 按累计预扣预缴档位查表 - 已扣个税
- 实发 = 应发 - 社保 - 公积金 - 个税

**Run 状态机**：DRAFT → CALCULATING → CALCULATED → APPROVED → PAID

**对应后端模块**：`hrms-payroll`
**数据库表**：7 张（salary_profile / salary_item / salary_run / payslip / payslip_detail / tax_rule / social_insurance_rule）
**前端覆盖**：✅ 完整覆盖（Compensation / PayrollRuns / Payslip）

---

### 模块 6：招聘与入职（Recruitment & Onboarding）

| 功能点 | 后端 Controller | API 端点 | 前端页面 | 状态 |
|---|---|---|---|---|
| 职位需求创建 | `RequisitionController` | `POST /api/recruit/requisitions` | `RecruitRequisition.vue` | ✅ 已实现 |
| 职位需求详情 | `RequisitionController` | `GET /api/recruit/requisitions/{id}` | — | ✅ 已实现 |
| 职位需求列表 | `RequisitionController` | `GET /api/recruit/requisitions` | — | ✅ 已实现 |
| 候选人创建 | `CandidateController` | `POST /api/recruit/candidates` | `RecruitCandidate.vue` | ✅ 已实现 |
| 候选人详情 | `CandidateController` | `GET /api/recruit/candidates/{id}` | — | ✅ 已实现 |
| 候选人列表 | `CandidateController` | `GET /api/recruit/candidates` | — | ✅ 已实现 |
| 面试安排创建 | `InterviewController` | `POST /api/recruit/interviews` | — | ✅ 已实现 |
| 面试列表查询 | `InterviewController` | `GET /api/recruit/interviews` | — | ✅ 已实现 |
| 面试结果录入 | `InterviewController` | `POST /api/recruit/interviews/{id}/result` | — | ✅ 已实现 |
| 面试评价创建 | `EvaluationController` | `POST /api/recruit/evaluations` | — | ✅ 已实现 |
| 面试评价列表 | `EvaluationController` | `GET /api/recruit/evaluations` | — | ✅ 已实现 |
| Offer 创建 | `OfferController` | `POST /api/recruit/offers` | `RecruitOffer.vue` | ✅ 已实现 |
| Offer 详情 | `OfferController` | `GET /api/recruit/offers/{id}` | — | ✅ 已实现 |
| Offer 提交审批 | `OfferController` | `POST /api/recruit/offers/{id}/submit` | — | ✅ 已实现 |
| Offer 接受（自动建档） | `OfferController` | `POST /api/recruit/offers/{id}/accept` | — | ✅ 已实现 |
| Offer 拒绝 | `OfferController` | `POST /api/recruit/offers/{id}/decline` | — | ✅ 已实现 |

**候选人状态机**：NEW → SCREENING → INTERVIEWING → OFFERED → ACCEPTED / REJECTED / WITHDRAWN
**Offer 状态机**：DRAFT → APPROVED → SENT → ACCEPTED → CONVERTED / REJECTED / CANCELLED

**对应后端模块**：`hrms-recruit`
**数据库表**：5 张（job_posting / candidate / candidate_resume / interview / offer）
**前端覆盖**：✅ 完整覆盖（RecruitRequisition / RecruitCandidate / RecruitOffer）

---

### 模块 7：绩效管理（Performance Management / PM）

| 功能点 | 后端 Controller | API 端点 | 前端页面 | 状态 |
|---|---|---|---|---|
| 绩效周期创建 | `CycleController` | `POST /api/performance/cycles` | — | ✅ 已实现 |
| 周期激活 | `CycleController` | `POST /api/performance/cycles/{id}/activate` | — | ✅ 已实现 |
| 周期关闭 | `CycleController` | `POST /api/performance/cycles/{id}/close` | — | ✅ 已实现 |
| 周期列表 | `CycleController` | `GET /api/performance/cycles` | — | ✅ 已实现 |
| 绩效模板创建 | `TemplateController` | `POST /api/performance/templates` | — | ✅ 已实现 |
| 模板列表 | `TemplateController` | `GET /api/performance/templates` | — | ✅ 已实现 |
| 模板详情 | `TemplateController` | `GET /api/performance/templates/{id}` | — | ✅ 已实现 |
| 绩效单列表 | `AppraisalController` | `GET /api/performance/appraisals` | `PerformanceReview.vue` | ✅ 已实现 |
| 绩效单详情 | `AppraisalController` | `GET /api/performance/appraisals/{id}` | — | ✅ 已实现 |
| 录入目标 | `AppraisalController` | `POST /api/performance/appraisals/{id}/goals` | — | ✅ 已实现 |
| 确认目标 | `AppraisalController` | `POST /api/performance/appraisals/{id}/goals/confirm` | — | ✅ 已实现 |
| 自评提交 | `AppraisalController` | `POST /api/performance/appraisals/{id}/self-review` | — | ✅ 已实现 |
| 上级评分提交 | `AppraisalController` | `POST /api/performance/appraisals/{id}/manager-review` | — | ✅ 已实现 |
| 终评/最终得分 | `AppraisalController` | `POST /api/performance/appraisals/{id}/finalize` | — | ✅ 已实现 |

**最终得分公式**：`最终得分 = 自评 × 0.3 + 上级评分 × 0.7`
**状态机**：DRAFT → SELF_REVIEWING → MANAGER_REVIEWING → COMPLETED / TERMINATED / INCOMPLETE

**对应后端模块**：`hrms-performance`
**数据库表**：4 张（performance_period / performance_template / performance_goal / performance_review）
**前端覆盖**：⚠️ 部分（仅 PerformanceReview.vue，缺周期管理、模板管理页面）

---

### 模块 8：ESS/MSS 自助服务（Employee & Manager Self Service）

| 功能点 | 后端 Controller | API 端点 | 前端页面 | 状态 |
|---|---|---|---|---|
| ESS 个人信息 | `EssController` | `GET /api/portal/ess/me` | `Ess.vue` | ✅ 已实现 |
| ESS 假期余额 | `EssController` | `GET /api/portal/ess/leave-balances` | — | ✅ 已实现 |
| ESS 请假记录 | `EssController` | `GET /api/portal/ess/leave-requests` | — | ✅ 已实现 |
| ESS 待办列表 | `EssController` | `GET /api/portal/ess/todo` | — | ✅ 已实现 |
| ESS 工资条列表 | `EssController` | `GET /api/portal/ess/payslips` | — | ✅ 已实现 |
| ESS 工资条详情 | `EssController` | `GET /api/portal/ess/payslips/{runId}` | — | ✅ 已实现 |
| MSS 团队列表 | `MssController` | `GET /api/portal/mss/team` | `Mss.vue` | ✅ 已实现 |
| MSS 团队请假 | `MssController` | `GET /api/portal/mss/team/leave-requests` | — | ✅ 已实现 |
| MSS 待办列表 | `MssController` | `GET /api/portal/mss/todo` | — | ✅ 已实现 |
| MSS 审批通过 | `MssController` | `POST /api/portal/mss/tasks/{taskId}/approve` | — | ✅ 已实现 |
| MSS 审批驳回 | `MssController` | `POST /api/portal/mss/tasks/{taskId}/reject` | — | ✅ 已实现 |

**对应后端模块**：`hrms-portal-api`（聚合 BFF，依赖各业务模块 Service）
**独立数据表**：0 张（聚合其他模块数据）
**前端覆盖**：✅ 完整覆盖（Ess.vue + Mss.vue）

---

## 三、数据库表总览（~49 张）

| 分组 | 表名 | 数量 |
|---|---|---|
| **组织管理** | company, department, position, job | 4 |
| **员工管理** | employee, employee_contract, employee_education, employee_work_experience, employee_emergency_contact, employee_bank_account, employee_family | 7 |
| **入离调转** | hire_form, transfer_form, termination_form | 3 |
| **考勤假期** | work_calendar, shift, schedule, attendance_record, leave_type, leave_balance, leave_application, overtime_application, settlement_batch | 9 |
| **薪酬** | salary_profile, salary_item, salary_run, payslip, payslip_detail, tax_rule, social_insurance_rule | 7 |
| **招聘** | job_posting, candidate, candidate_resume, interview, offer | 5 |
| **绩效** | performance_period, performance_template, performance_goal, performance_review | 4 |
| **RBAC** | sys_user, sys_role, sys_permission, sys_user_role, sys_role_permission | 5 |
| **横切** | sys_dict, sys_dict_item, sys_attachment, sys_audit_log, sys_user_token | 5 |
| **审批** | approval_definition, approval_instance, approval_task, approval_history | 4 |
| **合计** | | **~48-49** |

---

## 四、后端 Controller 完整清单（34 个）

| 序号 | Controller | 模块 | API 前缀 | 端点数 |
|---|---|---|---|---|
| 1 | `AuthController` | 认证 | `/api` | 6 |
| 2 | `RoleController` | RBAC | `/api/roles` | 5 |
| 3 | `ApprovalController` | 审批 | `/api/approval` | 5 |
| 4 | `CompanyController` | 组织 | `/api/company` | 2 |
| 5 | `DepartmentController` | 组织 | `/api/departments` | 5 |
| 6 | `PositionController` | 组织 | `/api/positions` | 5 |
| 7 | `JobController` | 组织 | `/api/jobs` | 5 |
| 8 | `EmployeeController` | 员工 | `/api/hr/employees` | 5 |
| 9 | `HireFormController` | 员工 | `/api/hr/hire-forms` | 4 |
| 10 | `TransferFormController` | 员工 | `/api/hr/transfer-forms` | 4 |
| 11 | `TerminationFormController` | 员工 | `/api/hr/termination-forms` | 4 |
| 12 | `ShiftController` | 考勤 | `/api/attendance/shifts` | 4 |
| 13 | `ScheduleController` | 考勤 | `/api/attendance/schedules` | 2 |
| 14 | `TimePunchController` | 考勤 | `/api/attendance/punches` | 3 |
| 15 | `AttendanceSummaryController` | 考勤 | `/api/attendance/summaries` | 2 |
| 16 | `LeaveTypeController` | 假期 | `/api/attendance/leave-types` | 2 |
| 17 | `LeaveBalanceController` | 假期 | `/api/attendance/leave-balances` | 1 |
| 18 | `LeaveRequestController` | 假期 | `/api/attendance/leave-requests` | 4 |
| 19 | `OvertimeRequestController` | 假期 | `/api/attendance/overtime-requests` | 3 |
| 20 | `SettlementController` | 假期 | `/api/attendance/settlements` | 3 |
| 21 | `CompensationController` | 薪酬 | `/api/payroll/compensations` | 3 |
| 22 | `PayrollPeriodController` | 薪酬 | `/api/payroll/periods` | 2 |
| 23 | `PayrollRunController` | 薪酬 | `/api/payroll/runs` | 6 |
| 24 | `PayslipController` | 薪酬 | `/api/payroll/payslips` | 2 |
| 25 | `RequisitionController` | 招聘 | `/api/recruit/requisitions` | 3 |
| 26 | `CandidateController` | 招聘 | `/api/recruit/candidates` | 3 |
| 27 | `InterviewController` | 招聘 | `/api/recruit/interviews` | 3 |
| 28 | `EvaluationController` | 招聘 | `/api/recruit/evaluations` | 2 |
| 29 | `OfferController` | 招聘 | `/api/recruit/offers` | 5 |
| 30 | `CycleController` | 绩效 | `/api/performance/cycles` | 4 |
| 31 | `TemplateController` | 绩效 | `/api/performance/templates` | 3 |
| 32 | `AppraisalController` | 绩效 | `/api/performance/appraisals` | 7 |
| 33 | `EssController` | 门户 | `/api/portal/ess` | 6 |
| 34 | `MssController` | 门户 | `/api/portal/mss` | 5 |

**API 端点总数**：约 **120+** 个

---

## 五、前端页面完整清单（25 个 .vue 文件）

| 序号 | 文件路径 | 功能 | 所属模块 |
|---|---|---|---|
| 1 | `Login.vue` | 登录页 | 认证 |
| 2 | `Dashboard.vue` | 仪表盘首页 | 系统 |
| 3 | `Forbidden.vue` | 403 无权限页 | 系统 |
| 4 | `auth/ChangePassword.vue` | 修改密码 | 认证 |
| 5 | `system/Role.vue` | 角色管理 | RBAC |
| 6 | `approval/ApprovalList.vue` | 审批待办列表 | 审批 |
| 7 | `org/OrgTree.vue` | 组织树视图 | 组织 |
| 8 | `org/DepartmentManage.vue` | 部门管理 | 组织 |
| 9 | `org/PositionManage.vue` | 岗位管理 | 组织 |
| 10 | `hr/EmployeeList.vue` | 员工列表 | 员工 |
| 11 | `attendance/ShiftSchedule.vue` | 班次与排班 | 考勤 |
| 12 | `attendance/TimePunch.vue` | 打卡管理 | 考勤 |
| 13 | `attendance/LeaveCreate.vue` | 请假申请创建 | 假期 |
| 14 | `attendance/LeaveRequest.vue` | 请假申请列表 | 假期 |
| 15 | `attendance/OvertimeRequest.vue` | 加班申请 | 假期 |
| 16 | `attendance/LeaveSettlement.vue` | 假期年度结算 | 假期 |
| 17 | `payroll/Compensation.vue` | 薪资档案 | 薪酬 |
| 18 | `payroll/PayrollRuns.vue` | 工资计算 Run | 薪酬 |
| 19 | `payroll/Payslip.vue` | 工资条查询 | 薪酬 |
| 20 | `recruit/RecruitRequisition.vue` | 职位需求 | 招聘 |
| 21 | `recruit/RecruitCandidate.vue` | 候选人管理 | 招聘 |
| 22 | `recruit/RecruitOffer.vue` | Offer 管理 | 招聘 |
| 23 | `performance/PerformanceReview.vue` | 绩效评审 | 绩效 |
| 24 | `portal/Ess.vue` | 员工自助 ESS | ESS |
| 25 | `portal/Mss.vue` | 经理自助 MSS | MSS |

---

## 六、功能实现状态汇总

| 模块 | 后端 API | 前端页面 | 数据库表 | 整体完成度 |
|---|---|---|---|---|
| **认证与系统** | ✅ 完整 | ✅ 完整 | 14 张 | 🟢 100% |
| **组织管理** | ✅ 完整 | ✅ 完整 | 4 张 | 🟢 100% |
| **员工管理** | ✅ 完整 | ⚠️ 部分 | 10 张 | 🟡 80% |
| **考勤假期** | ✅ 完整 | ✅ 完整 | 9 张 | 🟢 95% |
| **薪酬基础** | ✅ 完整 | ✅ 完整 | 7 张 | 🟢 95% |
| **招聘入职** | ✅ 完整 | ✅ 完整 | 5 张 | 🟢 90% |
| **绩效管理** | ✅ 完整 | ⚠️ 部分 | 4 张 | 🟡 75% |
| **ESS/MSS** | ✅ 完整 | ✅ 完整 | 0（聚合） | 🟢 90% |

---

## 七、MVP 硬边界（明确不做）

### 业务范围
- ❌ 多公司/多法人/集团合并
- ❌ 多语言/国际派遣/海外薪酬
- ❌ 多币种/多时区
- ❌ 继任者计划 / 学习管理 / 复杂福利 / 差旅管理
- ❌ 360 度环评 / 申诉流程
- ❌ 调薪历史与版本化

### 薪酬简化
- ❌ 真实税务全国规则（仅北京 2024 一档示例）
- ❌ 年度个税汇算清缴 / 专项附加扣除
- ❌ 入离当月按日折算 / 跨月补发补扣
- ❌ 缺勤联动扣薪 / 加班联动工资
- ❌ 多套薪资方案 / 银企直连

### 流程简化
- ❌ 会签 / 分支判断 / 子流程嵌套 / 代理人审批
- ❌ 超过 3 级的串行审批

### 集成
- ❌ 钉钉/企微/飞书 SSO
- ❌ 招聘渠道集成（智联/BOSS/LinkedIn）
- ❌ 移动端原生 App（仅响应式 Web）

---

## 八、与 SAP HCM 对比准备维度

| SAP HCM 模块 | 对应 HRMS 模块 | HRMS 实现状态 | 差距分析 |
|---|---|---|---|
| **OM** (Organization Management) | 组织管理 | ✅ 已实现 | 缺矩阵组织/跨部门外派/多法人 |
| **PA** (Personnel Administration) | 员工管理 | ✅ 已实现 | 缺按日折算/调薪版本化/外派 |
| **PT** (Time Management) | 考勤假期 | ✅ 已实现 | 缺硬件对接/复杂工时制/排班优化 |
| **PY** (Payroll) | 薪酬基础 | ⚠️ 简化版 | 仅一档示例，缺真实税务/社保/多方案 |
| **Recruitment** | 招聘入职 | ✅ 已实现 | 缺渠道集成/候选人门户/AI筛选 |
| **PM** (Performance Management) | 绩效管理 | ⚠️ 骨架 | 缺360/校准/强制分布/申诉 |
| **ESS/MSS** | ESS/MSS | ✅ 已实现 | 缺移动端/消息推送/委托审批 |
| **基础架构** | 认证/权限/审批/审计 | ✅ 完整 | 5 种数据库方言支持，JWT+RBAC+数据权限 |

---

## 九、已知遗留问题

| 类型 | 描述 | 优先级 |
|---|---|---|
| 测试环境 | JDK 25 上 Mockito mock 具体类需升级 byte-buddy 至 1.15+ | P2 |
| 前端覆盖 | 员工管理（入职/调岗/离职表单页面）、绩效（周期/模板管理页面）前端待补 | P2 |
| 多数据库 | 全矩阵 CI 仅在 PG 跑通；MySQL/Oracle 冒烟待补 | P2 |
| 硬件对接 | 打卡机仅做 CSV 导入，硬件 SDK 未接入 | P3 |

---

*本文档基于 HRMS MVP v0.1.0 代码库扫描生成，可作为与 SAP HCM 功能对比的基准参照。*
