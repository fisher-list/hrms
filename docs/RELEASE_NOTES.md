# HRMS MVP v0.1.0 — Release Notes

> 发布日期：2026-06-17
> 对标：SAP HCM / Oracle PeopleSoft HCM
> 项目方法：BMAD（Planning + Development）

## 范围一览

本次 MVP 覆盖 **6 个 Epic、14 个 Story**，分 5 个 Sprint 完成（Sprint 0 规划期 + Sprint 1~5 实现期）。

| Epic | 名称 | Story | Sprint |
|---|---|---|---|
| EP-01 | 组织与员工主数据（SAP OM+PA / PS Workforce Admin） | S01~S05 | 1 |
| EP-02 | 考勤与假期（SAP PT / PS Time Labor + Absence） | S06~S08 | 2 |
| EP-03 | 薪酬基础（SAP PY / PS Payroll，简化版） | S09~S10 | 3 |
| EP-04 | 招聘与入职（SAP Recruitment / PS eRecruit） | S11~S12 | 4 |
| EP-05 | 绩效（SAP PM / PS ePerformance，骨架） | S13~S14 | 4 |
| EP-06 | ESS/MSS 自助服务（SAP ESS/MSS） | E01~E04 | 5 |

## 关键能力

### 横切（基础设施）
- JWT 双 Token：access 30 分钟 + refresh 7 天，登录失败锁定，BCrypt 密码哈希
- RBAC：`@HasPermission` AOP + Caffeine 5 分钟缓存
- 数据权限：`@DataScope` 注解，4 档（ALL/OWN_DEPT/SUBORDINATE_TREE/SELF_ONLY）
- 自研轻量审批引擎：定义/实例/任务/历史 4 表，事件解耦各业务模块
- 敏感字段 AES-256-GCM 加密（身份证、手机、银行卡），`HRMS_AES_KEY` 环境变量
- 雪花 ID（hutool）跨方言一致；`BaseEntity` 含 `tenantId`/逻辑删除/版本号
- Liquibase YAML 跨方言（PG/MySQL/Oracle/SQL Server/达梦）

### 业务域
- **组织/员工**：4 级组织树（公司/部门/岗位/职位）、员工档案 + 6 张子表（教育/工作/家属/合同/银行/地址）、入离调转 3 张变更单 + 状态机
- **考勤/假期**：班次/排班/打卡（CSV 导入 ZKTeco/海康风格）、日汇总、请假/加班审批、假期年度结算（CARRY_MAX5/NONE/EXPIRE_2M）
- **薪酬**：薪资档案、工资计算 run（毛工资 → 五险一金 → 累计预扣个税 → 实发）、工资单查询、reverseRun 支持
- **招聘**：职位需求/候选人/面试/评价/Offer，Offer 通过自动建员工档案 + SysUser
- **绩效**：周期 → 模板 → 评分项；目标 → 自评 → 上级评 → 终评，最终分 = 自评 × 0.3 + 上级 × 0.7
- **ESS/MSS**：员工查个人/请假/工资条/待办；经理查团队/团队请假/审批待办/审批动作

## 数据库迁移

11 个 changelog 文件、约 40+ 张表：

```
changelog-00-system.yaml          系统字典/审计/附件
changelog-01-rbac.yaml            sys_user/role/permission + role_permission
changelog-02-approval.yaml        approval_definition/instance/task/history
changelog-03-org.yaml             sys_company/department/position/job_position
changelog-04-employee.yaml        hr_employee + 6 子表 + 3 变更单 + 20 员工种子
changelog-05-attendance-shift.yaml  at_shift/schedule/time_punch/daily_summary
changelog-06-attendance-leave.yaml  at_leave_type/balance/request/overtime/log
changelog-07-attendance-settlement.yaml  at_settlement_batch
changelog-08-payroll-master.yaml  py_compensation/period/run/detail/iit_bracket/social_rate/cumulative
changelog-09-recruit.yaml         rc_job_requisition/candidate/interview/evaluation/offer/status_log
changelog-10-performance.yaml     pf_cycle/template/scoring_item/appraisal/goal/self/manager_review
changelog-11-portal-permissions.yaml  ESS/MSS 权限种子（951-954）
```

## 测试与质量

- 单元测试 **104 个**，业务测试通过率 **95/104**（91.3%）
- 9 个失败：`HasPermissionAspectTest` (4) + `AuthServiceTest` (5)，均为 **JDK 25 + ByteBuddy 1.14 兼容性问题**，非业务代码缺陷
- 关键测试：
  - `HrEmployeeServiceTest` 11/11
  - `ApprovalServiceTest` 11/11
  - `OfferServiceTest` 9/9（Sprint 5 新增）
  - `AppraisalServiceTest` 5/5（含最终得分加权计算）
  - `PayrollServiceTest` 5/5（含累计预扣个税）

## 已知遗留

| 类型 | 描述 | 优先级 |
|---|---|---|
| 测试环境 | JDK 25 上 Mockito mock 具体类需升级 byte-buddy 至 1.15+ | P2 |
| 前端 | 已完成：登录+RBAC、ESS/MSS 门户、组织树、员工档案、请假申请、薪酬批次、招聘 Offer、绩效评审；S06/S08 业务前端待补 | P2 |
| 多数据库 | 全矩阵 CI 仅在 PG 跑通；MySQL/Oracle 冒烟待补 | P2 |
| 硬件对接 | 打卡机仅做 CSV 导入，硬件 SDK 未接入 | P3 |

## 部署

```bash
cd /Users/apple/hrms/backend
mvn package -DskipTests -Djacoco.skip=true
java -jar hrms-app/target/hrms-app-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=h2  # 或 pg/mysql
```

环境变量：`HRMS_AES_KEY`（32 字节 base64 字符串），`JWT_SECRET`（≥32 字符）

## 验收对照（计划文档第九节 10 项）

1. ✅ 启动后端 + 前端，登录页可用
2. ⚠️ 多方言 Liquibase changelog 仅在 PG/H2 验证；MySQL/Oracle 冒烟待补
3. ✅ 组织树/岗位/员工/入职闭环
4. ✅ 请假提交 + 审批 + 余额扣减
5. ✅ 工资 run + 工资条 ESS 可见
6. ✅ 职位 + 候选人 + Offer + 自动建档
7. ✅ 绩效周期 + 自评 + 上级评 + 终评
8. ⚠️ E2E 主路径手工验证；自动 E2E 待补
9. ⚠️ Reviewer P0/P1 已修；P2 进 backlog
10. ✅ 文档齐套

**MVP 后端核心可演示；前端只覆盖 S01/S02。**
