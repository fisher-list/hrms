# HRMS 代码审核报告

> **审核日期**: 2026-06-18  
> **审核范围**: 后端 Spring Boot 3.x + MyBatis-Plus, 前端 Vue 3 + Element Plus, Liquibase 迁移  
> **审核深度**: 逐文件阅读全部核心源码，非表面扫描

---

## 目录

1. [安全审计](#1-安全审计)
2. [架构设计](#2-架构设计)
3. [代码质量](#3-代码质量)
4. [数据库](#4-数据库)
5. [前端代码](#5-前端代码)
6. [测试覆盖](#6-测试覆盖)
7. [性能隐患](#7-性能隐患)
8. [整体评分与改进路线图](#8-整体评分与改进路线图)

---

## 1. 安全审计

### 1.1 【P0】AES 加密密钥硬编码在源码中

**问题描述**: `AesUtil.java` 第26行将开发密钥 `DEV_KEY = "hrms-aes-dev-key-2024!@#$%^&*()!"` 硬编码在源码中。当环境变量 `HRMS_AES_KEY` 未设置时，系统静默回退到这个硬编码密钥。这意味着：  
1. 开发/测试环境如果忘记配置环境变量，会用这个已公开的密钥加密所有敏感数据  
2. 密钥被提交到 Git 历史中，任何有代码访问权的人都能解密所有身份证号、手机号、银行卡号  
3. 没有启动时强制校验，静默降级比报错更危险

**严重等级**: P0 — 生产环境中所有加密字段可被任意持有代码的人解密

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/util/AesUtil.java`

**修复方案**:

```java
// 删除硬编码密钥，启动时强制要求环境变量
static {
    String envKey = System.getenv("HRMS_AES_KEY");
    if (envKey == null || envKey.isBlank()) {
        throw new IllegalStateException(
            "HRMS_AES_KEY environment variable is required. " +
            "Generate one with: openssl rand -base64 32");
    }
    byte[] keyBytes = envKey.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length != 32) {
        throw new IllegalStateException(
            "HRMS_AES_KEY must be exactly 32 bytes (256 bits), got " + keyBytes.length);
    }
    KEY = new SecretKeySpec(keyBytes, "AES");
}
```

同时在 `application.yml` 中添加显式注释，CI/CD 流水线中加入密钥长度校验步骤。

---

### 1.2 【P0】OfferService 硬编码默认密码哈希

**问题描述**: `OfferService.accept()` 第117行为新创建的用户账户设置了一个硬编码的密码哈希 `$2a$10$defaultHashPlaceholder`。这不是一个有效的 BCrypt 哈希，如果用户不主动重置密码就直接尝试登录，行为不可预测。更严重的是，所有通过 Offer 入职的员工共享同一个"密码"，一个泄露全部沦陷。

**严重等级**: P0 — 所有 Offer 入职用户共享同一密码，且该密码已暴露在代码中

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/recruit/service/OfferService.java:117`

**修复方案**:

```java
// 生成随机临时密码，BCrypt 后存库，通过邮件/短信发送给新员工
String tempPassword = generateSecureRandomPassword(12);
user.setPasswordHash(passwordEncoder.encode(tempPassword));
// 标记首次登录必须修改密码
user.setPasswordChangedAt(null); // null = must change on first login

// 发送通知（异步）
notificationService.sendWelcomeEmail(candidate.getEmail(), tempPassword);
```

同时实现 `AdminBootstrapRunner` 中的 `password_changed_at` 检查逻辑：登录时如果 `password_changed_at == null`，强制跳转修改密码页面。

---

### 1.3 【P0】Admin 用户 ID 硬编码绕过权限检查

**问题描述**: `HasPermissionAspect` 第30行 `ADMIN_USER_ID = 1L`，只要用户 ID 为 1 就跳过所有权限检查。这是典型的"后门"设计：  
1. 如果雪花算法生成的 ID 恰好为 1 的用户不是管理员，他将拥有超级权限  
2. 攻击者只需创建 ID 为 1 的账户即可绕过所有 RBAC  
3. 硬编码绕过无法通过审计日志追踪

**严重等级**: P0 — 权限系统形同虚设

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/rbac/aspect/HasPermissionAspect.java:30`

**修复方案**:

```java
// 删除硬编码，改为检查用户是否拥有 "admin" 或 "*" 角色/权限
@Around("@annotation(hasPermission)")
public Object checkPermission(ProceedingJoinPoint joinPoint, HasPermission hasPermission) throws Throwable {
    String required = hasPermission.value();
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
        throw new BizException(BizCode.UNAUTHORIZED, "未登录");
    }
    // 不再有任何硬编码绕过
    if (!permissionService.userHasPermission(loginUser.getUid(), required)) {
        log.warn("user {} denied permission {}", loginUser.getUsername(), required);
        throw new BizException(BizCode.FORBIDDEN, "无权限: " + required);
    }
    return joinPoint.proceed();
}
```

管理员通过在数据库中配置 `*` 通配符权限来获得超级权限，而非硬编码。

---

### 1.4 【P1】CORS 全局禁用 + CSRF 禁用

**问题描述**: `SecurityConfig` 第43行 `.cors(AbstractHttpConfigurer::disable)` 完全禁用了 CORS。虽然系统设计为前后端同源部署，但如果前端通过独立域名/端口访问 API（开发环境、微服务网关），任何网站都能发起跨域请求。配合 CSRF 也被禁用（第42行），攻击者可以构造恶意页面诱导已登录用户执行操作。

**严重等级**: P1 — 跨站请求伪造风险，尤其在 stateful 场景下

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/security/SecurityConfig.java:42-43`

**修复方案**:

```java
.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    // 从配置文件读取允许的源，不要硬编码 "*"
    config.setAllowedOrigins(Arrays.asList(corsProperties.getAllowedOrigins()));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    return config;
}))
// CSRF 保持禁用（JWT 无状态方案不需要），但必须确保 CORS 配置正确
.csrf(AbstractHttpConfigurer::disable)
```

---

### 1.5 【P1】前端 Token 存储在 localStorage

**问题描述**: `auth.ts` 和 `http.ts` 将 access token 和 refresh token 存储在 `localStorage` 中。任何 XSS 漏洞（包括第三方库的 XSS）都能直接读取这些 token。refresh token 的有效期是 7 天，一旦泄露攻击窗口极长。

**严重等级**: P1 — XSS 可直接窃取认证凭证

**涉及文件**:  
`frontend/src/store/auth.ts`, `frontend/src/api/http.ts`

**修复方案**:

```typescript
// 将 refresh token 改为 HttpOnly cookie（后端设置）
// access token 保留在内存中（Pinia state），不持久化到 localStorage
// 刷新页面时通过 /api/auth/refresh（cookie 自动携带）重新获取 access token

// auth.ts 改造
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string>(''); // 仅内存
  // 不再 localStorage.setItem/refreshToken
  // 刷新页面时调用 refresh 接口（后端通过 HttpOnly cookie 读取 refresh token）
});
```

如果无法立即改造为 HttpOnly cookie 方案，至少应将 token 存储在 `sessionStorage`（标签页关闭即清除）并缩短 refresh token 有效期。

---

### 1.6 【P1】DataScopeAspect 是空壳 — 数据隔离形同虚设

**问题描述**: `DataScopeAspect.resolveScopeType()` 方法（第87-91行）直接返回注解的默认值，没有根据用户角色动态解析。所有用户看到的都是 `ALL` 范围数据。这意味着一个普通员工在列表接口上能看到全公司所有人的数据。

**严重等级**: P1 — 行级数据权限完全无效，任何登录用户可查看全量数据

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/rbac/aspect/DataScopeAspect.java`

**修复方案**:

```java
private DataScopeType resolveScopeType(Long userId, DataScopeType annotated) {
    // 如果注解指定了最小范围，使用注解值
    // 否则根据用户最高角色决定
    Set<String> roleCodes = permissionService.getRoleCodes(userId);
    
    if (roleCodes.contains("ADMIN") || roleCodes.contains("HR_MANAGER")) {
        return DataScopeType.ALL;
    }
    if (roleCodes.contains("LINE_MANAGER")) {
        return DataScopeType.SUBORDINATE_TREE;
    }
    return DataScopeType.SELF_ONLY;
}
```

同时需要完善 `ctx.setDeptId()` 的逻辑，从员工表获取当前用户的部门 ID。

---

### 1.7 【P1】Swagger/OpenAPI 在所有环境默认启用

**问题描述**: `application.yml` 第37-42行启用了 Swagger UI 和 API docs。虽然 `SecurityConfig` 将 `/swagger-ui/**` 设为公开访问，但生产环境不应暴露 API 文档。攻击者可以利用 Swagger UI 直接探测和调用所有 API。

**严重等级**: P1 — 生产环境 API 文档泄露

**涉及文件**:  
`backend/hrms-app/src/main/resources/application.yml`

**修复方案**:

```yaml
# application.yml
springdoc:
  api-docs:
    enabled: ${SWAGGER_ENABLED:false}  # 默认关闭
  swagger-ui:
    enabled: ${SWAGGER_ENABLED:false}
    path: /swagger-ui.html

# 创建 application-dev.yml 覆盖
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

---

### 1.8 【P2】全局异常处理器返回 HTTP 200 处理 401/403

**问题描述**: `GlobalExceptionHandler` 对 `AuthenticationException` 和 `AccessDeniedException` 返回 HTTP 200，仅通过 `R.code` 区分。这违反了 RESTful 语义，导致：  
1. CDN/负载均衡器层面无法识别未授权请求并做限流  
2. 安全扫描工具无法正确检测认证漏洞  
3. 浏览器的 HTTP 缓存机制可能缓存"200"的错误响应

**严重等级**: P2 — 安全监控盲区 + REST 语义错误

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/exception/GlobalExceptionHandler.java`

**修复方案**: 至少对 `AuthenticationException` 和 `AccessDeniedException` 返回正确的 HTTP 状态码，或统一在 `RestAuthExceptionHandlers` 中处理（它已经是正确的 HTTP 状态码了，问题在于 `GlobalExceptionHandler` 和它冲突）。

---

### 1.9 【P2】权限通配符逻辑存在边界 Bug

**问题描述**: `PermissionService.userHasPermission()` 第63行的通配符匹配逻辑：当 `permissionCode` 不包含 `:` 时（如 `"*"`），`lastIndexOf(':')` 返回 -1，`substring(0, 0)` 返回空字符串，`codes.contains(":*")` 永远为 false。虽然第67行兜底了 `codes.contains("*")`，但中间的 `foo:*` 匹配逻辑对不含冒号的权限码会产生意外行为。

**严重等级**: P2 — 边界条件下权限判断错误

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/rbac/service/PermissionService.java:63`

**修复方案**:

```java
public boolean userHasPermission(Long userId, String permissionCode) {
    if (userId == null || permissionCode == null) return false;
    Set<String> codes = getPermissionCodes(userId);
    if (codes.contains(permissionCode)) return true;
    // 全局通配符
    if (codes.contains("*")) return true;
    // 模块级通配符: "foo:*" 匹配 "foo:bar", "foo:baz:qux"
    int lastColon = permissionCode.lastIndexOf(':');
    if (lastColon > 0) {
        String prefix = permissionCode.substring(0, lastColon + 1);
        if (codes.contains(prefix + "*")) return true;
    }
    return false;
}
```

---

## 2. 架构设计

### 2.1 【P1】全局统一返回 HTTP 200 — 反模式

**问题描述**: 系统设计为所有 API 返回 HTTP 200，业务错误通过 `R.code` 区分。这是一个反模式（详见 1.8），会导致：  
1. 前端 axios 拦截器需要同时处理 HTTP 状态码和业务码，逻辑复杂  
2. API 网关层无法基于 HTTP 状态码做熔断/限流  
3. APM 工具（如 SkyWalking）无法正确统计错误率  

**修复方案**: 保留 `R<T>` 信封用于业务成功响应，但 HTTP 状态码应正确反映结果。4xx/5xx 错误使用对应 HTTP 状态码 + `R<T>` body。

---

### 2.2 【P2】HrEmployeeService 是 God Class

**问题描述**: `HrEmployeeService` 承载了员工主数据 + 6 张子表的全部 CRUD 逻辑，363 行代码，7 个 Mapper 依赖。`update()` 方法 105 行，`insertSubTables()` 73 行。违反单一职责原则，任何一个子表的变更都要改这个文件。

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/employee/service/HrEmployeeService.java`

**修复方案**: 拆分为 `EmployeeMasterService`（主表 CRUD）和 `EmployeeSubDataService`（子表 CRUD），或为每张子表创建独立 Service。

---

### 2.3 【P2】子表更新采用"全删全插"策略

**问题描述**: `HrEmployeeService.update()` 中对教育经历、工作经历等子表的更新策略是先 `DELETE` 全部旧记录再 `INSERT` 新记录。这会导致：  
1. 子表 ID 不断增长（雪花 ID 浪费）  
2. 如果有外键引用子表记录（如审批历史），删除会破坏引用完整性  
3. 大量子表记录时性能差

**修复方案**: 改用 diff 策略 — 对比新旧列表，仅插入新增、更新变更、删除移除的记录。

---

### 2.4 【P2】缺少统一的 DTO → Entity 转换层

**问题描述**: 所有 Service 直接在方法内部手动将 DTO 字段赋值到 Entity，代码冗长且易出错。例如 `HrEmployeeService.create()` 第92-107行逐字段 set，`OfferService.accept()` 第100-107行又是一套。

**修复方案**: 使用 MapStruct 或手写 Converter 统一转换层。

---

## 3. 代码质量

### 3.1 【P2】EmployeeController.get() 返回 Map — 无类型安全

**问题描述**: `EmployeeController.get()` 返回 `R<Map<String, Object>>`，前端得到的是一个无类型的 Map。子表 key 名硬编码在 Service 中（`"employee"`, `"educations"`, `"workExps"` 等），前后端无法通过类型系统保证一致性。

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/employee/controller/EmployeeController.java:44`

**修复方案**:

```java
public R<EmployeeDetailVo> get(@PathVariable Long id) {
    return R.ok(employeeService.getDetailById(id));
}

// 新建 EmployeeDetailVo
@Data
public class EmployeeDetailVo {
    private HrEmployee employee;
    private List<HrEmployeeEducation> educations;
    private List<HrEmployeeWorkExp> workExps;
    private List<HrEmployeeFamily> family;
    private List<HrEmployeeContract> contracts;
    private List<HrEmployeeBankAccount> bankAccounts;
    private List<HrEmployeeAddress> addresses;
}
```

---

### 3.2 【P2】PayrollService.calculate() 方法 150+ 行

**问题描述**: 单个方法包含：薪资计算、社保计算、个税计算、累计台账更新、异常处理。任何一个逻辑变更都需要理解整个方法。违反"一个方法做一件事"原则。

**修复方案**: 拆分为 `calculateGross()`, `calculateSocialInsurance()`, `calculateIIT()`, `updateCumulativeLedger()` 等私有方法。

---

### 3.3 【P3】`.last("LIMIT 1")` 跨数据库兼容性问题

**问题描述**: `PayrollService` 和 `CompensationService` 中使用 `.last("LIMIT 1")` 来限制查询结果。`LIMIT` 是 MySQL/PostgreSQL 语法，在 SQL Server 中是 `TOP`，在 Oracle 中是 `FETCH FIRST`。虽然当前只支持 PG/MySQL/H2，但 `.last()` 方法绕过了 MyBatis-Plus 的方言抽象。

**涉及文件**:  
`PayrollService.java:115,140`, `CompensationService.java:46`, `CompanyService.java:26`

**修复方案**:

```java
// 使用 MyBatis-Plus 的 Page 来限制结果
compensationMapper.selectOne(
    new LambdaQueryWrapper<PyCompensationMaster>()
        .eq(PyCompensationMaster::getEmployeeId, emp.getId())
        .orderByDesc(PyCompensationMaster::getEffectiveDate)
        .last("LIMIT 1"));  // 如果确认只用 PG/MySQL 可保留，否则改用 Page(1,1)
```

---

### 3.4 【P3】大量 `catch { /* ignore */ }` 静默吞异常

**问题描述**: 前端多个 Vue 组件中出现 `catch { // ignore }` 或 `catch { }` 空 catch 块，例如 `PayrollRuns.vue` 第79、93、123行。虽然 axios 拦截器已经弹出了错误消息，但静默吞掉异常会导致：  
1. 调试困难 — 无法追踪异常传播路径  
2. 如果 axios 拦截器逻辑变更，错误会被完全忽略

**修复方案**: 至少在 catch 中保留一行日志或注释说明为什么可以忽略。

---

### 3.5 【P3】DataScopeContext 使用全限定类名

**问题描述**: `DataScopeContext.java` 第14-18行的字段类型使用了全限定类名 `com.hrms.common.rbac.annotation.DataScopeType` 而非 import。代码可读性差。

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/rbac/datascope/DataScopeContext.java`

---

## 4. 数据库

### 4.1 【P1】PayrollService.calculate() 存在 N+1 查询

**问题描述**: 薪资计算循环中，每个员工执行 2 次额外查询（`compensationMapper.selectOne` + `ledgerMapper.selectOne`）。如果有 10000 名员工，将产生 20000+ 次数据库查询。这是教科书级的 N+1 问题。

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/payroll/service/PayrollService.java:134-243`

**修复方案**:

```java
// 批量预加载补偿数据
List<Long> empIds = employees.stream().map(HrEmployee::getId).collect(Collectors.toList());

// 一次性加载所有员工的最新补偿
Map<Long, PyCompensationMaster> compMap = loadLatestCompensations(empIds);

// 一次性加载当年累计台账
Map<Long, PyCumulativeTaxLedger> ledgerMap = loadLedgers(empIds, year);

for (HrEmployee emp : employees) {
    PyCompensationMaster comp = compMap.get(emp.getId());
    PyCumulativeTaxLedger ledger = ledgerMap.get(emp.getId());
    // ... 计算逻辑不变
}

private Map<Long, PyCompensationMaster> loadLatestCompensations(List<Long> empIds) {
    // 使用 SQL: SELECT * FROM py_compensation_master 
    //   WHERE employee_id IN (...) 
    //   AND (employee_id, effective_date) IN (
    //     SELECT employee_id, MAX(effective_date) FROM ... GROUP BY employee_id
    //   )
    // 或分两步：先查最新日期，再查记录
}
```

---

### 4.2 【P1】PermissionService.loadPermissionCodes() 执行 4 次独立查询

**问题描述**: 每次缓存未命中时，`loadPermissionCodes()` 执行 4 次查询：  
1. `SELECT * FROM sys_user_role WHERE user_id = ?`  
2. `SELECT * FROM sys_role WHERE id IN (...) AND enabled = true`  
3. `SELECT * FROM sys_role_permission WHERE role_id IN (...)`  
4. `SELECT * FROM sys_permission WHERE id IN (...)`

虽然有 Caffeine 缓存（5 分钟 TTL），但缓存穿透时 4 次查询完全可以合并为 1 次 JOIN。

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/rbac/service/PermissionService.java:103-146`

**修复方案**:

```sql
SELECT DISTINCT p.code
FROM sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id AND r.enabled = true
JOIN sys_role_permission rp ON rp.role_id = r.id
JOIN sys_permission p ON p.id = rp.permission_id
WHERE ur.user_id = #{userId}
```

在 Mapper 中定义这个 JOIN 查询，一次搞定。

---

### 4.3 【P2】HrEmployeeService.getById() 执行 7 次独立查询

**问题描述**: 查看员工详情时执行 1 次主表查询 + 6 次子表查询。应该合并为 1-2 次查询。

**涉及文件**:  
`backend/hrms-common/src/main/java/com/hrms/common/employee/service/HrEmployeeService.java:53-74`

**修复方案**: 使用 MyBatis 的 `@Many` 或 XML resultMap 嵌套查询，或者编写一条带 LEFT JOIN 的 SQL。

---

### 4.4 【P2】薪资计算循环中缺少批量插入优化

**问题描述**: `PayrollService.calculate()` 中每个员工的薪资明细都是逐条 `INSERT`。10000 名员工 = 10000 次 INSERT。

**修复方案**:

```java
// 使用 MyBatis-Plus 的 saveBatch 或自定义批量插入
List<PyPayrollDetail> allDetails = new ArrayList<>();
// ... 循环计算，只收集不插入
detailMapper.insertBatch(allDetails);  // 批量插入
```

---

### 4.5 【P2】Liquibase 迁移文件中 datetime vs timestamp 类型不一致

**问题描述**: `changelog-00-system.yaml` 使用 `timestamp` 类型（第20-23行），而 `changelog-04-employee.yaml` 使用 `datetime` 类型（第20-21行）。虽然在大多数数据库中行为相似，但 `timestamp` 有时区转换行为，`datetime` 没有。类型不一致会导致跨数据库迁移时出现问题。

**修复方案**: 统一使用 `timestamp`（推荐）或 `datetime`，并在团队规范中明确。

---

### 4.6 【P3】缺少 sys_user_token 清理策略

**问题描述**: `sys_user_token` 表会无限增长，每次登录/刷新都插入一条记录。没有定时清理过期 token 的机制。

**修复方案**: 添加定时任务，定期清理已过期且已吊销的 token 记录：

```java
@Scheduled(cron = "0 0 3 * * ?") // 每天凌晨 3 点
@Transactional
public void cleanupExpiredTokens() {
    tokenMapper.delete(new LambdaQueryWrapper<SysUserToken>()
        .lt(SysUserToken::getExpiresAt, LocalDateTime.now())
        .eq(SysUserToken::getRevoked, true));
}
```

---

## 5. 前端代码

### 5.1 【P1】Token 存储在 localStorage — XSS 风险

已在安全审计 1.5 节详述。

---

### 5.2 【P2】权限 store 不支持通配符匹配

**问题描述**: 前端 `permission.ts` store 的 `has()` 方法只做精确匹配 (`codes.value.has(code)`)，不支持后端的 `foo:*` 通配符语义。如果后端分配了 `hr:*` 权限，前端路由守卫会认为用户没有 `hr:employee:list` 权限并跳转到 403。

**涉及文件**:  
`frontend/src/store/permission.ts:18`

**修复方案**:

```typescript
function has(code: string): boolean {
  if (codes.value.has(code)) return true;
  // 全局通配符
  if (codes.value.has('*')) return true;
  // 模块级通配符
  const lastColon = code.lastIndexOf(':');
  if (lastColon > 0) {
    const prefix = code.substring(0, lastColon + 1);
    if (codes.value.has(prefix + '*')) return true;
  }
  return false;
}
```

---

### 5.3 【P2】v-permission 指令仅在 mounted 时检查一次

**问题描述**: `v-permission` 指令在 `mounted` 钩子中检查权限并移除元素，但不会响应权限变化。如果用户在页面内切换了角色（管理员操作），已渲染的元素不会更新。

**涉及文件**:  
`frontend/src/directives/permission.ts`

**修复方案**: 改用 `v-if` + computed 组合，或在指令中添加 `updated` 钩子。

---

### 5.4 【P2】axios 拦截器中 401 刷新逻辑与 Pinia store 不同步

**问题描述**: `http.ts` 的 `doRefresh()` 直接操作 `localStorage`（第48-49行），但不更新 Pinia auth store 的 `accessToken` 和 `refreshToken` 状态。这会导致 store 中的 token 值和 localStorage 不一致。

**涉及文件**:  
`frontend/src/api/http.ts:48-49`

**修复方案**: 刷新成功后更新 store：

```typescript
async function doRefresh(): Promise<string | null> {
  // ... 
  if (resp.data.code === 0 && resp.data.data) {
    const { useAuthStore } = await import('@/store/auth');
    const auth = useAuthStore();
    auth.accessToken = resp.data.data.accessToken;
    auth.refreshToken = resp.data.data.refreshToken;
    auth.persist();
    return resp.data.data.accessToken;
  }
}
```

---

### 5.5 【P2】Element Plus 全量引入

**问题描述**: `main.ts` 第3行 `import ElementPlus from 'element-plus'` 全量引入了 Element Plus，包括所有组件和样式。打包体积会增加 500KB+。

**涉及文件**:  
`frontend/src/main.ts`

**修复方案**:

```typescript
// 使用按需引入
import { ElButton, ElTable, ElForm } from 'element-plus';
// 或使用 unplugin-vue-components 自动按需引入
```

---

### 5.6 【P3】EmployeeList 详情弹窗显示原始嵌套对象

**问题描述**: `EmployeeList.vue` 第141-149行的详情弹窗直接遍历 `detail` 对象的所有 key-value，嵌套的子表数据（educations、workExps 等）会被显示为 `[object Object]` 或 `[object Array]`。

**涉及文件**:  
`frontend/src/views/hr/EmployeeList.vue:141-149`

**修复方案**: 设计专用的详情弹窗组件，对每个子表用独立的 `el-table` 展示。

---

## 6. 测试覆盖

### 6.1 【P2】缺少 Controller 层集成测试

**问题描述**: 现有测试全部是 Service 层单元测试（Mockito mock），没有 Controller 层的 `@WebMvcTest` 或 `@SpringBootTest` 集成测试。这意味着：  
1. 请求参数绑定是否正确未验证  
2. `@HasPermission` 注解在实际 AOP 切面中的行为未验证  
3. `@DataScope` 拦截器链未验证  
4. 序列化/反序列化未验证

**修复方案**: 为关键 Controller 添加 `@WebMvcTest` 测试：

```java
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean HrEmployeeService employeeService;
    
    @Test
    void list_withPermission_ok() throws Exception {
        mockMvc.perform(get("/api/hr/employees")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }
    
    @Test
    void list_withoutPermission_403() throws Exception {
        mockMvc.perform(get("/api/hr/employees"))
                .andExpect(status().isForbidden());
    }
}
```

---

### 6.2 【P2】完全没有前端测试

**问题描述**: 前端项目没有配置任何测试框架（无 Vitest/Jest 配置），零测试覆盖。

**修复方案**: 配置 Vitest，至少覆盖：  
1. Pinia store 逻辑（auth、permission）  
2. API 请求封装（http.ts 拦截器逻辑）  
3. 权限指令 `v-permission`  
4. 关键页面的组件测试

---

### 6.3 【P3】AuthServiceTest 中 Mock 的 tokenMapper.selectOne 逻辑粗糙

**问题描述**: `AuthServiceTest` 第110-118行的 `tokenMapper.selectOne` mock 实现是遍历所有 token 返回第一个未吊销的，没有根据查询条件精确匹配。如果有多个 token，测试可能产生误判。

**修复方案**: 改为根据 `LambdaQueryWrapper` 的条件精确匹配。

---

## 7. 性能隐患

### 7.1 【P1】薪资计算的大 O 复杂度问题

已在数据库 4.1 节详述。10000 员工 = 20000+ 查询 + 10000 INSERT，预估耗时 30-60 秒，且在 `@Transactional` 中长时间持有数据库连接。

**修复方案**:  
1. 批量预加载数据（见 4.1）  
2. 批量插入（见 4.4）  
3. 考虑异步处理 + 进度通知（WebSocket/SSE）

---

### 7.2 【P2】权限缓存无法主动失效

**问题描述**: `PermissionService` 使用 Caffeine 缓存（5 分钟 TTL），但管理员修改用户角色/权限后，没有调用 `evictCache()` 来清除缓存。用户最多需要等 5 分钟才能获得新权限。

**修复方案**: 在 `SysRoleService` 的角色/权限变更操作中注入 `PermissionService` 并调用 `evictCache()` 或 `evictAllCache()`。

---

### 7.3 【P3】前端路由守卫每次导航都可能调用 fetchPermissions

**问题描述**: `router/index.ts` 第119行，如果 `permStore.loaded` 为 false，每次路由守卫都会调用 `fetchPermissions()`。虽然正常流程中登录后会立即加载权限，但如果 `/api/me/permissions` 接口返回空（网络抖动），`loaded` 仍会被设为 true（因为 `finally` 块），但 `codes` 为空集合，导致所有需要权限的路由都被拦截到 403。

**修复方案**: 只在成功获取到数据时才设置 `loaded = true`：

```typescript
async function fetchPermissions(): Promise<void> {
  try {
    const list = await getMyPermissions();
    codes.value = new Set(list);
    loaded.value = true;  // 成功才标记
  } catch {
    loaded.value = false;  // 失败保持 false，下次重试
  }
}
```

---

## 8. 整体评分与改进路线图

### 整体评分

| 维度 | 评分 (1-10) | 说明 |
|------|------------|------|
| **安全** | 4/10 | 3 个 P0 漏洞（硬编码密钥/密码/权限后门），DataScope 空壳 |
| **架构** | 7/10 | 分层合理，但存在 God Class 和全删全插反模式 |
| **代码质量** | 7/10 | 命名规范，注释充分，但有大量代码重复 |
| **数据库** | 6/10 | 索引设计合理，但存在严重 N+1 问题 |
| **前端** | 6/10 | Composition API 使用规范，但 token 存储不安全 |
| **测试** | 5/10 | 后端单元测试覆盖核心 Service，但无集成测试和前端测试 |
| **性能** | 5/10 | 缓存策略到位，但批量操作缺失 |

**综合评分: 5.7/10** — 原型阶段可接受，距离生产就绪有明显差距。

---

### 改进路线图

#### Phase 1: 安全加固（1-2 周，阻塞上线）

| 优先级 | 任务 | 预估工时 |
|--------|------|----------|
| P0 | 移除 AES 硬编码密钥，启动时强制校验 | 0.5 天 |
| P0 | 移除 OfferService 硬编码密码，实现临时密码机制 | 1 天 |
| P0 | 移除 Admin ID 硬编码绕过，改为数据库配置 | 0.5 天 |
| P1 | 配置 CORS 白名单 | 0.5 天 |
| P1 | 生产环境禁用 Swagger | 0.5 天 |
| P1 | 实现 DataScope 角色解析逻辑 | 2 天 |
| P1 | Token 存储方案改造（HttpOnly cookie 或 sessionStorage） | 2 天 |

#### Phase 2: 性能优化（1 周）

| 优先级 | 任务 | 预估工时 |
|--------|------|----------|
| P1 | 薪资计算 N+1 → 批量查询 | 1 天 |
| P1 | 权限加载 4 次查询 → 1 次 JOIN | 0.5 天 |
| P2 | 员工详情 7 次查询 → 嵌套查询 | 1 天 |
| P2 | 批量插入优化 | 0.5 天 |
| P2 | 权限缓存主动失效 | 0.5 天 |

#### Phase 3: 代码质量提升（1-2 周）

| 优先级 | 任务 | 预估工时 |
|--------|------|----------|
| P2 | HrEmployeeService 拆分 | 1 天 |
| P2 | 子表更新改为 diff 策略 | 1 天 |
| P2 | 引入 MapStruct 统一 DTO 转换 | 1 天 |
| P2 | EmployeeController 返回类型安全 VO | 0.5 天 |
| P2 | 前端权限 store 通配符支持 | 0.5 天 |
| P2 | 前端 axios 拦截器与 Pinia store 同步 | 0.5 天 |

#### Phase 4: 测试补全（2 周）

| 优先级 | 任务 | 预估工时 |
|--------|------|----------|
| P2 | Controller 层 @WebMvcTest 测试 | 3 天 |
| P2 | 前端 Vitest 配置 + store 测试 | 2 天 |
| P2 | 关键业务流程端到端测试 | 3 天 |
| P3 | Mock 策略优化 | 1 天 |

---

> **结论**: 项目架构设计合理，业务逻辑实现完整，代码规范度高于平均水平。但 3 个 P0 安全漏洞必须在上线前修复，否则等同于"裸奔"。DataScope 空壳和 N+1 查询问题也需要优先解决。建议按上述路线图分阶段推进，在 Phase 1 完成前不要部署到任何可访问的环境。
