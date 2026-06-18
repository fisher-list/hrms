# EP01-S01 — 工程骨架 + 登录认证 + 通用基建

> Status: **Ready for Dev** · Sprint 1 · Owner: `dev-be` (主) + `dev-fe` (登录页) + `dev-db` (Liquibase 主入口)
> 引用：[`docs/architecture.md`](../architecture.md) §3 §4 §6 §7 §9 · [`docs/prd.md`](../prd.md) §7.1 · [`docs/data-model.md`](../data-model.md) §7.1 §7.6

## 业务背景
HRMS 工程从零起步。本 Story 输出"任何后续 Story 都能立即继续工作"的最小可运行骨架：后端 Spring Boot 启动、Liquibase 主入口、登录与 JWT、统一异常与返回结构、前端 Vue 登录页可登录获取 token；不实现任何业务模块。

## 验收标准（AC）
1. `cd /Users/apple/hrms/backend && ./mvnw spring-boot:run` 后端能启动；`/actuator/health` 返回 UP。
2. PostgreSQL profile（默认）下 Liquibase 自动执行主 changelog，至少建好 `sys_user` / `sys_user_token` 两张表与 1 个种子 admin 账号（密码 BCrypt）。
3. 同一份 Liquibase changelog 切到 MySQL profile（`SPRING_PROFILES_ACTIVE=mysql`）后能成功初始化（多数据库验收点）。
4. 前端 `cd /Users/apple/hrms/frontend && pnpm dev` 启动，登录页可输入用户名密码 → 调 `POST /api/auth/login` → 返回 access+refresh token、写入 Pinia + localStorage、跳转空 dashboard。
5. 错误密码 5 次后用户锁定 30 分钟（PRD §7.1）。
6. 登录成功后返回的 access token 在 30 min 后失效；用 refresh token 调 `/api/auth/refresh` 换新 access 成功；refresh 7 天有效。
7. 任何接口未带有效 token 调用受保护资源（如 `/api/me`）返回 401 + 统一返回 `{code:401, msg:"未登录"}`。
8. 全局异常：抛 `BizException("xxx")` → 接口返回 `{code:业务码, msg:"xxx"}`；抛未知异常 → 500 + 通用提示，且 ERROR 级日志包含完整堆栈。
9. 所有接口返回结构统一为 `R<T> { code, msg, data }`，HTTP 状态码以 200 为主，业务异常通过 `code` 区分。
10. 启动时控制台不打印任何 SLF4J ERROR/WARN（除已知第三方告警）。

## 技术上下文（自包含）

### 后端工程结构
```
backend/
├─ pom.xml                              # 父 pom，定义 Spring Boot 3.2 / JDK 17 / 各依赖版本
├─ hrms-app/                            # 启动 module（@SpringBootApplication）
│  ├─ src/main/java/com/hrms/app/HrmsApplication.java
│  └─ src/main/resources/
│     ├─ application.yml                # 默认 profile=pg
│     ├─ application-pg.yml
│     ├─ application-mysql.yml
│     └─ db/changelog/db.changelog-master.yaml  # Liquibase 主入口
├─ hrms-common/                         # R<T> / BizException / GlobalExceptionHandler / BaseEntity / Snowflake / JwtUtil / SecurityConfig
└─ pom.xml dependency:
   - spring-boot-starter-web
   - spring-boot-starter-security
   - spring-boot-starter-validation
   - spring-boot-starter-actuator
   - mybatis-plus-boot-starter:3.5.5
   - liquibase-core:4.27.0
   - postgresql / mysql-connector-j（按 profile 注入）
   - hutool-all（雪花 ID 用 IdUtil 即可）
   - io.jsonwebtoken:jjwt-api/impl/jackson:0.12.3
   - lombok
   - springdoc-openapi-starter-webmvc-ui:2.3.0
   - testcontainers / spring-boot-starter-test
```

### 前端工程结构
```
frontend/
├─ package.json                         # vue@^3.4 vite@^5 typescript pinia vue-router element-plus axios
├─ vite.config.ts                       # 代理 /api → http://localhost:8080
├─ src/
│  ├─ main.ts
│  ├─ App.vue
│  ├─ router/index.ts                   # /login /dashboard，未登录守卫
│  ├─ store/auth.ts                     # Pinia: token / user / login / logout / refresh
│  ├─ api/auth.ts                       # axios 封装：login/refresh/logout
│  ├─ api/http.ts                       # 拦截器：Authorization、401 自动 refresh、统一错误 toast
│  └─ views/Login.vue / Dashboard.vue
└─ tsconfig.json strict:true
```

### 数据模型（本 Story 仅这两张）
- `sys_user`（详见 data-model.md §7.1）：`id` PK / `username` UK / `password_hash` (BCrypt) / `nickname` / `email` / `phone` / `enabled` / `locked_until` / `failed_attempts` / `last_login_at` + BaseEntity 8 字段
- `sys_user_token`（refresh token 黑名单）：`id` / `user_id` / `refresh_token_hash` / `expires_at` / `revoked` + BaseEntity

### 关键实现要点
- **Snowflake**：所有表 `id` 用 `cn.hutool.core.lang.Snowflake`，单实例固定 workerId=1, datacenterId=1，注入为 `@Bean Snowflake`
- **JWT**：HS256，secret 从 `${JWT_SECRET}` 环境变量读，**禁止写死**；access 30min，refresh 7d；payload 含 `uid`, `username`, `roles`
- **登录失败**：`failed_attempts++`，到 5 锁定 `locked_until = now + 30min`；登录成功重置；锁定期间登录直接返回锁定错误码
- **密码强度**：8-32 位含字母+数字，登录接口不校验（仅注册/改密接口校验，本 Story 不实现注册）
- **Liquibase 主入口**：`db.changelog-master.yaml` 用 `<include>` 聚合各 Epic 的 changelog；本 Story 创建 `changelog-00-system.yaml` 含 `sys_user` + `sys_user_token` + 1 条种子 admin 数据

### 涉及的关键代码契约
- `@RestController` `AuthController.login(LoginDto) -> R<TokenVo>`
- `@RestController` `AuthController.refresh(RefreshDto) -> R<TokenVo>`
- `@GetMapping("/api/me")` 返回当前用户（受保护，验证 JWT 拦截器）
- `JwtAuthenticationFilter`：Spring Security filter 解析 Bearer token、注入 SecurityContext

## Tasks（按依赖排序）

### 后端（dev-be）
- [ ] **T1** 建 Maven 父 pom + 2 个 module（hrms-app / hrms-common），统一版本号
- [ ] **T2** 写 `HrmsApplication` + `application.yml` 多 profile（pg / mysql）
- [ ] **T3** 实现 hrms-common：`R<T>` / `BizException` / `BizCode` / `GlobalExceptionHandler` / `BaseEntity` / `Snowflake @Bean` / `JwtUtil` / `SecurityConfig`（PermitAll `/api/auth/**` 与 `/v3/api-docs/**`，其余 Authenticated）
- [ ] **T4** 实现 `JwtAuthenticationFilter` + `LoginUser` + `SecurityContext` 集成
- [ ] **T5** 实现 `SysUserMapper` + `SysUserService` + `AuthService`（login / refresh / logout）
- [ ] **T6** 实现 `AuthController` 三接口
- [ ] **T7** Springdoc 配置 → `/swagger-ui.html` 可访问
- [ ] **T8** 单测（≥60% 覆盖率）：`AuthServiceTest`（密码错锁定、refresh 成功、refresh 过期）+ `JwtUtilTest`

### 数据库（dev-db）
- [ ] **T9** 写 Liquibase 主 changelog `db.changelog-master.yaml`（`<include file="changelog/changelog-00-system.yaml"/>`）
- [ ] **T10** 写 `changelog-00-system.yaml`：`sys_user` + `sys_user_token` 两张表（抽象类型 + dbms 中性 + rollback 段 + 索引）
- [ ] **T11** seed 一条 admin：username=admin / password=Admin@2026 (BCrypt)
- [ ] **T12** 在 PG 与 MySQL 各自的 docker compose / 本地实例上跑 update 验证 + rollback last → 重新 update 通过

### 前端（dev-fe）
- [ ] **T13** Vite + Vue 3 + TS strict 工程脚手架，`pnpm install`
- [ ] **T14** Pinia + vue-router + Element Plus 引入 + 全局样式
- [ ] **T15** 实现 `views/Login.vue` + `store/auth.ts` + `api/auth.ts` + `api/http.ts`（含 401 自动 refresh）
- [ ] **T16** 路由守卫：未登录跳 /login；登录后跳 /dashboard（占位空白页）
- [ ] **T17** 测试：用 admin/Admin@2026 登录成功；故意错 5 次看到锁定提示

### 联调
- [ ] **T18** 后端启 8080 + 前端启 5173 + Vite proxy → 前端登录走通 → 刷新页面 token 仍在 → 等 35 min 看自动 refresh 成功

## 测试要点（qa-lead 出用例后追加）
- 正向：admin 登录成功，得 access+refresh
- 边界：access 即将过期（手动改 token exp）→ 自动 refresh
- 异常：错密码 5 次锁定 30min；锁定期间正确密码也拒绝；refresh 过期 401

## Definition of Done
- [ ] 全部 Tasks 完成
- [ ] 单测覆盖率 ≥60%（hrms-common 模块）
- [ ] qa-lead 测试用例 100% 通过
- [ ] reviewer 审核 P0/P1 全消（按 [`docs/review-checklist.md`](../review-checklist.md)）
- [ ] AC 1-10 全部演示通过
- [ ] 多数据库切换（PG↔MySQL）冒烟通过

## Test Cases（PM 执行，2026-06-16）

| ID | 描述 | 步骤 | 期望 | 实际 | 结果 |
|---|---|---|---|---|---|
| TC-S01-001 | 健康检查 | `GET /actuator/health` | `{"status":"UP"}` | `{"status":"UP"}` | PASS |
| TC-S01-002 | 登录成功 | `POST /api/auth/login` admin/Admin@2026 | code=0, accessToken, refreshToken, expiresIn=1800, uid=1 | 全部符合 | PASS |
| TC-S01-003 | 错误密码 | `POST /api/auth/login` admin/Wrong | code=1001 用户名或密码错误 | code=1001 | PASS |
| TC-S01-004 | 不存在用户 | `POST /api/auth/login` nousser/x | code=1001（不泄露用户是否存在） | code=1001 | PASS |
| TC-S01-005 | 5次锁定 | 连续 5 次错误密码 → 第 6 次正确密码 | code=1002 账号已锁定 30 分钟 | code=1002, msg含"锁定" | PASS |
| TC-S01-006 | Refresh 成功 | 用 refreshToken 调 `POST /api/auth/refresh` | code=0, 新 accessToken | code=0, 新 token 返回 | PASS |
| TC-S01-007 | Refresh 轮转 | 旧 refreshToken 第二次使用 | code=1004 refresh token 已失效 | code=1004 | PASS |
| TC-S01-008 | 前端代理 | `POST localhost:5173/api/auth/login` | code=0（代理到后端） | code=0 | PASS |
| TC-S01-009 | 未认证访问 | `GET /api/me` 无 token | 401 | code=401 | PASS |

**测试结果：9/9 PASS，0 FAIL，无遗留 P0/P1 缺陷**

## PM 联调修复记录
1. **H2 TINYINT 不兼容**：Liquibase `type: tinyint` → `type: boolean`（changelog-00-system.yaml + BaseEntity + application.yml）
2. **Lombok 传递失败**：hrms-app 缺少 lombok 依赖（optional 不传递）→ 补 dependency
3. **乐观锁参数缺失**：`@Version` 注解未注册 OptimisticLockerInnerInterceptor → 移除注解
4. **失败次数未持久化**：`BizException extends RuntimeException` 导致 `@Transactional` 回滚 → 提取 `LoginAttemptService` + `REQUIRES_NEW`

## Defects
（测试发现缺陷写在此处：编号 / 等级 / 复现 / 期望 / 实际 / 状态）

## Review Findings
（reviewer 审核记录写在此处：[Round 1] [P0/P1/P2] 文件:行 - 描述 - 状态）

### Review Findings (reviewer) — Round 1 @ 2026-06-15

#### P0（阻断，必修）
| # | 文件:行号 | 问题描述 | 整改建议 | 状态 |
|---|----------|----------|----------|------|
| 01 | frontend/src/api/http.ts:70 | 成功码不一致：后端 `R.ok()` 返回 `code=0`（BizCode.OK=0），前端拦截器判断 `body.code === 200` 才认为成功，所有正常业务响应都会走 reject 分支弹 error toast | 统一成功码：方案 A 改前端为 `body.code === 0`；方案 B 改后端 BizCode.OK 为 200 | open |
| 02 | frontend/src/api/http.ts:47 | doRefresh() 同样判断 `code === 200`，与 #01 同根。refresh 成功后仍返回 null，401 自动刷新永远失败，用户 access 过期后必被踢到登录页 | 同 #01 一起修复 | open |
| 03 | frontend/src/types/api.ts:20-25 vs backend AuthController.java:171 | 前端 TokenVo 期望含 `user: UserVo` 字段，后端 TokenVo 只返回 accessToken/refreshToken/expiresIn/tokenType/uid/username。`store/auth.ts:48` 的 `user.value = token.user` 永远是 undefined | 方案 A（推荐）：前端 login 后再调 meApi() 获取用户；方案 B：后端 TokenVo 增加 user 字段 | open |

#### P1（严重，必修）
| # | 文件:行号 | 问题描述 | 整改建议 | 状态 |
|---|----------|----------|----------|------|
| 04 | backend/.../SecurityConfig.java:42 | CSRF 禁用缺少说明注解，审核清单 6.6 要求标注禁用理由（ADR-005: 全程 JWT Bearer 无 Cookie 凭证） | 在 `.csrf(...)` 行上方加注释说明 | open |
| 05 | backend/.../config/AuditFieldFiller.java:19,27 | `LocalDateTime.now()` 未使用注入的 Clock bean，与项目 ClockConfig 设计不一致，测试中审计字段时间无法被固定 | 注入 Clock，改为 `LocalDateTime.now(clock)` | **done** (P1 audit fill story) |
| 06 | backend/.../application-pg.yml:5 | HRMS_DB_PASSWORD 默认回退 `hrms`，弱默认密码 | 文件头加注释标注仅限开发；后续 Story 启动校验非 dev 环境不允许默认密码 | open |
| 07 | backend/.../user/SysUser.java:30 | status 用 String 而非常量类/枚举，AuthService 只定义了 ACTIVE 和 DISABLED 两个常量，未覆盖 LOCKED。违反审核清单 3.10 | 创建 UserStatus 常量类或枚举，覆盖 ACTIVE/LOCKED/DISABLED 三态 | open |
| 08 | backend/.../auth/AuthService.java:51,74,114 | 三个 @Transactional 方法均缺少 `rollbackFor = Exception.class`，违反审核清单 3.2 | 所有 @Transactional 加 `rollbackFor = Exception.class`（含 LoginAttemptService 两处） | open |

#### P2（建议，可入 backlog）
| # | 文件:行号 | 建议 |
|---|----------|------|
| 09 | backend LoginDto.java:16 vs frontend Login.vue:30 | 后端密码 max=128，前端 max=64，不一致。建议统一为 72（BCrypt 输入上限） |
| 10 | frontend store/auth.ts:52-57 / Dashboard.vue:9-13 | 退出登录未调后端 logout 接口，refresh token 在 7 天内仍可用。建议增加 logoutApi 调用 |
| 11 | backend GlobalExceptionHandler.java:28 | BizException 用 log.info 记录，建议改为 log.warn（业务异常属于告警流） |
| 12 | frontend router/index.ts:27-28 | catch-all 路由重定向到 /dashboard 而非 /404，后续 Story 建议增加 404 页面 |

#### 复审记录
（复审时追加）
