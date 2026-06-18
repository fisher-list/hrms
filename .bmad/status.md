# HRMS 项目台账（PM 维护）

> BMAD 项目管理 · 总负责人视角 · 最后更新：2026-06-16

## 项目元数据
- **项目名**：HRMS（对标 SAP HCM / Oracle PeopleSoft HCM）
- **目标**：MVP 演示原型，跑通六大核心 Epic 端到端业务流
- **技术栈**：Spring Boot 3.x + MyBatis-Plus + Vue 3 + Liquibase
- **多数据库目标**：PostgreSQL（首选）/ MySQL 8 / Oracle / SQL Server / DM 达梦
- **方法论**：BMAD（Planning Phase + Development Phase）
- **计划文档**：`/Users/apple/.claude/plans/graceful-splashing-kitten.md`

## 当前阶段
**Sprint 5 · 收尾完成**（MVP v0.1.0 后端就绪，前端待补 S03-S14 + ESS/MSS）

## 团队与代理映射

| 角色 | 通讯名 | 代理类型 | 主要产物 | 状态 |
|---|---|---|---|---|
| 项目总负责人 PM | `pm` | 主上下文 | `.bmad/status.md`、Epic/Story 编排 | active |
| 产品规划 | `product` | analyst (read-only) → executor (sonnet) | `docs/prd.md` | ✅ 完成 v0.1 (2026-06-14) |
| 技术架构师 | `architect` | system-architect (sonnet) | `docs/architecture.md`、`docs/data-model.md`、ADR-001~004 | ✅ 完成 v0.1 (2026-06-14) |
| 测试总监 | `qa-lead` | test-engineer (sonnet) | `docs/test-strategy.md` | ✅ 完成 v0.1 (2026-06-14) |
| 代码审核员 | `reviewer` | code-reviewer (sonnet) | `docs/review-checklist.md` | ✅ 完成 v0.1 (2026-06-14) |
| 后端研发 | `dev-be` | backend-dev | Spring Boot 实现 | Sprint 1 启用 |
| 前端研发 | `dev-fe` | coder (sonnet) | Vue 3 实现 | Sprint 1 启用 |
| 数据库研发 | `dev-db` | coder (sonnet) | Liquibase changelog | Sprint 1 启用 |

## Sprint 计划

| Sprint | 目标 | 主要产物 |
|---|---|---|
| **0** | 规划期（PRD/架构/测试/审核 4 份基线文档） | `docs/prd.md`、`docs/architecture.md`、`docs/data-model.md`、`docs/test-strategy.md`、`docs/review-checklist.md`、Epic 01 拆 Story |
| 1 | Epic 01 组织与员工主数据 + 登录/RBAC/审计骨架 | 可演示主数据 CRUD + 入离调转 |
| 2 | Epic 02 考勤假期 + Epic 06 请假自助 | 排班 + 请假审批可演示 |
| 3 | Epic 03 薪酬基础 + Epic 06 工资条 | 一次工资 run + ESS 工资条 |
| 4 | Epic 04 招聘 + Epic 05 绩效骨架 | 招聘到入职转化 + 绩效自评 |
| 5 | 收尾：联调 / 回归 / 审计 / Release Notes | MVP 演示就绪 |

## Epic 列表

| ID | 名称 | 对标 SAP/PeopleSoft | 状态 |
|---|---|---|---|
| EP-01 | 组织与员工主数据 | SAP OM+PA / PS Workforce Admin | ✅ 完成（S01-S05, 2026-06-16） |
| EP-02 | 考勤与假期 | SAP PT / PS Time Labor + Absence | ✅ 完成（S06-S08, 2026-06-17） |
| EP-03 | 薪酬基础 | SAP PY / PS Payroll | ✅ 完成（S09-S10, 2026-06-17） |
| EP-04 | 招聘与入职 | SAP Recruitment / PS eRecruit | ✅ 完成（S11-S12, 2026-06-17） |
| EP-05 | 绩效 | SAP PM / PS ePerformance | ✅ 完成（S13-S14, 2026-06-17） |
| EP-06 | ESS/MSS 自助服务 | SAP ESS/MSS | ✅ 后端 + 前端门户完成（2026-06-17） |

## 待办（PM 视角）

- [x] 初始化目录骨架与台账（2026-06-14）
- [x] 派单 product 写 PRD（首轮 analyst 仅出审查报告，PM 拍板 17 条决策后改派 executor 落盘 591 行 v0.1）
- [x] 派单 architect 写架构 + 数据模型（2026-06-14 已落盘 architecture.md 782 行 / data-model.md 642 行 / ADR-001~004 完成）
- [x] 派单 qa-lead 写测试策略（2026-06-14 已落盘 386 行）
- [x] 派单 reviewer 写审核清单（2026-06-14 已落盘 866 行）
- [x] 评审四份基线文档（PRD/架构/数据模型/测试/审核 对齐通过，2026-06-14）
- [x] 拆 Epic 01：epic-01-org-employee.md + 5 个自包含 Story（S01~S05）已落盘 2026-06-14
- [x] **Sprint 1 / S01 开发完成**：后端骨架 + Liquibase + 登录/JWT/锁/refresh + 前端登录页（2026-06-16）
- [ ] EP01-S01 QA 测试 + reviewer 审核 → S02 → S03/S04 并行 → S05 收口

## 风险与阻塞

| 风险 | 等级 | 缓解 |
|---|---|---|
| MVP 范围蔓延（用户后续可能想要全套 SAP 功能） | 高 | PRD 明确写 MVP 范围，超出范围一律进 backlog |
| 多数据库方言差异（如分页/JSON/CLOB） | 中 | 强制 Liquibase + MyBatis-Plus 分页插件，CI 矩阵覆盖 PG+MySQL |
| 自研轻量审批流不够用 | 中 | 留扩展点，必要时 Sprint 4 后切 Flowable |
| 工资计算规则简化 vs 真实需求 | 中 | MVP 仅做一档示例，不承诺合规性 |

## 决策记录指针
ADR 写在 `.bmad/decisions.md`（架构师维护）。
