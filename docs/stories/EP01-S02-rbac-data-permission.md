# EP01-S02 — RBAC 角色权限 + 数据权限拦截器

> Status: **Ready for Dev** · Sprint 1 · Owner: `dev-be` (主) + `dev-fe` (角色管理页) + `dev-db`
> 依赖：EP01-S01
> 引用：[`docs/architecture.md`](../architecture.md) §7.3 · [`docs/prd.md`](../prd.md) §7.2 §7.3 · [`docs/data-model.md`](../data-model.md) §7.1

## 业务背景
S01 跑通登录后，权限永远是 ALL_PERMIT 不可用。本 Story 落地 RBAC 三表（user/role/permission + 两张关联）+ `@HasPermission` 注解 + 数据权限切面（按部门递归子树过滤），并提供 Admin 角色管理页。

## 验收标准（AC）
1. `sys_role` / `sys_permission` / `sys_user_role` / `sys_role_permission` 4 张表通过 Liquibase 建好；种子 4 个角色：`Admin` / `HR_MANAGER` / `LINE_MANAGER` / `EMPLOYEE`，并按 PRD §3 角色画像分配权限。
2. Controller 方法标注 `@HasPermission("employee:view")` 后，无该权限用户调用接口返回 403 + `{code:403, msg:"无权限"}`。
3. **数据权限切面**：方法标注 `@DataScope(type=SUBORDINATE_TREE, deptField="deptId")` 后，自动在 SQL where 注入 `dept_id IN (子树部门 id 集合)`；Admin 角色绕过、HR_MANAGER 全公司、LINE_MANAGER 看本部门递归子树、EMPLOYEE 仅自己。
4. 未登录调受保护接口 401；已登录无权限调 403——前端拦截器分别跳 `/login` 与 `/403` 页。
5. Admin 可在角色管理页：列出/新建/编辑/删除角色、勾选权限节点（树形）、把角色分配给用户。
6. 删除内置 4 角色任一被拒绝（业务校验）。
7. 用户被分配多个角色时，权限取并集；任一角色被禁用 (`enabled=false`) 不计入。
8. 所有角色/权限变更产生审计日志（who/when/before/after）。

## 技术上下文

### 数据模型（详见 data-model.md §7.1）
- `sys_role`：`id`/`code` UK/`name`/`description`/`builtin`(true 不可删)/`enabled`
- `sys_permission`：`id`/`code` UK（如 `employee:view`）/`name`/`type`(MENU/BUTTON/API)/`parent_id`(树)/`sort_no`
- `sys_user_role`：`user_id`/`role_id`，UK(`user_id,role_id`)
- `sys_role_permission`：`role_id`/`permission_id`，UK(`role_id,permission_id`)

### 数据权限模型
枚举 `DataScopeType`：
- `ALL`（绕过过滤，Admin/HR_MANAGER）
- `OWN_DEPT`（仅本部门，不递归）
- `SUBORDINATE_TREE`（本部门 + 所有递归子部门，LINE_MANAGER 默认）
- `SELF_ONLY`（仅自己 employee_id，EMPLOYEE 默认）

`@DataScope` 注解作用于 Service 方法；切面在 ThreadLocal 注入 `DataScopeContext`，Mapper 用 MyBatis-Plus `InnerInterceptor` 在 SQL 末尾自动追加条件。

### 涉及的关键代码契约
- `@interface HasPermission(String value)` + `HasPermissionAspect`（用 Spring AOP 实现，方法切面）
- `@interface DataScope(DataScopeType type(), String deptField())` + `DataScopeAspect`
- `PermissionService.userHasPermission(userId, code)` 缓存 5 分钟
- `RoleController` / `RoleService` / `RoleMapper` CRUD
- `DepartmentTreeService.getSubordinateDeptIds(userId)` —— 用部门表的 `path` 字段一次 SQL 查出递归子树（注意 path 中性写法，PG/MySQL 用 LIKE）

### 前端
- `views/system/Role.vue`：el-table 角色列表 + el-tree 权限勾选 + 用户分配
- `directive/permission.ts`：`v-permission="'employee:edit'"` 自定义指令隐藏无权按钮
- `store/auth.ts`：登录后从 `/api/me/permissions` 拉权限码集合缓存

## Tasks
### 后端（dev-be）
- [ ] T1 实现 4 张表的 Mapper / Service / Controller（Admin 才能进角色管理）
- [ ] T2 实现 `@HasPermission` + Aspect + `PermissionService` + Caffeine 缓存
- [ ] T3 实现 `@DataScope` + Aspect + MyBatis-Plus `InnerInterceptor` 注入 SQL 条件
- [ ] T4 修改 `JwtAuthenticationFilter` 在登录态注入 `LoginUser.permissions`
- [ ] T5 单测：HasPermissionAspectTest / DataScopeAspectTest（覆盖 4 种 DataScopeType）

### 数据库（dev-db）
- [ ] T6 写 `changelog-00-system-rbac.yaml`：4 张表 + 索引 + 种子 4 角色 + 权限码常量
- [ ] T7 把 admin 用户分配 Admin 角色（种子）

### 前端（dev-fe）
- [ ] T8 角色管理页 + 权限勾选树 + 用户角色分配
- [ ] T9 全局 axios 拦截器处理 403 → 跳 `/403`；`/403` 与 `/login` 页占位
- [ ] T10 `v-permission` 指令 + 路由 meta `permission` 守卫

## 测试要点
- 正向：HR_MANAGER 调 `/api/employees`（数据范围 ALL）拿到全公司
- 正向：LINE_MANAGER 部门为 D2，调 `/api/employees` 仅返回 D2+其子树员工
- 异常：EMPLOYEE 调 `/api/roles` 返回 403
- 边界：用户多角色取并集

## Definition of Done
- [ ] Tasks 全做完
- [ ] 单测 ≥60%
- [ ] qa-lead 用例 100% 通过
- [ ] reviewer P0/P1 全消（重点查：是否每个 Controller 都有 `@HasPermission`、Service 是否生效 `@DataScope`）
- [ ] AC 1-8 演示通过

## Defects
（待填）

## Review Findings
（待填）
