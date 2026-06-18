# HRMS Security Fix Report

Generated: 2026-06-18

---

## 【P0-1】AesUtil.java — AES密钥硬编码

**文件**: `backend/hrms-common/src/main/java/com/hrms/common/util/AesUtil.java` (lines 22-38)

**问题**: AES-256 密钥 `DEV_KEY` 硬编码在源码中，环境变量缺失时自动回退到硬编码密钥。

**修复**: 删除 `DEV_KEY` 常量，启动时强制要求 `HRMS_AES_KEY` 环境变量，缺失或长度不为 32 字节时抛出 `IllegalStateException`。

**修复前**:
```java
private static final String DEV_KEY = "hrms-aes-dev-key-2024!@#$%^&*()!";

static {
    String envKey = System.getenv("HRMS_AES_KEY");
    byte[] keyBytes = (envKey != null && !envKey.isBlank())
            ? envKey.getBytes(StandardCharsets.UTF_8)
            : DEV_KEY.getBytes(StandardCharsets.UTF_8);
    KEY = new SecretKeySpec(keyBytes, "AES");
}
```

**修复后**:
```java
private static final int KEY_LENGTH_BYTES = 32;

static {
    String envKey = System.getenv("HRMS_AES_KEY");
    if (envKey == null || envKey.isBlank()) {
        throw new IllegalStateException(
                "AES key is not configured. Set the HRMS_AES_KEY environment variable (must be exactly 32 bytes).");
    }
    byte[] keyBytes = envKey.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length != KEY_LENGTH_BYTES) {
        throw new IllegalStateException(
                "HRMS_AES_KEY must be exactly 32 bytes, but was " + keyBytes.length + " bytes.");
    }
    KEY = new SecretKeySpec(keyBytes, "AES");
}
```

---

## 【P0-2】OfferService.java — 硬编码默认密码

**文件**: `backend/hrms-common/src/main/java/com/hrms/common/recruit/service/OfferService.java` (line 117)

**问题**: 用户账户创建时使用硬编码的 BCrypt 哈希 `$2a$10$defaultHashPlaceholder`，所有新员工共享同一密码。

**修复**: 使用 `SecureRandom` 生成 16 字符随机临时密码，BCrypt 编码后存库，并设置 `passwordChangedAt=null` 标记首次登录必须改密。

**修复前**:
```java
user.setPasswordHash("$2a$10$defaultHashPlaceholder"); // Will be reset on first login
userMapper.insert(user);
```

**修复后**:
```java
String tempPassword = generateTempPassword();
PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
user.setPasswordHash(passwordEncoder.encode(tempPassword));
user.setPasswordChangedAt(null); // force password change on first login
userMapper.insert(user);
```

新增辅助方法:
```java
private static String generateTempPassword() {
    final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder(16);
    for (int i = 0; i < 16; i++) {
        sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
    }
    return sb.toString();
}
```

---

## 【P0-3】HasPermissionAspect.java — Admin ID 硬编码绕过

**文件**: `backend/hrms-common/src/main/java/com/hrms/common/rbac/aspect/HasPermissionAspect.java` (line 30)

**问题**: `ADMIN_USER_ID = 1L` 硬编码，uid=1 的用户无条件绕过所有权限检查，即使数据库中未配置任何权限。

**修复**: 删除硬编码的管理员 ID 和短路逻辑，所有用户（包括管理员）统一走 `permissionService.userHasPermission()` 检查。管理员通过数据库配置 `*` 通配符权限获得超级权限。

**修复前**:
```java
private static final Long ADMIN_USER_ID = 1L;

// Admin bypasses permission checks
if (ADMIN_USER_ID.equals(loginUser.getUid())) {
    return joinPoint.proceed();
}

if (!permissionService.userHasPermission(loginUser.getUid(), required)) {
```

**修复后**:
```java
// All permission checks (including super-admin via "*") go through the service
if (!permissionService.userHasPermission(loginUser.getUid(), required)) {
```

---

## 【P1-1】SecurityConfig.java — CORS 全局禁用

**文件**: `backend/hrms-common/src/main/java/com/hrms/common/security/SecurityConfig.java` (line 43)

**问题**: `.cors(AbstractHttpConfigurer::disable)` 完全禁用 CORS，浏览器会允许任意来源的跨域请求。

**修复**: 替换为配置 CORS 白名单，从 `application.yml` 的 `hrms.cors.allowed-origins` 属性读取允许的源（默认 `http://localhost:3000`）。

**修复前**:
```java
.cors(AbstractHttpConfigurer::disable)
```

**修复后**:
```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

新增 Bean:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}

@Value("${hrms.cors.allowed-origins:http://localhost:3000}")
private List<String> allowedOrigins;
```

---

## 【P1-2】Swagger 生产环境默认启用

**文件**: `backend/hrms-app/src/main/resources/application.yml` (lines 38-41)

**问题**: `springdoc.api-docs.enabled` 和 `swagger-ui.enabled` 均为 `true`，生产环境暴露 API 文档。

**修复**: 改为 `${SWAGGER_ENABLED:false}`，默认关闭。新建 `application-dev.yml` 在开发环境覆盖为 `enabled: true`。

**修复前**:
```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

**修复后**:
```yaml
springdoc:
  api-docs:
    enabled: ${SWAGGER_ENABLED:false}
  swagger-ui:
    enabled: ${SWAGGER_ENABLED:false}
```

新建 `application-dev.yml`:
```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

---

## 【P1-3】GlobalExceptionHandler — HTTP 200 处理 401/403

**文件**: `backend/hrms-common/src/main/java/com/hrms/common/exception/GlobalExceptionHandler.java` (lines 56-66)

**问题**: `AuthenticationException` 和 `AccessDeniedException` 均返回 HTTP 200 + R.code，导致前端/监控无法通过 HTTP 状态码识别认证失败。

**修复**: `AuthenticationException` 返回 HTTP 401，`AccessDeniedException` 返回 HTTP 403，其他业务异常保持 HTTP 200 + R.code 不变。

**修复前**:
```java
@ExceptionHandler(AuthenticationException.class)
public R<Void> handleAuth(AuthenticationException ex) {
    return R.fail(BizCode.UNAUTHORIZED, "未登录");
}

@ExceptionHandler(AccessDeniedException.class)
public R<Void> handleAccessDenied(AccessDeniedException ex) {
    return R.fail(BizCode.FORBIDDEN, "无权限");
}
```

**修复后**:
```java
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<R<Void>> handleAuth(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(R.fail(BizCode.UNAUTHORIZED, "未登录"));
}

@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(R.fail(BizCode.FORBIDDEN, "无权限"));
}
```

---

## 【P1-4】PermissionService — 通配符逻辑边界 Bug

**文件**: `backend/hrms-common/src/main/java/com/hrms/common/rbac/service/PermissionService.java` (lines 58-68)

**问题**: `userHasPermission()` 中 `permissionCode.lastIndexOf(':')` 对不含冒号的权限码（如 `"dashboard"`）返回 `-1`，导致 `substring(0, 0)` 产生空前缀，`"*"` 全局通配符无法正确匹配。

**修复**: 先检查 `*` 全局通配符，再对含冒号的权限码检查前缀通配符。

**修复前**:
```java
// wildcard: "foo:*" or "*"
String prefix = permissionCode.substring(0, permissionCode.lastIndexOf(':') + 1);
if (codes.contains(prefix + "*")) {
    return true;
}
return codes.contains("*");
```

**修复后**:
```java
// global wildcard — handles codes both with and without colons
if (codes.contains("*")) {
    return true;
}
// prefix wildcard: "foo:*" matches "foo:bar", "foo:baz:qux", etc.
int lastColon = permissionCode.lastIndexOf(':');
if (lastColon >= 0) {
    String prefix = permissionCode.substring(0, lastColon + 1);
    if (codes.contains(prefix + "*")) {
        return true;
    }
}
return false;
```

---

## 修改文件汇总

| # | 文件 | 修改类型 |
|---|------|----------|
| 1 | `util/AesUtil.java` | 删除硬编码密钥，强制环境变量 |
| 2 | `recruit/service/OfferService.java` | 删除硬编码密码，随机生成 + BCrypt |
| 3 | `rbac/aspect/HasPermissionAspect.java` | 删除 Admin ID 硬编码 |
| 4 | `security/SecurityConfig.java` | CORS 白名单配置 |
| 5 | `application.yml` | Swagger 默认关闭 |
| 6 | `application-dev.yml` (新建) | 开发环境启用 Swagger |
| 7 | `exception/GlobalExceptionHandler.java` | 正确 HTTP 状态码 |
| 8 | `rbac/service/PermissionService.java` | 通配符匹配逻辑修复 |
