# HRMS 测试策略（test-strategy.md）

> 责任人：测试总监（qa-lead） · 版本：v0.1 · 日期：2026-06-14
> 项目阶段：Sprint 0 · Planning Phase
> 上位文档：`/Users/apple/.claude/plans/graceful-splashing-kitten.md` 第五节

本文件是 HRMS 项目的测试基线。所有 Story 的测试用例编写、缺陷管理、回归节奏、演示日 Go/No-Go 都按本文件执行。研发（dev-be/dev-fe/dev-db）、PM、审核员均需按本文档要求协作。

---

## 一、测试原则

测试不是"功能写完之后再补"的环节，而是与研发同周期的工程产物。本项目坚持以下四条原则：

1. **测试金字塔（Pyramid first）**：底层多、上层少。单元测试 ≈ 70%，接口测试 ≈ 20%，E2E ≈ 10%。任何想用 E2E 覆盖单元能解决的问题都要被驳回——E2E 是演示主路径的"压舱石"，不是 bug 探针。
2. **缺陷左移（Shift-Left）**：测试用例随 Story 一并评审；研发提交前必须自跑单测；接口测试在 PR 合并前跑通；不让缺陷从研发态溜进测试态。每发现一次"研发本可以自测拦住"的缺陷，都要回写到 `.bmad/decisions.md` 作为流程改进。
3. **自动化优先（Automation by default）**：所有可重复用例必须可自动化执行。不接受"我口头测过"。手工探索性测试只在新功能初次评审时使用，发现问题后立即转写自动化用例。
4. **可重复且可隔离（Reproducible & isolated）**：测试不依赖外部环境、不依赖前一个用例的副作用、不依赖真实当前时间。任何用例必须能在干净容器里独立跑通；多数据库测试用 Testcontainers profile 切换，禁止本地共享 DB。

> 一句话：**用例不能跑就不算用例；E2E 只为主路径站岗；其他全部下沉到单元和接口层。**

---

## 二、测试分层与工具

### 2.1 单元测试（Unit Test）

| 项 | 内容 |
|---|---|
| 框架 | JUnit 5（Jupiter）+ Mockito 5 + AssertJ |
| 责任人 | 研发（dev-be、dev-fe）自测 |
| 覆盖率门槛 | **行覆盖 ≥ 60%**，关键 Service 层 ≥ 80% |
| 边界 | 不连数据库、不连网络；DB/HTTP/MQ 全部 Mock |
| 命令 | `mvn -pl hrms-<module> test` |
| 失败处理 | PR 红线：单测失败的 PR 不进入接口测试 |
| 度量工具 | JaCoCo（后端）、Vitest coverage（前端） |

前端单元测试：Vitest + Vue Test Utils，组件粒度，覆盖 props 渲染、emit 事件、关键计算属性、表单校验函数。覆盖率门槛同后端 60%。

### 2.2 接口测试（Integration Test）

| 项 | 内容 |
|---|---|
| 框架 | Spring Boot Test（`@SpringBootTest` + `MockMvc`/`WebTestClient`）+ Testcontainers |
| 责任人 | qa-lead 主导，dev-be 协助 |
| 触发频率 | 每个 Epic 完成时跑全部；每日 nightly 跑核心模块 |
| 数据库容器 | **PostgreSQL 16 默认，每个 Epic 抽测 MySQL 8** |
| 命令 | `mvn -Dspring.profiles.active=it,postgres verify`<br>`mvn -Dspring.profiles.active=it,mysql verify` |
| 隔离 | 每个测试类用 `@Transactional` 回滚或 `@Sql` 重置；禁止用例间共享状态 |

关键：**Testcontainers 必须真启容器**，不能用 H2 替代——HRMS 的 DDL/分页/JSON/数值精度都强依赖真实方言。

### 2.3 端到端测试（E2E）

| 项 | 内容 |
|---|---|
| 框架 | Playwright（TypeScript）+ Allure 报告 |
| 责任人 | qa-lead 编写并维护 |
| 覆盖范围 | **6 个 Epic 各自的演示主路径**（见第四节） |
| 浏览器 | Chromium 必跑，Firefox/WebKit 发布前抽测 |
| 数据 | 每次跑前用 Liquibase `tag:test-seed` 重置数据库 |
| 命令 | `npx playwright test --project=chromium` |
| 通过率 | 演示日 Go/No-Go 闸口 ≥ 90% |

### 2.4 数据库迁移测试

| 项 | 内容 |
|---|---|
| 工具 | Liquibase 4.x + Testcontainers |
| 验证维度 | 1) 正向迁移：空库执行所有 changeSet 成功；2) 回滚：每个 changeSet 必须有 rollback 块，能滚回前一版本；3) 幂等：重复执行同 changelog 不报错 |
| 矩阵 | PostgreSQL（必跑）+ MySQL 8（必跑）；发布前抽测 SQL Server |
| 命令 | `mvn -pl db -Dliquibase.contexts=test liquibase:update` 与 `liquibase:rollbackCount` |
| 责任人 | dev-db 编写 changelog，qa-lead 编写迁移测试 |

### 2.5 性能冒烟（Performance Smoke）

非性能压测，仅做基线观测，防止回归。

| 场景 | 数据规模 | 基线目标 | 工具 |
|---|---|---|---|
| 员工列表分页查询 | 500 员工 | p95 < 500ms | JMH / Gatling |
| 考勤批量导入 | 10000 条考勤 | 总耗时 < 30s | 自写 Spring Boot Test |
| 一次工资计算 run | 500 员工 × 1 周期 | 总耗时 < 60s | 自写 Spring Boot Test |
| ESS 工资条查询 | 单员工单月 | p95 < 200ms | Playwright + 自带计时 |

性能基线写入 `docs/perf-baseline.md`（首次运行后由 qa-lead 落地），后续每个 Epic 完成时回归一次，下滑超过 20% 必须在该 Sprint 内修复。

---

## 三、测试用例设计原则

### 3.1 用例数量与分布

每个 Story **至少 3 条用例**，且必须覆盖三类：

| 类型 | 占比建议 | 示例 |
|---|---|---|
| 正向（Happy Path） | 1+ | 合法输入、合法权限、按预期流转 |
| 边界（Boundary） | 1+ | 字段长度上限、金额精度、时间临界（年末跨年、闰年）、零/空集合、最大记录数 |
| 异常（Negative） | 1+ | 非法输入、并发冲突、网络中断、权限不足、状态机非法转移 |

### 3.2 AC ↔ E2E 映射规则

**Story 的每一条验收标准（AC）必须 1:1 映射到至少一条 E2E 用例**。映射关系写在 Story 文件的 "Test" 段，格式：

```
AC-1: 员工可以提交请假申请 → E2E:leave-submit-happy
AC-2: 余额不足时拒绝提交 → E2E:leave-submit-balance-insufficient
```

PM 在 Story Done 之前必须核对此表完整。

### 3.3 状态机覆盖（强制）

以下字段属于"状态机字段"，必须**全状态转移覆盖**，包括非法转移的拒绝行为：

| 模块 | 字段 | 状态集合（示例） |
|---|---|---|
| 员工 | `employee_status` | applicant → active → on_leave → transferred → terminated |
| 请假 | `leave_status` | draft → submitted → approving → approved / rejected → cancelled |
| Offer | `offer_status` | drafted → sent → accepted / declined → expired |
| 工资 run | `payroll_run_status` | created → calculating → calculated → confirmed → paid |
| 招聘候选人 | `candidate_status` | new → screening → interviewing → offered → hired / rejected |
| 绩效周期 | `cycle_status` | draft → in_progress → self_review → manager_review → finalized |

每个状态转移至少 1 条用例；非法转移（如 paid → calculating）必须有 1 条用例验证 API 返回 4xx 且数据未变。

### 3.4 权限与数据范围（强制）

每个角色对每个数据敏感接口都要有用例：

| 角色 | 数据范围 | 必测点 |
|---|---|---|
| 普通员工（ESS） | 仅本人 | 看不到他人工资条/档案；接口直接返回 403 |
| 经理（MSS） | 本人 + 下属 | 跨部门员工不可见；越权请求 403 |
| HR | 本部门 / 全公司（按授权） | 跨授权范围越权 403 |
| 系统管理员 | 全部 | 仅管理员可改 RBAC |

**任何接口未覆盖"越权"用例的，审核员有权打回。**

---

## 四、6 个 Epic 的测试要点

### 4.1 Epic 01 组织与员工主数据

- 组织树 CRUD：新增/移动子树/删除（含级联保护——有员工的部门不允许直接删除）
- 员工状态机：入职/调岗/晋升/离职全转移；调岗时历史任职记录正确归档
- 批量导入：CSV 字段缺失/编码错误/重复工号；部分失败时事务回滚 vs 行级失败两种模式都要测
- 审计日志：每次状态变更必有审计记录，操作人/时间/前后值齐全
- 性能：员工列表 500 条分页 p95 < 500ms

### 4.2 Epic 02 考勤与假期

- 排班冲突：同一员工同一日期不可双班次；跨夜班次的日期归属
- 假期余额：跨年度结算（12-31 → 01-01 余额转结/清零规则）；闰年 2-29
- 多级审批流：1 级 / 2 级 / 3 级审批；中途撤回；审批人离职时的代理逻辑
- 加班自动计算：工作日/周末/法定假日三种系数；按小时取整规则
- 数据权限：员工只能给自己提请假；经理只看下属

### 4.3 Epic 03 薪酬基础

- **金额精度**：所有金额用 `BigDecimal(18,4)`，禁止用 `double`；用例必须断言精度（如 3333.3333 不能变成 3333.33）
- 个税分档：示例档位边界值（临界点 ±0.01）必须有用例
- 工资条隐私：员工只看自己；HR 看授权范围；接口越权必 403
- 工资 run 状态机：calculating 中不允许重复触发；calculated 后允许重算（生成新版本）；paid 后锁定
- 重算幂等：同输入 run 两次结果一致

### 4.4 Epic 04 招聘与入职

- 候选人去重：手机号 + 邮箱组合唯一；同一人多次投递归并到同一档案
- Offer 接受触发入职：Offer accepted → 自动生成员工档案 draft → HR 确认入职日期 → 状态转 active
- 附件上传：文件类型白名单（pdf/docx/jpg/png）、大小上限（10MB）、病毒扫描占位
- 招聘漏斗数据：各状态计数与状态机变更同步

### 4.5 Epic 05 绩效

- 周期约束：同一员工同一周期只能有一份评价；周期未开启时不可提交
- 自评 vs 上级评并发：双方同时提交时数据不互相覆盖（乐观锁版本号）
- 最终分计算：自评权重 + 上级评权重 + 校准系数；边界（0/100）值
- 周期状态机：draft → in_progress → self_review → manager_review → finalized 全转移

### 4.6 Epic 06 ESS / MSS

- 员工只看自己：所有 ESS 接口都要有越权用例
- 经理看下属：直接下属 + 跨级下属（按配置）
- 敏感字段脱敏：身份证（前 6 后 4 + ****）、银行卡（后 4）、薪资明细（非授权角色仅看到 "***"）
- 审批待办：经理首页待办计数与实际审批单一致

---

## 五、缺陷管理流程

### 5.1 缺陷写入位置

缺陷统一写在对应 Story 文件 `docs/stories/<story>.md` 末尾的 **"Defects" 段**。一个 Story 一段，缺陷追加。

### 5.2 缺陷字段

| 字段 | 说明 |
|---|---|
| 编号 | `BUG-<storyId>-<seq>`，如 `BUG-EP01-S02-001` |
| 严重等级 | P0 阻断主流程 / P1 重要功能错 / P2 体验或边界问题 / P3 文案或样式 |
| 复现步骤 | 1, 2, 3 步骤化，含测试数据 |
| 期望 | 与 AC 对照写 |
| 实际 | 现象 + 截图/日志路径 |
| 状态 | open → fixed → verified → closed；不修则 wontfix（PM 批准） |
| 责任人 | 修复研发的通讯名（dev-be / dev-fe / dev-db） |
| 提交时间 / 修复时间 | ISO 日期 |

### 5.3 流转

```
qa-lead 测出 → SendMessage 缺陷给 dev-* → dev 修复并标 fixed →
SendMessage 回 qa-lead → qa-lead 复测 →
verified → qa-lead 关闭 closed
```

- **P0 / P1 必须修复并 verified 才能 close Story**，PM 不接受带 P0/P1 关 Story。
- **P2** 可与 PM 协商进入 backlog（写入 `.bmad/status.md` 风险段）。
- **P3** 默认进 backlog，发布前批量处理。

---

## 六、回归测试

### 6.1 触发节奏

| 时机 | 范围 |
|---|---|
| 每完成一个 Epic | 全量跑该 Epic 的 E2E + **上一 Epic 的 E2E**（防回归） |
| 每周五 nightly | 全部接口测试（PG）+ 关键 E2E |
| 发布前（演示日前 2 个工作日） | **全量回归**：所有 E2E + 所有接口（PG 全部 + MySQL 冒烟）+ 多数据库切换冒烟 + 性能基线 |

### 6.2 回归报告

每次回归后由 qa-lead 在 `.bmad/status.md` 追加一行：

```
| 日期 | 触发 | E2E 通过率 | 接口通过率 | P0 数 | P1 数 | 备注 |
```

回归通过率低于 90% 时立刻 SendMessage 给 PM，触发临时修复 Sprint。

---

## 七、测试数据

### 7.1 标准数据集（test-seed）

通过 Liquibase changeSet `context=test-seed` 一键加载：

| 实体 | 数量 | 说明 |
|---|---|---|
| 公司 | 1 | "演示科技有限公司" |
| 部门 | 5 | 总经办 / 研发 / 产品 / 销售 / 人事 |
| 岗位 | 6 | CEO / 研发经理 / 高级工程师 / 产品经理 / 销售 / HR |
| 员工 | 20 | 覆盖各部门各岗位，含 1 名管理员、3 名经理、若干普通员工 |
| 假期类型 | 4 | 年假 / 病假 / 事假 / 调休 |
| 工资周期 | 1 | 当月一个周期，可触发一次 run |
| 候选人 | 5 | 各状态分布 |
| 绩效周期 | 1 | in_progress 状态 |

### 7.2 加载与重置

```bash
# 加载
mvn -pl db liquibase:update -Dliquibase.contexts=test-seed

# 重置（drop + 重新建库 + 加载）
mvn -pl db liquibase:dropAll
mvn -pl db liquibase:update -Dliquibase.contexts=test-seed
```

### 7.3 敏感数据规则

- 身份证、银行卡、手机号一律使用**符合校验规则的假数据生成器**（如 `Faker`），禁止使用真实数据
- 工资金额使用 4000~50000 范围的整百数字，避免与真实薪资接近
- 邮箱统一使用 `@example.com`

---

## 八、多数据库测试矩阵

这是 HRMS 架构关键验证点，单独成节。

### 8.1 矩阵设计

| 阶段 | PostgreSQL | MySQL 8 | SQL Server |
|---|---|---|---|
| 单元测试 | 不涉及 DB | — | — |
| 每个 Epic 完成 | **全部接口测试 + 全部 E2E** | **接口冒烟（CRUD + 分页 + 状态机）** | 不跑 |
| 发布前 | 全量回归 | 全量接口 + E2E 主路径 | **核心 CRUD + 分页抽测** |
| Liquibase 迁移 | 必跑（正向 + 回滚） | 必跑（正向 + 回滚） | 发布前抽测 |

### 8.2 切换方式

通过 Maven profile + Spring profile 双重切换，由 Testcontainers 自动起容器：

```bash
# PostgreSQL（默认）
mvn -Dspring.profiles.active=it,postgres verify

# MySQL 冒烟
mvn -Dspring.profiles.active=it,mysql verify -Dtest=*SmokeIT

# SQL Server 抽测
mvn -Dspring.profiles.active=it,sqlserver verify -Dtest=*CoreCrudIT
```

### 8.3 必测兼容点

任何 PR 改了以下内容，**必须跑 MySQL 冒烟**：

1. 任何 Liquibase changelog 改动
2. 任何 MyBatis XML 中的复杂 SQL（含 `<choose>`、子查询、CTE、窗口函数）
3. 分页接口
4. 涉及 JSON 字段的接口
5. 涉及金额计算的 SQL

---

## 九、演示日 Go / No-Go 准入标准

演示日前一个工作日，PM 召集 qa-lead 与 architect 走 Go/No-Go 检查。**全部满足才能 Go**：

| 检查项 | 通过标准 |
|---|---|
| 主路径 E2E 通过率 | ≥ 90% |
| P0 缺陷 | 0 |
| P1 缺陷 | ≤ 2，且演示路径不涉及 |
| 多数据库切换 | PG → MySQL 同 changelog 启动成功，登录通过 |
| 性能基线 | 全部场景达标，无超 20% 退化 |
| Liquibase 回滚 | 最近 5 个 changeSet 可回滚 |
| 演示数据 | test-seed 一键加载成功 |
| 文档齐套 | PRD / 架构 / 数据模型 / 测试策略 / 审核清单 / Epic / Story 全部齐全 |

任意一项不通过即 No-Go，PM 启动应急 Sprint。

---

## 十、测试用例模板

研发与 qa-lead 在 Story 文件 "Test" 段中按下表填写用例。**复制此模板使用**。

| 字段 | 填写说明 |
|---|---|
| 编号 | `TC-<storyId>-<seq>`，如 `TC-EP01-S02-001` |
| 标题 | 一句话描述测的行为，如"非 HR 不能查看他人工资条" |
| 类型 | 正向 / 边界 / 异常 |
| 优先级 | P0 主路径 / P1 重要 / P2 一般 / P3 文案 |
| 前置条件 | 数据 / 角色 / 状态前置 |
| 步骤 | 1. ... 2. ... 3. ... |
| 预期结果 | 与 AC 对照，含 HTTP 状态码、UI 提示、DB 状态 |
| 测试数据 | 引用 test-seed 中的固定 ID 或描述生成方式 |
| 关联 AC | AC-x（必填） |
| 自动化层级 | 单元 / 接口 / E2E |

### 用例示例

```
TC-EP02-S01-003
标题：员工年假余额不足时提交请假被拒绝
类型：异常
优先级：P1
前置：员工 EMP-001 年假余额 = 0；登录 EMP-001
步骤：
  1. 进入"我的请假" → "新建"
  2. 假期类型 = 年假，时长 = 1 天
  3. 提交
预期：
  - HTTP 400，错误码 LEAVE_BALANCE_INSUFFICIENT
  - UI 提示"年假余额不足"
  - leave_application 表无新增记录
测试数据：EMP-001（test-seed 提供）
关联 AC：AC-3
自动化层级：接口 + E2E
```

---

## 变更记录

| 版本 | 日期 | 作者 | 变更内容 |
|---|---|---|---|
| v0.1 | 2026-06-14 | qa-lead | 初稿，覆盖测试原则 / 分层 / 用例设计 / 6 Epic 要点 / 缺陷流程 / 回归 / 测试数据 / 多数据库矩阵 / Go-NoGo / 用例模板 |
