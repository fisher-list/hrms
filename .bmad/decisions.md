# 架构决策记录（ADR）

> 由架构师维护。每条决策包含：背景 / 选项 / 决策 / 后果。

## 待决策清单（架构师在 Sprint 0 内完成 ADR-001 ~ ADR-004）

- [x] **ADR-001**：持久层选 MyBatis-Plus 而非 JPA（2026-06-14 Accepted）
- [x] **ADR-002**：DDL 管理用 Liquibase 而非 Flyway（2026-06-14 Accepted）
- [x] **ADR-003**：审批流 MVP 自研而非引入 Flowable/Activiti（2026-06-14 Accepted）
- [x] **ADR-004**：MVP 单体部署而非微服务（2026-06-14 Accepted）

---

## ADR-001：持久层选 MyBatis-Plus 而非 JPA

- **状态**：Accepted
- **日期**：2026-06-14
- **决策者**：architect
- **相关文档**：`docs/architecture.md` §3、§6

### 背景
HRMS 需在 PostgreSQL / MySQL / Oracle / SQL Server / DM 达梦 5 个方言上跑通，且业务上有大量「列表筛选 + 分页 + 报表 + 多表 join」的查询（员工搜索、工资条汇总、组织树查询、审批待办列表）。持久层既要 CRUD 高效，也要复杂 SQL 可控。我们必须在 Spring Data JPA（含 Hibernate）和 MyBatis-Plus 之间二选一。

### 选项对比

| 维度 | Spring Data JPA / Hibernate | MyBatis-Plus |
|---|---|---|
| CRUD 简洁度 | `JpaRepository` 一行接口；命名约定生成 | `BaseMapper<T>` 同样一行 |
| 复杂查询 | JPQL / Criteria / `@Query`，跨方言抽象较强但调试难 | XML / 注解 SQL 可见可控，分页插件多方言成熟 |
| 多方言适配 | Hibernate `Dialect` 体系成熟，但报表 SQL 仍需 `@Query` 写方言分支 | `databaseId` + `<choose>` 显式分支；分页插件按 `DbType` 自动切 LIMIT/ROWNUM/OFFSET FETCH |
| 性能调优 | N+1 问题需要严格 `fetch join` 把控，二级缓存复杂 | SQL 即代码，无隐藏 SQL；性能问题易 trace |
| 学习曲线 | 概念多（持久化上下文 / 一级缓存 / 懒加载 / 实体状态），HR 后台开发 onboarding 慢 | SQL 即文档，Java 工程师开箱即用 |
| 生态（中文社区） | 国际成熟 | 国内 HR/ERP 后台主流，Mapper.xml 资料丰富 |
| 报表友好度 | 弱（JPQL 不擅长复杂分组、窗口函数等） | 强（任意 SQL 可写） |
| 与达梦 / 国产数据库适配 | Hibernate 方言非官方，第三方维护 | MyBatis-Plus 社区有完整 DM 适配 |

### 决策
**采用 MyBatis-Plus 作为唯一持久层框架**。Spring Data JPA 不引入。

### 理由
1. **HR/工资/报表场景 SQL 复杂度高**：多表 join、动态条件、自定义聚合、窗口函数，MyBatis 的 SQL 可见性显著降低 debug 成本。
2. **多方言风险可控**：MyBatis-Plus 分页插件按 `DbType` 自动切方言；JPA 走 Hibernate Dialect，国产数据库（DM）支持非官方，发布前矩阵测试不可控。
3. **不存在两个 ORM 并存的成本收益**：曾考虑「JPA 做简单 CRUD + MyBatis 做报表」，实际会带来事务边界混乱、实体不一致、团队认知负担。砍掉 JPA。
4. **团队认知**：MVP 团队熟悉 MyBatis 模式，培训成本最低。
5. **乐观锁、软删、自动填充、逻辑删除**全部由 MyBatis-Plus 内置注解满足，无功能短板。

### 后果

**正面**：
- SQL 调优路径短，性能问题易定位。
- 5 方言适配的工作量集中在 Mapper.xml 的 `databaseId` 分支，不渗透到业务代码。
- 报表与列表查询开发节奏快。

**负面**：
- 失去 JPA 的实体状态管理（如脏检查 / 级联保存），需在 Service 层手工管理多表更新。
- 数据库 schema 与 Java Entity 双向同步靠人工，需 reviewer 在 PR 中检查（可写工具脚本辅助）。

**扩展点**：
- 后续若需要复杂 OLAP 报表，可叠加 jOOQ 用于 type-safe 查询，**不改回 JPA**。
- 多租户拦截器（`tenant_id`）已在数据模型预留，未来启用时通过 MyBatis-Plus `TenantLineInnerInterceptor` 接入，无业务侵入。

---

## ADR-002：DDL 管理用 Liquibase 而非 Flyway

- **状态**：Accepted
- **日期**：2026-06-14
- **决策者**：architect
- **相关文档**：`docs/architecture.md` §6.2、`db/changelog/`

### 背景
HRMS DDL 必须跨 5 方言迁移，且要支持回滚、按 Epic 拆分维护、CI 多方言矩阵跑同一套 changelog。Java 生态主流的 schema 迁移工具有 Flyway 与 Liquibase。

### 选项对比

| 维度 | Flyway | Liquibase |
|---|---|---|
| 配置格式 | 纯 SQL（V1__init.sql） | XML / YAML / JSON / SQL changeSet |
| 跨方言 | 弱：每个方言一份 SQL 文件，靠 `flyway.locations` 区分；维护成本高 | 强：抽象类型（`bigint` / `varchar` / `clob`）由 Liquibase 自动翻译方言；差异处用 `dbms` 限定 |
| 回滚 | 社区版无 undo，付费版才支持 | 开源版原生支持 `<rollback>` |
| 国产数据库（DM） | 第三方适配 | 官方/社区适配齐全 |
| 学习曲线 | 极低（写 SQL） | 中（要熟悉抽象类型与 changeSet 结构） |
| 校验能力 | 基于 hash 的版本校验 | 基于 changeSet id + author + filename 的更细粒度校验 + `validCheckSum` 灵活 |
| 数据初始化（seed） | 走 SQL 即可 | `loadData` 直接读 CSV，方言透明 |
| 与 Spring Boot 集成 | 自动配置 | 自动配置 |

### 决策
**采用 Liquibase（YAML 格式 changeSet）作为 DDL 管理工具**。Flyway 不引入。

### 理由
1. **跨方言适配是 P0 需求**，Liquibase 抽象类型一次写、多方言自动翻译，Flyway 必须 5 份 SQL 维护，长期维护成本巨大。
2. **回滚原生支持**，发布失败时可走标准回滚，符合「演进式架构」原则。
3. **changeSet 拆分**按 Epic 一份文件（`changelog-01-org.yaml`、`changelog-02-employee.yaml` …），主入口 include 聚合，模块独立演进。
4. **种子数据**走 `loadData` 加载 CSV，方言透明，dev profile 自动加载，prod 跳过。
5. **国产数据库覆盖**：Liquibase 官方/社区对达梦支持完整，Flyway 生态较弱。

### 后果

**正面**：
- 一份 changelog 跨 5 方言初始化通过，是 MVP 的 P0 验收点之一。
- 团队认知统一：所有 DDL 必须走 changeSet，禁止手动改库。
- 回滚有标准化路径，发布前可演练。

**负面**：
- Liquibase YAML 学习曲线略高于 Flyway 纯 SQL，团队需在 Sprint 0 末做 30 分钟培训。
- 复杂 SQL（如方言专属函数）仍需 `dbms` 分支处理，无法 100% 抽象。

**扩展点**：
- 多租户多 schema 部署时，Liquibase `default-schema` 参数化即可适配。
- 性能基准变化时，可在 changeSet 中加索引，不破坏既有 schema 历史。

---

## ADR-003：审批流 MVP 自研而非引入 Flowable / Activiti

- **状态**：Accepted
- **日期**：2026-06-14
- **决策者**：architect
- **相关文档**：`docs/architecture.md` §8、`docs/data-model.md` §7.5

### 背景
HRMS 中至少 6 类业务需要审批流：请假、加班、Offer、工资 run、员工调岗、员工离职。可选方案：（A）引入开源 BPMN 引擎（Flowable / Camunda / Activiti），（B）自研 4 表轻量审批流。

### 选项对比

| 维度 | Flowable / Camunda / Activiti | 自研轻量审批流（4 表） |
|---|---|---|
| 功能完备度 | BPMN 2.0 全集（并行 / 网关 / 子流程 / 时间事件 / 信号 / 补偿） | MVP 6 类需求够用：单/多节点串行、撤回、拒绝、超时；并行预留扩展点 |
| 学习与运维 | BPMN 概念 + Modeler 工具 + 50+ 内置表 | 4 张表 + 1 个 Service，0 学习成本 |
| 多方言适配 | Flowable 官方支持 PG / MySQL / Oracle / MSSQL，达梦需自适配 | 完全在 HRMS 自己的 DDL 里，5 方言一致 |
| 与业务模型耦合 | 需在 BPMN 模型中定义流程；变更要重发版 | JSON 配置（`approval_definition.nodes`），运行时可变 |
| 流程统计 / 报表 | 强 | 简单（4 表 SQL 即可） |
| 大型企业适用 | 是 | 否（流程超过 5-7 节点会力不从心） |
| 启动数据膨胀 | 50+ 内置表 + 历史归档表 | 4 张表 |
| 二次开发成本 | 高（Listener / Delegate / 任务表单 / Form Engine） | 低（直接改自己的 Service） |

### 决策
**MVP 自研 4 表轻量审批流**（`approval_definition` / `approval_instance` / `approval_task` / `approval_history`）。Flowable / Activiti / Camunda 不引入。

### 理由
1. **YAGNI（You Aren't Gonna Need It）**：MVP 6 类审批均为线性串行，无并行 / 网关 / 时间事件需求，BPMN 引擎能力 90% 闲置。
2. **运维成本**：BPMN 引擎引入会带来 50+ 张额外表、Modeler 工具链、版本升级负担。MVP 单租户演示原型不应承担。
3. **多方言风险隔离**：自研 4 表完全在 HRMS Liquibase 管理下，跨方言一致；BPMN 引擎的内置表跨方言风险不可控。
4. **演进路径清晰**：审批流通过事件总线（`ApprovalCompletedEvent`）与业务模块解耦，未来若复杂度上升，可整体替换为 Flowable，业务模块零改动。
5. **节点配置 JSON 化**：`approval_definition.nodes` 字段为 JSON 字符串，运行时可改，避免重发版。

### 后果

**正面**：
- 审批流引擎可在 Sprint 1 内 1 名研发 1 周内完工，不阻塞业务 Epic。
- 4 张表语义清晰，新成员上手快。
- 跨方言风险与业务一致，无独立测试矩阵。

**负面**：
- 不支持并行会签 / 网关分流 / 自动跳过等高级特性，超出能力时需人工绕开。
- 自研引擎缺少可视化流程设计器，运维需通过 SQL 或后台管理页面维护流程定义。

**扩展点**：
- `approval_task.parallel_group` 字段已预留，未来支持并行会签时不需改 schema。
- 与业务的解耦点是 `ApprovalCompletedEvent`，整体替换 Flowable 时只需重写 `ApprovalService` 实现。
- 流程定义版本字段 `approval_definition.version` 已预留，支持新旧流程共存。

---

## ADR-004：MVP 单体部署而非微服务

- **状态**：Accepted
- **日期**：2026-06-14
- **决策者**：architect
- **相关文档**：`docs/architecture.md` §1、§4、§11

### 背景
HRMS 业务划分为 6 大 Epic，每个 Epic 在 SAP HCM 中是独立子模块。技术上有两种部署形态可选：（A）从一开始拆微服务（按 Epic 一服务），（B）单体多 module（Maven module 边界，单一可部署单元）。

### 选项对比

| 维度 | 微服务（每 Epic 一服务） | 模块化单体（Modular Monolith） |
|---|---|---|
| 启动复杂度 | 6 服务 + 注册中心 + 网关 + 配置中心 + 链路追踪 + 5 数据库实例 / schema | 1 个 Spring Boot + 1 个数据库 |
| MVP 演示成本 | 高（演示日要起 ≥10 个容器） | 低（docker-compose up 一条命令） |
| 跨模块事务 | 分布式事务（Seata / Saga / TCC），复杂 | 本地事务 `@Transactional` |
| 数据共享 | 跨服务调用 RPC / 事件 | 直接 Service 注入 |
| 测试 | 集成测试 / 端到端测试设施投入大 | Spring Boot Test + Testcontainers 即可 |
| 团队协作 | 适合 ≥3 个独立小组 | 适合 1-2 个紧密小组（HRMS 当前规模） |
| 演进到大规模 | 一开始就到位 | 后续按需拆，工程量集中在 1-2 周 |
| 故障域 | 服务隔离强 | 单点 |
| MVP 演示风险 | 任一服务挂 → 演示中断 | 单进程稳定可控 |

### 决策
**MVP 采用模块化单体部署**：Maven 多 module（`hrms-org` / `hrms-employee` / `hrms-attendance` / `hrms-payroll` / `hrms-recruit` / `hrms-performance` / `hrms-portal-api` / `hrms-common` / `hrms-app`），单一启动 jar，单一数据库。微服务化推迟到 MVP 验收后视业务规模决定。

### 理由
1. **MVP 目标是端到端演示**，不是大规模生产，启动复杂度必须最小化。
2. **架构允许后续无痛拆分**：模块间已通过 Service 接口解耦、不互相直查表；数据库 schema 已按模块隔离表前缀；审批流、字典、附件、审计走事件 / 接口契约。后续切微服务的边界已就位。
3. **跨模块事务简化**：员工入职、工资发放、Offer 转员工等流程跨多个模块，本地事务一行 `@Transactional` 即解决；微服务下要做 Saga，MVP 阶段是过度工程。
4. **运维节奏**：MVP 阶段没有 SRE / DevOps 专职，单体部署降低故障面。
5. **业界共识**：Sam Newman、Martin Fowler 都建议「Monolith First」，等业务边界清晰、团队规模到位后再拆。

### 后果

**正面**：
- Sprint 1 即可上线演示主路径，不被基础设施拖慢。
- 测试简化（一个 Spring Boot Test 上下文跑全部模块）。
- 故障定位集中。

**负面**：
- 单进程承载所有模块，CPU / 内存上限受单机限制（MVP 不是问题）。
- 任一模块的高负载（如批量工资 run）可能拖慢其它模块——通过线程池隔离 + 异步任务 mitigate。
- 团队规模扩张时，代码合并冲突会增多——通过 module 边界 + CODEOWNERS 减缓。

**扩展点**：
- 已预设的拆分顺序：① `hrms-payroll`（计算密集，先切）② `hrms-attendance`（写入密集，次切）③ `hrms-portal-api`（前端 BFF，最后切）。
- 拆分时引入：Spring Cloud Gateway（替代 Nginx 反代部分能力）、Nacos / Consul（注册 + 配置）、Sentinel / Resilience4j（限流熔断）、Seata（分布式事务）、SkyWalking / Jaeger（链路追踪）。
- 数据库拆分：每模块独立 schema → 独立实例，迁移路径已在 Liquibase 多 default-schema 支持下平滑。
- 事件总线：MVP 用 Spring `ApplicationEventPublisher`，拆分后替换为 Kafka / RabbitMQ，业务代码不改（监听器接口保持）。

