# HRMS 技术架构基线 v0.1

> 文档归属：技术架构师（architect）
> 适用范围：HRMS MVP 演示原型（单租户 / 单实例）
> 版本：v0.1 · 2026-06-14
> 配套文档：`docs/prd.md`（产品规划）、`docs/data-model.md`（数据模型）、`.bmad/decisions.md`（ADR）

本文件为 HRMS 项目 Sprint 0 的架构基线，覆盖系统架构原则、总体架构、技术选型、模块划分、多数据库适配、安全与权限、审批流、横切关注点、测试金字塔、部署、可观测性、CI 矩阵共 13 个章节。所有 ADR（ADR-001 ~ ADR-004）的决策依据见 `.bmad/decisions.md`。

---

## 1. 架构原则

HRMS 为 MVP 演示原型，但同时是「未来正式产品」的雏形，因此架构必须在「现在简单可跑」与「后续可演进」之间取得平衡。我们确立 7 条原则：

1. **模块化优先（Modular Monolith）**：MVP 用 Maven 多 module 单体部署，但模块边界按未来微服务拆分预设；模块间只通过 Service 接口依赖，禁止跨模块直查表。
2. **多数据库方言中性**：全部 DDL 走 Liquibase，全部 SQL 优先 MyBatis-Plus 自适配，禁用方言专属类型与函数；方言差异在 changeSet 与 `_databaseId` 中显式声明。
3. **可演进**：MVP 单体 → 后续 Epic 可独立切出微服务（hrms-payroll / hrms-attendance 优先），ESS 端点天然适合做 BFF 拆分。
4. **业务域驱动**：模块按 SAP HCM 经典域划分（OM / PA / PT / PY / Recruitment / PM / ESS-MSS），不按技术分层划分（不出现 controller-module / dao-module 这种反域驱动结构）。
5. **接口先行**：每个 Epic 启动前先在 `docs/api-spec.md` 写 OpenAPI 摘要，前后端基于契约并行；运行时由 springdoc-openapi 校验。
6. **可观测**：从第一行代码起注入结构化日志（JSON）、操作审计、Actuator 健康端点，禁止后期补观测。
7. **安全是底线**：JWT + RBAC + 数据权限 + 敏感字段加密 + 审计日志，五件套从 Sprint 1 全部启用，不延后到上线前。

---

## 2. 总体架构

### 2.1 部署视图（C4 Context + Container 简化）

```
                ┌────────────────────────────────────────────┐
                │             浏览器（Chrome / Edge）         │
                │   Vue 3 + Vite + TypeScript + Element Plus  │
                │     Pinia (state) · Vue Router (route)      │
                └──────────────┬─────────────────────────────┘
                               │  HTTPS · JSON · JWT (Bearer)
                               ▼
        ┌────────────────────────────────────────────────────┐
        │              Nginx（静态资源 + 反向代理）            │
        └──────────────┬─────────────────────────────────────┘
                       │ /api/** 反代到 8080
                       ▼
   ┌──────────────────────────────────────────────────────────┐
   │              Spring Boot 3.x（hrms-app 启动）             │
   │ ┌──────────────────────────────────────────────────────┐ │
   │ │ Filter Chain: CORS → JWT → Audit → ExceptionHandler  │ │
   │ ├──────────────────────────────────────────────────────┤ │
   │ │  Controllers (REST · springdoc-openapi)              │ │
   │ │     ├── hrms-org      (公司/部门/岗位/职位)          │ │
   │ │     ├── hrms-employee (员工档案 · 入离调转)          │ │
   │ │     ├── hrms-attendance (排班/打卡/请假/加班)        │ │
   │ │     ├── hrms-payroll  (薪资档案/工资 run/工资条)     │ │
   │ │     ├── hrms-recruit  (职位/候选人/面试/Offer)       │ │
   │ │     ├── hrms-performance (周期/目标/评分)            │ │
   │ │     └── hrms-portal-api (ESS/MSS 聚合接口)           │ │
   │ ├──────────────────────────────────────────────────────┤ │
   │ │  Services（事务边界 · 业务规则 · 审批回调）           │ │
   │ ├──────────────────────────────────────────────────────┤ │
   │ │  Mappers（MyBatis-Plus BaseMapper + 自定义 XML）     │ │
   │ │  Cross-cutting: hrms-common (auth / audit / dict /   │ │
   │ │     approval / exception / dto / util / i18n)        │ │
   │ └──────────────────────────────────────────────────────┘ │
   └──────────────┬───────────────────────────────────────────┘
                  │ JDBC（HikariCP · Snowflake ID）
                  ▼
        ┌────────────────────────────────────────┐
        │  RDBMS（任选其一，由 spring profile 切换）│
        │  PostgreSQL (默认) / MySQL 8 /          │
        │  Oracle / SQL Server / DM 达梦          │
        │  DDL 由 Liquibase 维护                  │
        └────────────────────────────────────────┘

旁路：
- 文件存储：本地 FS（MVP）/ 预留 S3 兼容接口
- 监控：Actuator → Prometheus（预留 endpoint）
- 日志：Logback JSON → stdout → docker-compose 日志卷
```

### 2.2 关键数据流（举例：员工提交请假）

```
[ESS 浏览器]
    ├─ POST /api/leave/applications  { employeeId, type, start, end }
    │
    ▼
[Controller LeaveController.submit()]
    ├─ JWT 解析 → 当前用户 → 校验 employeeId == self
    │
    ▼
[Service LeaveApplicationService.submit()]
    ├─ 业务规则校验（余额 / 重叠 / 班次）
    ├─ 写 leave_application（status = SUBMITTED）
    ├─ 调 ApprovalService.start(definitionCode = "LEAVE", refId)
    │   └─ 生成 approval_instance + 第一条 approval_task（指派经理）
    └─ 返回 applicationNo
        │
        ▼
[经理 MSS 浏览器]
    ├─ GET /api/approval/tasks?assignee=self → 列表
    └─ POST /api/approval/tasks/{id}/approve
        │
        ▼
[ApprovalService.approve()]
    ├─ 写 approval_history
    ├─ 推进 approval_instance（终态 APPROVED）
    └─ 触发回调 LeaveApplicationService.onApproved(refId)
        ├─ 更新 leave_application.status = APPROVED
        └─ 扣减 leave_balance.used_hours
```

---

## 3. 技术选型清单（带版本）

| 类别 | 技术 | 版本 | 选择理由（要点） |
|---|---|---|---|
| 运行时 | OpenJDK | 17 (LTS) | Spring Boot 3 强制 17+；valhalla 之前最稳的 LTS |
| 应用框架 | Spring Boot | 3.3.x | 3.x 切到 Jakarta 命名空间，长期主线 |
| Web | Spring MVC | 随 Boot | REST 控制器，不上 WebFlux（HR 系统多事务、不偏 IO 密集） |
| 安全 | Spring Security | 6.3.x | JWT Resource Server + 方法级权限注解 |
| 持久层 | MyBatis-Plus | 3.5.7+ | 见 ADR-001 |
| JDBC 池 | HikariCP | 随 Boot | 默认即可，Boot 自带 |
| DDL 管理 | Liquibase | 4.27+ | 见 ADR-002，跨方言 changeSet |
| 参数校验 | Spring Validation (Jakarta) | 随 Boot | `@Valid` + `@Validated` + 分组 |
| API 文档 | springdoc-openapi | 2.5+ | OpenAPI 3 自动生成、Swagger UI |
| JWT | java-jwt（auth0） | 4.4+ | API 简洁、签名算法可控 |
| ID 生成 | Hutool 雪花算法 | 5.8+ | 全局 64bit Long，避免数据库自增方言差异 |
| 加密 | Bouncy Castle / JDK | 1.78+ | AES-256-GCM 字段加密 |
| 缓存 | Caffeine | 3.x | 本地缓存（字典/权限），分布式预留 Redis 接口 |
| 日志 | Logback + logstash-logback-encoder | 7.x | JSON 结构化输出 |
| 监控 | Spring Boot Actuator + Micrometer | 随 Boot | Prometheus 端点预留 |
| 单元测试 | JUnit 5 + Mockito | 5.10 / 5.x | 标准组合 |
| 集成测试 | Spring Boot Test + Testcontainers | 1.20+ | 起真实 PG/MySQL 容器跑接口测试 |
| E2E | Playwright | 1.45+ | 跨浏览器 + Trace 录像 |
| 构建 | Maven | 3.9+ | 多 module 友好，Gradle 不必要 |
| 代码质量 | Spotless + Checkstyle | 最新 | 提交前 hook |
| **前端框架** | Vue | 3.4+ | SFC + Composition API |
| 构建 | Vite | 5.x | dev 体验好、HMR 快 |
| 语言 | TypeScript | 5.4+ | 强类型，配合后端 OpenAPI 生成类型 |
| UI 组件 | Element Plus | 2.7+ | HR 后台型 UI 组件齐全 |
| 状态管理 | Pinia | 2.x | Vue 3 官方推荐 |
| 路由 | Vue Router | 4.x | 标配 |
| HTTP | Axios | 1.7+ | 拦截器封装 token / error handling |
| 表单 | Element Plus Form + VeeValidate（备选） | - | 简单场景直接用 Element Plus |
| 图表 | ECharts | 5.5+ | 组织树 / 报表 |
| **基础设施** | Docker + Docker Compose | 24+ / v2 | 一键起 PG + 应用 + 前端 |
| Nginx | 1.27+ | 静态托管 + 反代 |
| CI | GitHub Actions（或 Gitlab CI） | - | 矩阵跑 PG / MySQL |

---

## 4. 后端模块划分（Maven 多 module）

```
hrms-parent (pom)
├── hrms-common              # 公共：auth / audit / dict / approval / exception / dto / util / i18n
├── hrms-org                 # 组织：公司/部门/岗位/职位/任职关系
├── hrms-employee            # 员工：档案/合同/教育/经历/紧急联系人/银行卡/家属/入离调转
├── hrms-attendance          # 考勤：班次/排班/打卡/请假/加班/假期余额
├── hrms-payroll             # 薪酬：薪资档案/工资 run/工资条/个税/社保
├── hrms-recruit             # 招聘：职位发布/候选人/简历/面试/Offer
├── hrms-performance         # 绩效：周期/模板/目标/评分
├── hrms-portal-api          # ESS/MSS 聚合接口（依赖上述业务 module 的 Service）
└── hrms-app                 # 启动 module（main class + application.yml + Liquibase 主入口）
```

### 4.1 模块依赖矩阵

|  | common | org | employee | attendance | payroll | recruit | performance | portal | app |
|---|---|---|---|---|---|---|---|---|---|
| common | - | - | - | - | - | - | - | - | - |
| org | ✓ | - | - | - | - | - | - | - | - |
| employee | ✓ | ✓ | - | - | - | - | - | - | - |
| attendance | ✓ | ✓ | ✓ | - | - | - | - | - | - |
| payroll | ✓ | ✓ | ✓ | ✓ | - | - | - | - | - |
| recruit | ✓ | ✓ | ✓ | - | - | - | - | - | - |
| performance | ✓ | ✓ | ✓ | - | - | - | - | - | - |
| portal | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - | - |
| app | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - |

依赖原则：
- 单向依赖，无环。
- `hrms-common` 不依赖任何业务 module。
- `hrms-portal-api` 仅依赖业务 module 的 `Service` 接口与 DTO，不依赖 Mapper。
- `hrms-app` 是装配根，禁止承载业务代码（除 main class、Boot 配置、Liquibase 主 changelog）。

### 4.2 模块职责说明

- **hrms-common**：JWT 工具、`@HasPermission` 注解与切面、全局异常处理器、统一响应 `R<T>`、操作日志切面、敏感字段 `@Sensitive` 注解 + TypeHandler、字典服务、轻量审批流引擎、附件上传服务、i18n MessageSource、Snowflake ID 工厂、MyBatis 自动填充器（自动写 `created_at/updated_at/created_by/updated_by`）、`BaseEntity`。
- **hrms-org**：四级组织树（company / department / position / job）。注意区分「岗位 position」（坑位，可空岗）与「职位 job」（职级体系），任职关系另存于 employee 表。
- **hrms-employee**：员工生命周期（PENDING_HIRE → ACTIVE → ON_LEAVE → TERMINATED）+ 7 张子表（合同 / 教育 / 工作经历 / 紧急联系人 / 银行卡 / 家属 / 附件）+ 调岗晋升离职审批。
- **hrms-attendance**：工作日历、班次、排班、打卡记录、请假/加班申请单、假期余额（每年初结算）。
- **hrms-payroll**：薪资档案（基本/岗位/绩效/补贴）、工资 run（DRAFT → CALCULATED → APPROVED → PAID）、工资条、个税示例规则、社保示例规则。
- **hrms-recruit**：职位发布 → 候选人简历库 → 面试评价 → Offer → 接受后回调 employee 模块发起入职。
- **hrms-performance**：周期 / 模板 / 目标 / 自评 / 上级评 / 最终得分。
- **hrms-portal-api**：ESS（员工自助）和 MSS（经理自助）的聚合 BFF 端点，UI 友好的字段裁剪 + 多模块数据合并。
- **hrms-app**：装配 + Liquibase master changelog + 多 profile（dev / pg / mysql / oracle / sqlserver / dm / docker / prod）。

---

## 5. 前端目录约定（Vue 3 + Vite + TS）

```
frontend/
├── public/
├── src/
│   ├── main.ts                   # 入口
│   ├── App.vue
│   ├── api/                      # 接口封装（按 module）
│   │   ├── http.ts               # axios 实例（拦截器 / token / error）
│   │   ├── org.ts
│   │   ├── employee.ts
│   │   ├── attendance.ts
│   │   ├── payroll.ts
│   │   ├── recruit.ts
│   │   ├── performance.ts
│   │   └── portal.ts
│   ├── components/               # 全局通用组件（不绑业务）
│   │   ├── DictSelect.vue
│   │   ├── OrgTree.vue
│   │   ├── DataTable.vue
│   │   └── ApprovalTimeline.vue
│   ├── views/                    # 页面（按业务域分目录）
│   │   ├── login/
│   │   ├── org/
│   │   ├── employee/
│   │   ├── attendance/
│   │   ├── payroll/
│   │   ├── recruit/
│   │   ├── performance/
│   │   └── portal/               # ESS/MSS
│   ├── router/
│   │   ├── index.ts              # 路由表
│   │   └── guards.ts             # 登录守卫 + 权限守卫
│   ├── store/                    # Pinia
│   │   ├── auth.ts               # 登录态 + token + 当前用户 + 权限码
│   │   ├── dict.ts               # 字典缓存
│   │   └── app.ts                # UI 全局状态
│   ├── utils/
│   │   ├── permission.ts         # 权限判断 hasPerm()
│   │   ├── format.ts             # 日期/金额格式化
│   │   └── download.ts           # 附件/工资条下载
│   ├── styles/
│   └── assets/
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

约定：
- **api/** 文件按业务 module 一一对应后端，函数命名 `verbResource`（`createEmployee` / `listLeaveApplications`）。
- **views/** 与 **router/** 的层级保持镜像，便于代码导航。
- **store/auth.ts** 持有 `permissions: Set<string>`，`utils/permission.ts` 暴露 `hasPerm(code)` 给路由守卫和按钮级 v-permission 指令。
- 字典数据由 `store/dict.ts` 全量预拉，组件直接 `<DictSelect type="EMP_STATUS" />`。
- 接口类型定义由 OpenAPI 自动生成到 `src/api/types.gen.ts`（`openapi-typescript`），禁止手写 DTO 类型。

---

## 6. 多数据库适配方案（核心章节）

HRMS 必须在 PostgreSQL（首选 / 默认 dev）/ MySQL 8 / Oracle 19c+ / SQL Server 2019+ / DM 达梦 8 五个方言上跑通。本章规定从配置、DDL、SQL、类型映射、主键、字符集 6 个维度的一致策略。

### 6.1 数据源配置：profile 切换

`hrms-app/src/main/resources/` 下：

```
application.yml                  # 公共配置
application-pg.yml               # PostgreSQL（默认 dev）
application-mysql.yml            # MySQL 8
application-oracle.yml           # Oracle 19c
application-sqlserver.yml        # SQL Server 2019
application-dm.yml               # 达梦 8
application-docker.yml           # docker-compose（PG）
```

公共配置中：
```yaml
spring:
  profiles:
    active: pg                   # 默认
mybatis-plus:
  global-config:
    db-config:
      id-type: assign_id          # 雪花
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
liquibase:
  change-log: classpath:db/changelog/db.changelog-master.yaml
  default-schema: ${HRMS_SCHEMA:public}
```

各 profile 文件只覆盖 `spring.datasource.{url,driver-class-name,username,password}` 与 `liquibase.default-schema`，其它一致。运行时通过 `SPRING_PROFILES_ACTIVE=mysql` 切换；CI 矩阵也用此机制。

### 6.2 DDL：Liquibase YAML changeSet + 方言差异

主入口 `db/changelog/db.changelog-master.yaml` 只 include 各业务域 changelog：
```yaml
databaseChangeLog:
  - include: { file: db/changelog/changelog-00-baseline.yaml }
  - include: { file: db/changelog/changelog-01-org.yaml }
  - include: { file: db/changelog/changelog-02-employee.yaml }
  - include: { file: db/changelog/changelog-03-attendance.yaml }
  - include: { file: db/changelog/changelog-04-payroll.yaml }
  - include: { file: db/changelog/changelog-05-recruit.yaml }
  - include: { file: db/changelog/changelog-06-performance.yaml }
  - include: { file: db/changelog/changelog-99-seed.yaml }
```

通用建表用 Liquibase 抽象类型（`bigint`、`varchar(n)`、`timestamp`、`decimal(p,s)`、`clob`），由 Liquibase 翻译方言：

```yaml
- changeSet:
    id: 02-employee-create
    author: architect
    changes:
      - createTable:
          tableName: employee
          columns:
            - column: { name: id, type: bigint, constraints: { primaryKey: true, nullable: false } }
            - column: { name: emp_no, type: varchar(32), constraints: { nullable: false } }
            - column: { name: name, type: varchar(64), constraints: { nullable: false } }
            - column: { name: id_card_enc, type: varchar(256) }     # AES 加密后存
            - column: { name: status, type: varchar(16), defaultValue: PENDING_HIRE }
            - column: { name: hire_date, type: date }
            - column: { name: dept_id, type: bigint }
            - column: { name: position_id, type: bigint }
            - column: { name: created_at, type: timestamp, defaultValueComputed: CURRENT_TIMESTAMP }
            - column: { name: updated_at, type: timestamp }
            - column: { name: created_by, type: bigint }
            - column: { name: updated_by, type: bigint }
            - column: { name: deleted, type: tinyint, defaultValue: 0 }
            - column: { name: version, type: int, defaultValue: 0 }
      - addUniqueConstraint:
          tableName: employee
          columnNames: emp_no
          constraintName: uk_employee_emp_no
      - createIndex:
          tableName: employee
          indexName: idx_employee_dept
          columns: [{ column: { name: dept_id } }]
```

方言差异在同一 changeSet 用 `dbms` 限定：

```yaml
- changeSet:
    id: 04-payroll-json-column
    author: architect
    changes:
      # 方言差异：PG / MySQL 用原生 json，Oracle / SQL Server / DM 用 clob/varchar2
      - sql:
          dbms: postgresql,mysql
          sql: ALTER TABLE payslip ADD COLUMN extra_json JSON
      - sql:
          dbms: oracle,mssql,dm
          sql: ALTER TABLE payslip ADD extra_json CLOB
```

> 我们的策略是**避免**这种差异——见 6.4 类型映射，统一用 `String` 存 JSON 文本，业务层 Jackson 序列化。仅当不得不用时才走方言分支。

### 6.3 SQL 兼容：BaseMapper + databaseId 选择 + 分页插件

**首选**：所有简单 CRUD 走 MyBatis-Plus `BaseMapper<T>`：
```java
public interface EmployeeMapper extends BaseMapper<Employee> {}
```
分页用 `IPage` + `MybatisPlusInterceptor` 内置的 `PaginationInnerInterceptor(DbType.POSTGRES_SQL)`——**注意**：MVP 中 `DbType` 不写死，由 `mybatis-plus.global-config.db-config.db-type` 读 profile 配置自动设置。

**复杂查询**：自定义 Mapper.xml + `<choose>` + `_databaseId`：
```xml
<select id="searchEmployee" resultType="EmployeeVO" databaseId="postgresql">
  SELECT e.*, d.name AS dept_name
  FROM employee e
  LEFT JOIN department d ON d.id = e.dept_id
  WHERE e.deleted = 0
    <if test="kw != null"> AND e.name ILIKE CONCAT('%', #{kw}, '%') </if>
  ORDER BY e.id DESC
</select>
<select id="searchEmployee" resultType="EmployeeVO" databaseId="mysql">
  SELECT e.*, d.name AS dept_name
  FROM employee e
  LEFT JOIN department d ON d.id = e.dept_id
  WHERE e.deleted = 0
    <if test="kw != null"> AND e.name LIKE CONCAT('%', #{kw}, '%') </if>
  ORDER BY e.id DESC
</select>
<!-- oracle / mssql / dm 各写一份；公共结构提取到 <sql id="..."/> -->
```

`databaseId` 由 `VendorDatabaseIdProvider` 提供：
```yaml
mybatis-plus:
  configuration:
    database-id: ${db.id}
```
通过 `application-{profile}.yml` 设 `db.id=postgresql|mysql|oracle|mssql|dm`。

**分页**：MyBatis-Plus 分页插件根据 DbType 自动生成 `LIMIT ?,?`（MySQL/PG）/ `ROWNUM`（Oracle 11g 及以下）/ `OFFSET FETCH`（SQL Server 2012+ / Oracle 12c+ / DM）。MVP 不手写分页 SQL。

**禁止**：在 Mapper.xml 中使用 `LIMIT`、`TOP`、`ROWNUM`、`SYSDATE`、`NOW()` 等方言函数；时间用 `#{now}`（Java 端传入），日期函数用 MyBatis-Plus 抽象。

### 6.4 类型映射

| Java 类型 | DDL 类型（Liquibase 抽象） | 方言落地 | 说明 |
|---|---|---|---|
| `Long` (PK / FK) | `bigint` | PG `bigint` / MySQL `BIGINT` / Oracle `NUMBER(19)` / MSSQL `BIGINT` / DM `BIGINT` | 雪花 ID 必为 Long |
| `Integer` | `int` | 各方言均 `INT/INTEGER` | |
| `String` (短) | `varchar(n)` | PG `varchar(n)` / MySQL `VARCHAR(n)` / Oracle `VARCHAR2(n)` / MSSQL `NVARCHAR(n)` / DM `VARCHAR(n)` | 短文本 ≤ 4000 |
| `String` (长 / JSON / 富文本) | `clob` | PG `text` / MySQL `LONGTEXT` / Oracle `CLOB` / MSSQL `NVARCHAR(MAX)` / DM `CLOB` | **统一存字符串**，应用层 Jackson 序列化 |
| `LocalDate` | `date` | 各方言均 `DATE` | |
| `LocalDateTime` | `timestamp` | PG `timestamp` / MySQL `DATETIME(6)` / Oracle `TIMESTAMP` / MSSQL `DATETIME2` / DM `TIMESTAMP` | 不存时区 |
| `BigDecimal` (金额) | `decimal(18,4)` | 全部 `DECIMAL(18,4)` | 4 位小数留汇率；展示截断 |
| `Boolean` | `tinyint(1)` 或 `boolean` | PG `boolean` / MySQL `TINYINT(1)` / Oracle `NUMBER(1)` / MSSQL `BIT` / DM `TINYINT` | 由 Liquibase 抽象 `boolean` 翻译；`@TableField(typeHandler=...)` 兜底 |
| `byte[]` (附件) | `blob` | 各方言 `BLOB/VARBINARY(MAX)` | 大附件**不进库**，存文件系统/对象存储，DB 只存路径 |

**禁用项**：
- PostgreSQL 专属 `JSONB`、`UUID`、`ARRAY`、`HSTORE`、`CITEXT`。
- MySQL 专属 `ENUM`、`SET`、`UNSIGNED`。
- Oracle 专属 `NUMBER` 不带精度、`DATE` 当 timestamp 用、`SEQUENCE` 隐式依赖。
- SQL Server 专属 `UNIQUEIDENTIFIER`（默认值 `NEWID()`）、`MONEY`。
- 触发器、存储过程、函数索引（在 MVP 中）。

### 6.5 主键：Snowflake

- 全表主键统一 `bigint`，由 MyBatis-Plus `IdType.ASSIGN_ID`（雪花算法）生成。
- 不依赖任何方言的自增（`SERIAL` / `AUTO_INCREMENT` / `IDENTITY` / `SEQUENCE`），避免迁移与多实例 ID 冲突。
- workerId / dataCenterId 由环境变量注入（MVP 单实例固定 1/1，预留多实例配置）。
- 显式拒绝 UUID 主键（索引膨胀 + 范围查询不友好）。

### 6.6 字符集

- 全部数据库统一 UTF-8。
  - PostgreSQL：建库 `ENCODING 'UTF8' LC_COLLATE 'en_US.UTF-8'`。
  - MySQL：`utf8mb4` + `utf8mb4_0900_ai_ci`（5.7 用 `utf8mb4_unicode_ci`）。
  - Oracle：`AL32UTF8`。
  - SQL Server：列级使用 `NVARCHAR`（隐含 UTF-16）；MSSQL 2019+ 可用 `_UTF8` collation。
  - DM：`UTF8`。
- 排序规则不在 DDL 中显式声明（避免方言差异），由数据库实例级配置统一。

### 6.7 多数据库验收清单（CI 必跑）

每个 Epic 完成时强制：
1. PG：单元 + 集成 + E2E 全跑。
2. MySQL：集成 + E2E 跑。
3. Oracle / SQL Server / DM：每个 Epic 至少跑一次「初始化 + 登录 + 该 Epic 主路径」。
4. 跨方言一致性测试：同一个 API 在两个方言下结果应一致（DTO 字节对比，不比时间戳/ID）。

---

## 7. 认证与权限

### 7.1 JWT（access + refresh）

- 算法：HS256（MVP）/ RS256（上线前切）。秘钥从环境变量注入，禁止硬编码。
- access token：15 分钟，载荷 `{ uid, username, tenantId, iat, exp, jti }`，**不放权限码**（避免 token 膨胀；权限走服务端缓存）。
- refresh token：7 天，独立签发，存 `sys_user_token` 表（jti 黑名单可撤销），单设备登录可强制踢出。
- 登录端点：`POST /api/auth/login` → 返回 `{ accessToken, refreshToken, expiresIn }`。
- 刷新端点：`POST /api/auth/refresh`。
- 注销端点：`POST /api/auth/logout` → 把 jti 加入黑名单。

### 7.2 Spring Security 6 配置要点

```java
@Bean
SecurityFilterChain filter(HttpSecurity http, JwtAuthFilter jwt) {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/auth/**", "/api/public/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(e -> e
            .authenticationEntryPoint(restAuthEntry)
            .accessDeniedHandler(restAccessDenied))
        .build();
}
```

### 7.3 RBAC + 数据权限

- **功能权限**：`User—Role—Permission` 三表，`@HasPermission("EMPLOYEE_CREATE")` 注解 + 切面校验。权限码大写下划线命名 `<MODULE>_<ACTION>`（`EMPLOYEE_CREATE` / `PAYROLL_RUN_APPROVE`）。
- **数据权限**：在 Service 层注入 `DataScopeContext`，由切面根据当前用户的「数据范围」（ALL / SELF_DEPT / SELF_DEPT_AND_SUB / SELF_ONLY / CUSTOM_DEPTS）拼接 SQL where 条件。
- 数据权限实现：MyBatis-Plus 的 `DataPermissionInterceptor` + 自定义 `DataPermissionHandler`，对带 `@DataScope` 的 Mapper 方法自动注入 `dept_id IN (...)`。
- 字段级权限（如薪资敏感字段）：`@Sensitive` 注解 + Jackson Serializer，在响应序列化时按权限脱敏。

### 7.4 密码与凭据

- 密码：BCrypt（强度 10）。
- 重置：管理员重置生成临时密码，首次登录强制改密。
- 失败锁定：5 次失败锁 15 分钟（`sys_user.locked_until`）。
- 双因素 / SSO：MVP 不做，留接口扩展点。

---

## 8. 审批流（轻量自研）

见 ADR-003。MVP 用 4 张表实现：

```
approval_definition           # 流程定义（流程编码 + 节点列表 JSON）
approval_instance             # 流程实例（关联业务单据 ref_type / ref_id）
approval_task                 # 待办任务（assignee_id, status, seq, due_at）
approval_history              # 审批历史（每次 approve/reject 都写一条）
```

### 8.1 节点描述

`approval_definition.nodes` 字段为 JSON 字符串（统一 String 存储），结构：
```json
[
  { "seq": 1, "name": "直属上级", "assigneeRule": "EMPLOYEE_MANAGER", "type": "APPROVE" },
  { "seq": 2, "name": "HR 审批", "assigneeRule": "ROLE:HR_OFFICER", "type": "APPROVE" }
]
```

`assigneeRule` MVP 支持：
- `EMPLOYEE_MANAGER`：取发起人所在部门负责人
- `EMPLOYEE_DEPT_HEAD`：取发起人所在部门 head_id
- `ROLE:<ROLE_CODE>`：所有具备该角色的用户均可处理（先到先得）
- `USER:<USER_ID>`：固定指派

### 8.2 流程实例运行

- 启动：`ApprovalService.start(definitionCode, refType, refId, initiator)` → 写 `approval_instance` + 第 1 个 `approval_task`。
- 处理：`ApprovalService.approve(taskId, comment)` / `reject(taskId, comment)` → 写 `approval_history`，推进 `approval_instance.current_seq`，若到末节点则置 `APPROVED` 并回调业务（事件总线 `ApplicationEventPublisher` + `ApprovalCompletedEvent`）。
- 撤回：发起人可在第一个节点未处理时撤回 → 实例置 `WITHDRAWN`，业务回调。
- 抄送、会签、跳转、并行：MVP 不做，留扩展点（`approval_task.parallel_group` 字段已预留）。

### 8.3 与业务模块的契约

业务模块通过监听 `ApprovalCompletedEvent`(`refType`, `refId`, `outcome`) 处理回调，业务模块**不依赖**审批模块的内部状态。

```java
@TransactionalEventListener
public void onApproved(ApprovalCompletedEvent e) {
    if (!"LEAVE_APPLICATION".equals(e.getRefType())) return;
    leaveApplicationService.onApproved(e.getRefId(), e.getOutcome());
}
```

---

## 9. 横切关注点

### 9.1 全局异常处理

`hrms-common` 提供 `@RestControllerAdvice GlobalExceptionHandler`，捕获：
- `BusinessException`（业务异常，error code + message）
- `MethodArgumentNotValidException` / `ConstraintViolationException`（参数校验）
- `AccessDeniedException` / `AuthenticationException`
- `Exception` 兜底

统一返回：
```json
{ "code": "EMP_001", "message": "员工编号已存在", "data": null, "traceId": "..." }
```

### 9.2 统一返回 `R<T>`

```java
public record R<T>(String code, String message, T data, String traceId) {
    public static <T> R<T> ok(T data) { return new R<>("0", "ok", data, MDC.get("traceId")); }
    public static <T> R<T> fail(String code, String msg) { return new R<>(code, msg, null, MDC.get("traceId")); }
}
```

Controller 直接返回 `R.ok(...)`；HTTP 状态码语义保留（404 / 403 / 401 用 ResponseEntity）。

### 9.3 操作日志切面

- 注解 `@AuditLog(action = "EMPLOYEE_CREATE", refType = "EMPLOYEE")` 标在 Controller 方法上。
- 切面在事务提交后异步写 `sys_audit_log`：操作人 / IP / UA / 入参摘要 / 出参摘要 / 耗时 / traceId。
- 入参中的敏感字段（`@Sensitive`）自动掩码。

### 9.4 敏感字段加密（AES-256-GCM）

- 注解 `@Encrypted` + `EncryptedTypeHandler`：MyBatis 写库时加密、读库时解密。
- 适用字段：`employee.id_card`、`employee_bank_account.account_no`、`payslip.salary_*`（薪资字段保留另一种「按权限脱敏」方式）。
- 密钥从环境变量 `HRMS_AES_KEY` 注入；轮换计划留扩展点。

### 9.5 文件上传

- `POST /api/files/upload` → 校验类型 / 大小 → 计算 SHA-256 → 存 `data/uploads/<yyyy>/<MM>/<sha256>.<ext>` → 返回 `fileId`。
- 业务表只存 `fileId`（关联 `sys_attachment`），不存路径。
- 下载走 `GET /api/files/{fileId}`，鉴权 + 防越权（关联 refType / refId 校验）。

### 9.6 字典

- `sys_dict` + `sys_dict_item` 两表，按 `dict_type` 分组。
- 启动时 Caffeine 全量缓存，更新时发本地事件刷新。
- 前端通过 `GET /api/dict/{type}` 拉取并缓存到 Pinia。

### 9.7 i18n 预留

- `messages_zh_CN.properties`（默认）、`messages_en.properties`。
- `Accept-Language` 头解析 → `LocaleContextHolder`。
- 错误码消息走 MessageSource：`messages.getMessage("EMP_001", args, locale)`。
- MVP 中文为主，但所有字面量从代码层面就走 MessageSource，避免后期返工。

---

## 10. 测试金字塔

```
                ┌─────────────┐
                │     E2E     │  ~ 每 Epic 1-3 条主路径
                │  Playwright │  覆盖度低、信号高
                ├─────────────┤
                │   接口/集成  │  ~ 每 Service 主要方法
                │ Spring Boot │  Testcontainers 起 PG/MySQL
                │   Test      │
                ├─────────────┤
                │    单元     │  ~ 每 Service / 工具类
                │ JUnit 5 +   │  覆盖率门槛 60%
                │  Mockito    │
                └─────────────┘
```

### 10.1 单元测试

- 框架：JUnit 5 + Mockito + AssertJ。
- 覆盖：Service 层业务规则、工具类、DTO 转换。
- 不连数据库；外部依赖一律 mock。
- 覆盖率门槛 60%（JaCoCo）；Service 层关键方法 ≥ 80%。

### 10.2 接口测试（集成测试）

- 框架：Spring Boot Test + Testcontainers + RestAssured / MockMvc。
- 启动方式：`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Testcontainers`。
- **多方言**：基类 `AbstractIntegrationTest` 提供 `@PostgresTest` / `@MySqlTest` 注解，可通过参数化跑两套。
- 数据准备：`@Sql` 或 Testcontainers 的 init script，禁止依赖前一个测试的副作用。

### 10.3 E2E（端到端）

- 框架：Playwright（TS）。
- 启动：`docker-compose up -d`（含 PG + 应用 + 前端） → `playwright test`。
- 用例：每个 Epic 至少 1 条主路径（如 Epic 02 的「请假申请 → 审批 → 余额扣减」）。
- Trace 录像：失败时自动保留，CI 工件上传。

### 10.4 测试数据策略

- 种子数据：`db/seed/` 下按 Epic 分文件，由 Liquibase `runOnChange=true` 加载（dev profile）。
- 测试隔离：每个 `@Test` 方法用 `@Transactional` + 回滚（单元/集成）；E2E 用独立数据库实例。

---

## 11. 部署架构

### 11.1 docker-compose 一键启动（dev / demo）

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: hrms
      POSTGRES_USER: hrms
      POSTGRES_PASSWORD: hrms
    ports: [ "5432:5432" ]
    volumes: [ "pgdata:/var/lib/postgresql/data" ]

  hrms-app:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/hrms
      SPRING_DATASOURCE_USERNAME: hrms
      SPRING_DATASOURCE_PASSWORD: hrms
      HRMS_AES_KEY: ${HRMS_AES_KEY}
    ports: [ "8080:8080" ]
    depends_on: [ postgres ]

  hrms-frontend:
    build: ./frontend
    ports: [ "80:80" ]
    depends_on: [ hrms-app ]

volumes:
  pgdata:
```

- 演示日切 MySQL：复制一份 `docker-compose.mysql.yml`，启动时 `docker compose -f ... up`。
- 生产架构（非 MVP 范围，留扩展点）：K8s + 外部 RDS + S3 + Redis + Nginx Ingress。

### 11.2 配置外置

- 12-Factor：所有可变配置走环境变量；`application-{profile}.yml` 只放占位符。
- 敏感配置（DB 密码、AES key、JWT secret）：MVP 用 `.env` 文件 + docker-compose 注入；生产走 KMS / Vault。

### 11.3 数据迁移

- 升级：CI 构建镜像 → docker-compose pull → restart；Liquibase 自动跑增量 changeSet。
- 回滚：每个 changeSet 必须能回滚（`<rollback>` 或对应 `down` 操作），由 reviewer 在 PR 中检查。

---

## 12. 可观测性

### 12.1 日志

- Logback + logstash-logback-encoder，输出 JSON 到 stdout。
- 每条日志带 `traceId / spanId / userId / requestUri / httpStatus / costMs`。
- `traceId` 由 `OncePerRequestFilter` 在请求入口生成（UUID 短码），写入 MDC，响应头返回。
- 敏感信息禁入日志：身份证 / 银行卡 / 密码 / 薪资明细均掩码。

### 12.2 健康检查与指标

- Actuator 端点：`/actuator/health`、`/actuator/info`、`/actuator/metrics`、`/actuator/prometheus`（预留）。
- 健康检查检查项：DB / 磁盘 / 自定义 `LiquibaseHealthIndicator`（验证最新 changeSet 已应用）。
- 自定义业务指标（Micrometer）：`hrms_login_total{status="success|fail"}`、`hrms_payroll_run_duration_seconds`、`hrms_approval_pending_total`。

### 12.3 链路追踪（预留）

- MVP 不接 Jaeger / Zipkin，仅在日志中记 traceId；
- 留 Spring Cloud Sleuth / Micrometer Tracing 接入点，未来切微服务时启用。

---

## 13. CI 矩阵与发布流水线

### 13.1 CI 阶段

```
PR 触发：
  ├─ lint (Spotless + Checkstyle + ESLint + Stylelint)
  ├─ build (Maven + npm)
  ├─ unit (JUnit + Vitest)            ← 必须通过
  ├─ integration:postgres             ← 必须通过
  ├─ integration:mysql                ← 必须通过（每个 PR 都跑）
  └─ e2e:postgres                     ← 必须通过

main 合并后：
  ├─ image:build & push (backend / frontend)
  ├─ integration:oracle               ← 抽测，每周一次
  ├─ integration:sqlserver            ← 抽测
  ├─ integration:dm                   ← 抽测
  └─ deploy:demo (docker-compose 拉取最新镜像)

发布前（Sprint 5 或上线前）：
  └─ 全矩阵：5 方言 × 全部 E2E
```

### 13.2 阶段对应人

- lint + unit：研发自测必须通过才提 PR。
- integration + e2e：CI 自动跑，QA 复核失败用例。
- 抽测矩阵：DBA 兼 dev-db 周期性回归。
- 发布门禁：reviewer 出 P0/P1 清零审计报告 + qa-lead 回归报告 + PM 演示通过 → 发布。

---

## 附录 A：架构演进路线（MVP → 正式产品）

| 阶段 | 关键变化 | 对应 Epic |
|---|---|---|
| MVP | 单体 + 单实例 + PG 默认 | EP-01 ~ EP-06 |
| 准生产 | 引入 Redis（字典/权限缓存）+ JWT RS256 + S3 附件 | 收尾 + 生产化 |
| 微服务化 | hrms-payroll / hrms-attendance 独立部署，引入 Spring Cloud Gateway + Nacos | 后续 |
| 多租户 | 加 tenant_id + 数据权限拦截器 + Liquibase 多 schema | 后续 |
| 规模化 | 分库分表（ShardingSphere）+ Flink 实时报表 | 远期 |

## 附录 B：与 paiban 项目的边界

- HRMS 与 paiban（Next.js 工程）**不互通**、不复用代码、不共享数据库。
- HRMS 工程独立放在 `/Users/apple/hrms/`，技术栈完全独立。
- 后续若需打通，通过 RESTful API 互调，不做代码层耦合。

---

## 文档维护

- 本文档由 `architect` 维护，每个 Epic 完成后回头补充实战发现。
- 重大架构变更必须新增 ADR（`.bmad/decisions.md`）。
- 与 PRD 冲突时由 `pm` 召开三方（pm / product / architect）评审。
