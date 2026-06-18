# HRMS — 人力资源管理系统（MVP 演示原型）

对标 SAP HCM 与 Oracle PeopleSoft HCM 核心域，按 BMAD（Breakthrough Method for Agile AI-Driven Development）方法论协作开发。

## 当前状态
**Sprint 0 · Planning Phase**——产品 PRD / 技术架构 / 数据模型 / 测试策略 / 审核清单 撰写中。代码尚未开始。

## 技术栈
- 后端：Spring Boot 3.x（JDK 17）+ MyBatis-Plus + Spring Security + Liquibase
- 前端：Vue 3 + Vite + TypeScript + Element Plus + Pinia
- 多数据库：PostgreSQL（首选）/ MySQL 8 / Oracle / SQL Server / DM 达梦
- API：RESTful + OpenAPI 3
- 测试：JUnit 5 + Mockito + Testcontainers + Playwright

## 目录约定
```
hrms/
├─ docs/
│  ├─ prd.md                  # 产品 PRD
│  ├─ architecture.md         # 技术架构
│  ├─ data-model.md           # 域模型 ER
│  ├─ test-strategy.md        # 测试策略
│  ├─ review-checklist.md     # 代码审核清单
│  ├─ epics/                  # Epic 文档
│  └─ stories/                # 自包含 Story 文件
├─ backend/                   # Spring Boot 工程（Sprint 1 起）
├─ frontend/                  # Vue 3 工程（Sprint 1 起）
├─ db/migration/              # Liquibase changelog
└─ .bmad/
   ├─ status.md               # PM 项目台账
   └─ decisions.md            # ADR 架构决策记录
```

## MVP 六大 Epic
1. 组织与员工主数据（对标 SAP OM+PA）
2. 考勤与假期（对标 SAP PT）
3. 薪酬基础（对标 SAP PY，简化版）
4. 招聘与入职（对标 SAP Recruitment）
5. 绩效（对标 SAP PM，骨架）
6. ESS/MSS 自助服务

详见 `docs/prd.md`。

## Sprint 1 状态
EP01-S01（工程骨架 + 登录认证 + Liquibase 主入口）已交付。代码位于 `backend/`，详见 `docs/stories/EP01-S01-bootstrap-auth-skeleton.md`。

### 数据库 profile 切换
默认 profile 为 `pg`（PostgreSQL）。多数据库切换通过环境变量 `SPRING_PROFILES_ACTIVE`：

| 命令 | 行为 |
|---|---|
| `./mvnw spring-boot:run` | 默认走 `pg` profile（PostgreSQL） |
| `SPRING_PROFILES_ACTIVE=mysql ./mvnw spring-boot:run` | 切到 MySQL 8，Liquibase 用同 changelog 初始化 |
| `SPRING_PROFILES_ACTIVE=h2 ./mvnw spring-boot:run` | 本机无 PG/MySQL 时用 H2 内存库（PostgreSQL 方言模式）冒烟验证 |

各 profile 数据库连接信息通过 `HRMS_DB_URL` / `HRMS_DB_USERNAME` / `HRMS_DB_PASSWORD` 注入；Liquibase 会按 `application-{profile}.yml` 中指定的 `default-schema` 落表。`JWT_SECRET` 环境变量必填且 ≥32 字节，否则启动 fail-fast。

### 种子管理员
首次启动后，admin 账号的密码哈希由 `AdminBootstrapRunner` 写为 `BCrypt('Admin@2026')`，默认账号 `admin / Admin@2026`。正式使用前请立即改密。

## 项目计划
详见 `/Users/apple/.claude/plans/graceful-splashing-kitten.md`。
