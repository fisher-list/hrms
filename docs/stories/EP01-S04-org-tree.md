# EP01-S04 — 组织树（公司/部门/岗位/职位）

> Status: **Ready for Dev** · Sprint 1 · Owner: `dev-be` + `dev-fe` + `dev-db`
> 依赖：EP01-S01, EP01-S02
> 引用：[`docs/architecture.md`](../architecture.md) §4 · [`docs/prd.md`](../prd.md) §6 Epic 01 · [`docs/data-model.md`](../data-model.md) §2

## 业务背景
组织树是 HR 业务的脊柱：公司（常量化 1 家）→ 部门（多级）→ 岗位（编制坑位）→ 职位（职级序列）。本 Story 完成 4 张表的 CRUD、部门树循环引用校验、岗位编制占用统计与树形 UI。

## 验收标准（AC）
1. 4 张表 `company` / `department` / `position` / `job` 通过 Liquibase 建好；种子 1 条公司 + 5 条部门（按 PRD §8 演示规模）+ 6 个岗位 + 3 个职位序列。
2. 部门树支持最深 5 级（PRD §6 Epic 01 AC 1）；UI 显示树形 + 拖拽移动。
3. 创建/移动部门时若 `parent_id` 等于自身或自身后代，返回 BizException("循环引用")。
4. 删除部门：若存在 `status=ACTIVE` 子部门或 `status=ACTIVE` 员工任职于其下岗位，拒绝删除并提示需先迁移；通过软删（`deleted=1`）。
5. 岗位 `headcount` 编制数 vs `occupied`（已占用）：employee 入职/离职时由触发器或 service 维护；编制满时入职接口拒绝（业务校验）。
6. 岗位创建/编辑必须关联到一个 `job`（职位序列）；薪资范围 `min_salary` / `max_salary` 必填且 max ≥ min。
7. `department.path`（如 `/1/12/120`）由 service 维护，移动时递归更新所有后代 path（事务内）。
8. 所有变更走审计日志；前端用 `el-tree` 展示部门树，右侧抽屉编辑详情。
9. `GET /api/departments/tree` 一次返回完整树（递归 path 已冗余，1 次 SQL 即可）。
10. Line Manager 角色（继承 S02 数据权限）只能看到本部门 + 子树；Admin/HR_MANAGER 看全树。

## 技术上下文

### 数据模型（详见 data-model.md §2.2.1~2.2.4）
- `company`: 单条种子，禁止删除（业务校验）
- `department`: `parent_id` + `path` + `level`（冗余）+ `head_id`（部门负责人 employee_id，可空）
- `position`: `dept_id` FK + `job_id` FK + `headcount` + `occupied`
- `job`: 职位序列 + 数字职级 + 薪资范围（用作 Epic 03 校验）

### path 维护规则
- 创建：`path = parent.path + "/" + newId` 或顶级 `path = "/" + newId`
- 移动：用一条 UPDATE `WHERE path LIKE 'oldPath/%' OR id = movedId`，批量替换前缀（注意 PG/MySQL 字符串函数中性，用 `concat()` 不用 `||`）
- 子树查询：`WHERE path LIKE :rootPath || '/%' OR id = :rootId`（中性写法 `WHERE path LIKE concat(:rootPath, '/%')`）

### 涉及的关键代码契约
- `DepartmentService.tree()` / `create()` / `update()` / `move(id, newParentId)` / `delete(id)`
- `DepartmentService.getSubordinateDeptIds(deptId)` —— 给 S02 的 DataScope 复用
- `PositionService` 维护 `occupied`（提供给 employee Service 调用，不要让 employee 直接 update）
- `CompanyService` 仅提供查询和编辑；删除接口物理拒绝

### 前端
- `views/org/DepartmentTree.vue`：左 el-tree 拖拽 + 右详情/新建表单
- `views/org/Position.vue`：el-table 岗位列表（含编制占用 progress bar）
- `views/org/Job.vue`：职位序列管理
- 路由 `meta.permission` 控制 Admin / HR_MANAGER 才能编辑

## Tasks
### 后端（dev-be）
- [ ] T1 4 张表的 Mapper / Service / Controller
- [ ] T2 `DepartmentService` 循环引用校验 + path 维护 + 子树查询
- [ ] T3 `PositionService` 编制占用同步（暴露 `incrOccupied / decrOccupied` 给 employee 调用）
- [ ] T4 删除前置校验（部门有 active 子或员工 / 公司不可删）
- [ ] T5 全部接口加 `@HasPermission` 注解（沿用 S02）
- [ ] T6 单测：循环引用、path 移动、编制满

### 数据库（dev-db）
- [ ] T7 `changelog-01-org.yaml`：4 张表 + 唯一索引 + 业务索引（详见 data-model §8.1）
- [ ] T8 种子数据：1 公司 + 5 部门（HR/研发/销售/产品/财务）+ 3 职位序列（M/P/T）+ 6 岗位

### 前端（dev-fe）
- [ ] T9 部门树页（拖拽用 el-tree draggable + before-drop hook）
- [ ] T10 岗位/职位管理页
- [ ] T11 树节点删除 confirm + 拒绝时提示需先迁移

## 测试要点
- 部门树 5 级深度可建
- 拖拽部门到自己后代：被拒
- 删除带 active 员工的部门：被拒，提示先转岗
- LINE_MANAGER 看本部门 + 子树
- 岗位编制 = 1，第一个员工入职后 occupied=1，第二个入职被拒

## Definition of Done
- [ ] Tasks 全做完
- [ ] 单测 ≥60%
- [ ] qa-lead 用例 100% 通过
- [ ] reviewer P0/P1 全消（重点查：path 字符串拼接是否方言中性、子树查询是否被 SQL 注入、删除前校验是否在事务内）
- [ ] AC 1-10 演示通过

## Defects
（待填）

## Review Findings
（待填）
