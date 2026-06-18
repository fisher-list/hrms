# HRMS 代码审核清单 v0.1

> 审核员（reviewer）出品，自 Sprint 1 起对每个 Story 落地执行。
> 上位约束见 `/Users/apple/.claude/plans/graceful-splashing-kitten.md` 第六节。
> 业务术语以 `docs/data-model.md` 为准，技术选型以 `docs/architecture.md` 为准。

---

## 一、审核原则

1. **闸口位置**：每个 Story 必须先经 `qa-lead` 测试通过，再进入审核闸口；审核未通过不得标记 Done。
2. **强制修复**：审核结论按 **P0 / P1 / P2** 三级分级管理。
   - **P0（阻断）**：安全漏洞、数据正确性、生产事故风险（如 SQL 注入、敏感字段裸返、权限缺失、N+1、事务错位）。**必须当 Sprint 修完**，否则 Story 退回 dev。
   - **P1（严重）**：架构违规、规范违反、可维护性低（如 Controller 直调 Mapper、any 类型、缺校验、日志未脱敏）。**必须当 Sprint 修完**。
   - **P2（建议）**：风格、注释、命名优化、轻度重复。**可登记进 backlog**，不阻断当前 Story close。
3. **公平原则**：审核员只看代码与提交，不质疑业务设计；如发现疑似业务问题，回 `pm` 走变更流程，不在审核报告里挂 P0。
4. **闭环要求**：每条审核项必须有 **文件:行号 + 问题描述 + 整改建议**；只写"不好"不算合格审核条目。

---

## 二、审核流程

```
dev 完成实现 + 自测
   │
   ▼
qa-lead 测试通过（Story.Defects 全 verified）
   │  SendMessage(reviewer, "请审核 <story-id>")
   ▼
reviewer 拉取 diff（git）+ Read story 文件 + 逐项对照本清单
   │
   ▼
reviewer 在 docs/stories/<story>.md 末尾追加 "Review Findings" 段
   │  P0/P1/P2 分级登记，open 状态
   │
   ▼
SendMessage(dev-be / dev-fe / dev-db, "审核反馈，请修复 P0/P1")
   │
   ▼
dev 修复 → SendMessage(reviewer, "已修复，等复审")
   │
   ▼
reviewer 复审（只看被改动文件） → 把 open 改 fixed → 全 fixed 改 verified
   │
   ▼
SendMessage(pm, "<story-id> 审核 close")  → pm 标 Done
```

补充：
- 审核员**只看**、**不改代码**；发现问题写报告，由 dev 修。
- **复审范围**仅限 dev 在修复轮次中改过的文件；如新引入问题，按本清单登记新条目。
- 审核报告**追加**而非覆盖，保留历史轨迹。

---

## 三、后端 Java 代码审核清单（Spring Boot 3.x + JDK 17 + MyBatis-Plus）

### 3.1 分层依赖（P0）

**规则**：Controller → Service → Mapper 单向依赖；Controller 不得直接注入 Mapper；Service 不得返回 Entity 给前端，必须经 DTO 转换。

反例：
```java
@RestController
public class EmployeeController {
    @Autowired private EmployeeMapper mapper; // ❌ 越层
    @GetMapping("/{id}")
    public Employee get(@PathVariable Long id) { return mapper.selectById(id); } // ❌ 直返 Entity
}
```

正例：
```java
@RestController
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;
    @GetMapping("/{id}")
    public Result<EmployeeDTO> get(@PathVariable Long id) {
        return Result.ok(employeeService.findById(id));
    }
}
```

### 3.2 事务边界（P0）

**规则**：`@Transactional` 只标注在 Service 方法上；必须显式 `rollbackFor = Exception.class`；禁止跨多个 Service 的"上帝事务"；禁止在事务方法内做远程调用 / 文件 IO / 长耗时操作。

反例：
```java
@Transactional // ❌ 默认只回滚 RuntimeException
public void hire(EmployeeDTO dto) {
    employeeService.create(dto);
    contractService.create(dto);
    fileClient.upload(dto.getResume()); // ❌ 远程调用进事务
}
```

正例：
```java
@Transactional(rollbackFor = Exception.class)
public Long hire(EmployeeDTO dto) {
    Long empId = employeeService.create(dto);
    contractService.create(empId, dto.getContract());
    return empId;
}
// 文件上传放事务外
```

### 3.3 异常处理（P0）

**规则**：业务异常统一继承 `BizException`（带 errorCode）；通过 `@RestControllerAdvice` 全局转译为 `Result`；禁止 `catch (Exception e)` 后什么都不做或只 `log.error`。

反例：
```java
try {
    payrollService.run(month);
} catch (Exception e) {
    log.error("err", e); // ❌ 吞掉，调用方拿到 null
    return null;
}
```

正例：
```java
public PayrollRunDTO run(YearMonth month) {
    if (!periodService.isOpen(month)) {
        throw new BizException(PayrollErrorCode.PERIOD_CLOSED, month.toString());
    }
    return doRun(month);
}
// 全局 handler
@ExceptionHandler(BizException.class)
public Result<Void> onBiz(BizException e) {
    return Result.fail(e.getCode(), e.getMessage());
}
```

### 3.4 入参校验（P1）

**规则**：Controller 入参用 `@Valid` + Bean Validation（`@NotNull` / `@Size` / `@Pattern` 等）；Service 入口对**业务规则**再校验一次（如"离职日期不得早于入职日期"）；不要把 Service 当作 Controller 的复刻。

反例：
```java
@PostMapping
public Result<Long> create(@RequestBody EmployeeCreateReq req) {
    return Result.ok(employeeService.create(req)); // ❌ 没校验
}
```

正例：
```java
@PostMapping
public Result<Long> create(@Valid @RequestBody EmployeeCreateReq req) {
    return Result.ok(employeeService.create(req));
}
// DTO
public class EmployeeCreateReq {
    @NotBlank @Size(max = 64) private String name;
    @Pattern(regexp = "^1[3-9]\\d{9}$") private String mobile;
}
```

### 3.5 SQL 注入（P0）

**规则**：MyBatis 占位符必须用 `#{}`；只有动态表名 / 动态排序字段才允许 `${}`，且必须经 `SqlInjectionUtils.check()` 白名单校验。

反例：
```xml
<select id="search">
  SELECT * FROM employee WHERE name LIKE '%${keyword}%' <!-- ❌ 注入 -->
  ORDER BY ${orderBy} <!-- ❌ 直接拼 -->
</select>
```

正例：
```xml
<select id="search">
  SELECT * FROM employee WHERE name LIKE CONCAT('%', #{keyword}, '%')
  ORDER BY ${orderBy} <!-- 必须配合 service 层白名单校验 -->
</select>
```
```java
SqlInjectionUtils.check(orderBy);
List<String> WHITE = List.of("hire_date", "name");
if (!WHITE.contains(orderBy)) throw new BizException(...);
```

### 3.6 N+1 查询（P0）

**规则**：禁止在循环中调用 Mapper；批量查询用 `selectBatchIds` / `IN` 查询，再在内存做 Map 关联。

反例：
```java
for (Employee e : employees) {
    Department d = deptMapper.selectById(e.getDeptId()); // ❌ N+1
    e.setDeptName(d.getName());
}
```

正例：
```java
Set<Long> deptIds = employees.stream().map(Employee::getDeptId).collect(toSet());
Map<Long, Department> deptMap = deptMapper.selectBatchIds(deptIds).stream()
    .collect(toMap(Department::getId, Function.identity()));
employees.forEach(e -> e.setDeptName(deptMap.get(e.getDeptId()).getName()));
```

### 3.7 方言中性（P0）

**规则**：禁止使用 PG/MySQL/Oracle 专属函数；JSON 字段操作只允许"整存整取 String"；字符串拼接用标准 `CONCAT`，不用 `||`；分页用 MyBatis-Plus 分页插件，不手写 `LIMIT` / `ROWNUM`。

反例：
```xml
<select id="findByTag">
  SELECT * FROM employee WHERE ext_info::jsonb @> '{"tag":"core"}' <!-- ❌ PG 专属 -->
  AND name || '_' || code = #{key} <!-- ❌ Oracle/PG 拼接 -->
</select>
```

正例：
```xml
<select id="findByTag">
  SELECT * FROM employee
  <choose>
    <when test="_databaseId == 'postgresql'">
      WHERE ext_info::jsonb @> #{tagJson}::jsonb
    </when>
    <when test="_databaseId == 'mysql'">
      WHERE JSON_CONTAINS(ext_info, #{tagJson})
    </when>
    <otherwise>
      WHERE ext_info LIKE CONCAT('%', #{tagFragment}, '%')
    </otherwise>
  </choose>
  AND CONCAT(name, '_', code) = #{key}
</select>
```

### 3.8 日志（P1）

**规则**：用 SLF4J（`private static final Logger log = LoggerFactory.getLogger(X.class)` 或 `@Slf4j`）；关键业务点 INFO，调试 DEBUG，错误 ERROR；**敏感字段必须脱敏**：薪资 / 身份证 / 银行卡 / 手机号 / 密码。

反例：
```java
log.info("user login: {}", user); // ❌ 整对象打印，含密码
log.info("salary set: empId={}, salary={}", id, salary); // ❌ 薪资明文
```

正例：
```java
log.info("user login: empNo={}", user.getEmpNo());
log.info("salary updated: empId={}, salary={}", id, MaskUtils.maskMoney(salary));
// MaskUtils.maskMobile("13812345678") -> "138****5678"
```

### 3.9 空值与 Optional（P1）

**规则**：返回集合优先返回**空集合**而非 null；可能不存在的单值用 `Optional<T>`；DTO 入参的可选字段用包装类型 + `@Nullable`。

反例：
```java
public List<Employee> listByDept(Long deptId) {
    if (deptId == null) return null; // ❌
    return mapper.selectByDept(deptId);
}
```

正例：
```java
public List<Employee> listByDept(Long deptId) {
    if (deptId == null) return Collections.emptyList();
    return mapper.selectByDept(deptId);
}
public Optional<Employee> findByEmpNo(String empNo) { ... }
```

### 3.10 常量与枚举（P1）

**规则**：禁止 Magic Number / Magic String；状态、类型、字典码必须用 enum 或常量类；状态 enum 实现 `IEnum<Integer>` 与 MyBatis-Plus 集成。

反例：
```java
if (employee.getStatus() == 1) { ... } // ❌ 1 是什么
employee.setType("REGULAR"); // ❌
```

正例：
```java
public enum EmployeeStatus implements IEnum<Integer> {
    ACTIVE(1, "在职"), LEAVE(2, "休假"), TERMINATED(9, "离职");
    private final int code; private final String desc;
    @Override public Integer getValue() { return code; }
}
if (employee.getStatus() == EmployeeStatus.ACTIVE) { ... }
```

---

## 四、前端 Vue / TypeScript 代码审核清单

### 4.1 组件粒度（P1）

**规则**：单 `.vue` 文件不超过 **300 行**；超出则按职责拆分；可复用逻辑抽到 `composables/useXxx.ts`。

反例：一个 800 行的 `EmployeeForm.vue` 同时管表单 / 列表 / 弹窗 / API 调用。

正例：
```ts
// composables/useEmployee.ts
export function useEmployee() {
  const list = ref<EmployeeVO[]>([]);
  const fetch = async (q: Query) => { list.value = await api.searchEmployee(q); };
  return { list, fetch };
}
// EmployeeList.vue 只管渲染与事件
```

### 4.2 状态管理（P1）

**规则**：跨页面 / 跨组件共享状态用 **Pinia store**；单页内用 `ref` / `reactive`；**禁止挂载 `window.xxx` 当全局变量**。

反例：
```ts
window.__currentUser = userInfo; // ❌
```

正例：
```ts
// stores/auth.ts
export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null);
  const setUser = (u: UserInfo) => { user.value = u; };
  return { user, setUser };
});
```

### 4.3 路由守卫（P0）

**规则**：未登录访问受保护页 → 跳 `/login`；已登录但无权限 → 跳 `/403`；不要在每个组件 `onMounted` 里自己判断。

反例：
```ts
// 每个页面都 onMounted(() => { if (!token) router.push('/login') }) ❌
```

正例：
```ts
router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.token) return next('/login');
  if (to.meta.permission && !auth.has(to.meta.permission)) return next('/403');
  next();
});
```

### 4.4 API 错误处理（P1）

**规则**：在 axios 拦截器里统一处理 401（触发 refresh token 或跳登录）/ 403 / 5xx；业务错误（4xx 带 errorCode）由调用方决定 toast 还是页面提示；**禁止裸 `try/catch + alert`**。

反例：
```ts
try {
  await api.createEmployee(form);
} catch (e) {
  alert(e.message); // ❌
}
```

正例：
```ts
// http.ts
http.interceptors.response.use(
  res => res.data.success ? res.data : Promise.reject(res.data),
  err => {
    if (err.response?.status === 401) authStore.refreshOrLogout();
    if (err.response?.status === 403) router.push('/403');
    return Promise.reject(err);
  }
);
// 业务侧
try { await api.createEmployee(form); ElMessage.success('创建成功'); }
catch (e: any) { ElMessage.error(e.message ?? '操作失败'); }
```

### 4.5 XSS 防护（P0）

**规则**：禁止用 `v-html` 渲染用户输入；如必须渲染富文本，先经 DOMPurify 过滤；`href` / `src` 的 URL 也要校验协议白名单。

反例：
```html
<div v-html="employee.remark"></div> <!-- ❌ remark 用户可填 -->
```

正例：
```html
<div v-html="safeRemark"></div>
```
```ts
import DOMPurify from 'dompurify';
const safeRemark = computed(() => DOMPurify.sanitize(employee.value.remark));
```

### 4.6 表单校验（P1）

**规则**：前端校验规则必须与后端 Bean Validation **一致**；前端校验只是 UX，**后端校验才是安全边界**。

反例：前端 `@Pattern("^1[3-9]\\d{9}$")` 但后端只 `@NotBlank` ❌

正例：
```ts
const rules: FormRules = {
  mobile: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  name: [{ max: 64, message: '姓名不超过 64 字' }],
};
// 与 EmployeeCreateReq 的 @Pattern / @Size(max=64) 完全一致
```

### 4.7 可访问性（P2）

**规则**：表单元素必须配 `<label>` 或 `aria-label`；纯图标按钮加 `aria-label`；颜色对比度满足 WCAG AA（前景/背景对比 ≥ 4.5:1）。

反例：
```html
<el-button :icon="Delete" @click="del" /> <!-- ❌ 屏幕阅读器不知是什么 -->
```

正例：
```html
<el-button :icon="Delete" aria-label="删除员工" @click="del" />
<el-form-item label="姓名" prop="name">
  <el-input v-model="form.name" />
</el-form-item>
```

### 4.8 TypeScript 严格模式（P0）

**规则**：`tsconfig.json` 开 `"strict": true`；**禁用 `any`**（如确实需要用 `unknown` + 类型守卫）；所有 API 返回有显式类型定义。

反例：
```ts
const res: any = await http.get('/api/employee'); // ❌
const list = res.data; // 类型丢了
```

正例：
```ts
interface EmployeeVO { id: number; name: string; status: EmployeeStatus; }
interface PageResult<T> { records: T[]; total: number; }
const res = await http.get<PageResult<EmployeeVO>>('/api/employee');
const list: EmployeeVO[] = res.records;
```

### 4.9 性能（P1）

**规则**：路由必须懒加载；列表 ≥500 行启用虚拟滚动（`el-table-v2` / `vue-virtual-scroller`）；大图表 `v-if` 懒挂载；图片懒加载。

反例：
```ts
import EmployeeList from '@/views/EmployeeList.vue'; // ❌ 同步引
const routes = [{ path: '/emp', component: EmployeeList }];
```

正例：
```ts
const routes = [{
  path: '/emp',
  component: () => import('@/views/EmployeeList.vue'),
}];
```

---

## 五、数据库 / Liquibase 审核清单

### 5.1 DDL 走 Liquibase（P0）

**规则**：所有 DDL（CREATE / ALTER / DROP TABLE / INDEX / CONSTRAINT）必须通过 Liquibase changeSet 管理；禁止人工 SQL 改库；DML 初始数据也走 changeSet（标 `context="seed"`）。

反例：上线前手动 `psql -c "ALTER TABLE employee ADD column ..."` ❌

正例：
```xml
<changeSet id="20260601-01-employee-add-email" author="dev-db">
  <addColumn tableName="employee">
    <column name="email" type="varchar(128)"/>
  </addColumn>
  <rollback>
    <dropColumn tableName="employee" columnName="email"/>
  </rollback>
</changeSet>
```

### 5.2 changeSet 元数据（P1）

**规则**：每个 changeSet 必须有：`id`（项目内唯一，建议 `yyyyMMdd-序号-语义`）+ `author`（团队成员通讯名）+ `<rollback>`（除非天然不可回滚，如数据迁移要写 `<empty/>` 并加注释）。

反例：
```xml
<changeSet> <!-- ❌ 没 id 没 author -->
  <createTable tableName="t"/>
</changeSet>
```

正例：见 5.1。

### 5.3 字段类型方言中性（P0）

**规则**：禁止用 `JSON` / `JSONB` / `SERIAL` / `IDENTITY` / `MEDIUMTEXT` 等单方言类型；统一映射：

| 业务含义 | 推荐类型 | 备注 |
|---|---|---|
| 主键 | `bigint` + 雪花算法或序列 | 不用 SERIAL |
| 字符串短 | `varchar(N)` | N 显式声明 |
| 字符串长 | `clob` 或 `varchar(4000)` | 不用 TEXT/MEDIUMTEXT |
| 金额 | `decimal(18,4)` | 全局统一 |
| 日期时间 | `timestamp` | 不用 DATETIME（MySQL 专属语义）|
| 布尔 | `tinyint(1)` 或 `boolean` | Liquibase 自动适配 |
| JSON | `varchar(4000)` 或 `clob` | 应用层序列化为 String |

反例：
```xml
<column name="ext" type="jsonb"/> <!-- ❌ -->
<column name="id" type="serial"/> <!-- ❌ -->
```

正例：
```xml
<column name="id" type="bigint" autoIncrement="false">
  <constraints primaryKey="true" nullable="false"/>
</column>
<column name="ext_info" type="varchar(4000)"/>
<column name="salary" type="decimal(18,4)"/>
```

### 5.4 索引（P1）

**规则**：以下字段必须建索引：① 状态/类型字段（`status`、`emp_type` 等高频过滤）② 外键（`dept_id`、`emp_id`）③ 业务唯一键（`emp_no`、`id_card`）；命名规则：唯一键 `uk_<table>_<col>`，普通索引 `idx_<table>_<col>`。

反例：员工表 50 万行，按 `dept_id` 查询无索引 ❌

正例：
```xml
<createIndex indexName="idx_employee_dept_id" tableName="employee">
  <column name="dept_id"/>
</createIndex>
<addUniqueConstraint constraintName="uk_employee_emp_no"
                     tableName="employee" columnNames="emp_no"/>
```

### 5.5 禁止级联删除（P0）

**规则**：外键不允许 `ON DELETE CASCADE`；删除走业务层软删 + 业务校验，避免误删炸库。

反例：
```xml
<addForeignKeyConstraint baseTableName="employee" baseColumnNames="dept_id"
  referencedTableName="department" referencedColumnNames="id"
  onDelete="CASCADE"/> <!-- ❌ -->
```

正例：FK 不带级联；业务删除部门时，Service 层校验"该部门下是否还有在职员工"，有则拒绝。

### 5.6 命名规范（P1）

**规则**：表名 / 字段名 `snake_case` 全小写；表名单数（`employee` 不是 `employees`）；业务唯一键 `uk_*`，索引 `idx_*`，外键 `fk_*`，主键 `pk_*`；时间字段固定四件套：`created_at` / `created_by` / `updated_at` / `updated_by`。

反例：`Employee_Info` / `empNo` / `idx1` ❌

正例：`employee` / `emp_no` / `idx_employee_dept_id`

---

## 六、安全审核清单（重点章节）

### 6.1 JWT（P0）

**规则**：
- `access_token` 有效期 **15-30 分钟**；`refresh_token` **7 天**；
- 签名密钥从环境变量读（`HRMS_JWT_SECRET`），禁止硬编码或写到 `application.yml` 提交；
- access 放 `Authorization: Bearer` 头，refresh 放 HttpOnly Cookie；
- 退出登录将 refresh token 加入黑名单（Redis）。

反例：
```yaml
hrms:
  jwt:
    secret: my-super-secret-key-12345 # ❌ 硬编码进库
    access-ttl: 7d # ❌ 太长
```

正例：
```yaml
hrms:
  jwt:
    secret: ${HRMS_JWT_SECRET}
    access-ttl: 30m
    refresh-ttl: 7d
```

### 6.2 密码（P0）

**规则**：用 `BCryptPasswordEncoder`（cost ≥ **10**）；**禁止 MD5 / SHA1 / 明文**；改密接口必须验证旧密码；密码字段不准出现在任何返回 DTO。

反例：
```java
user.setPassword(DigestUtils.md5Hex(req.getPassword())); // ❌
```

正例：
```java
private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);
user.setPassword(encoder.encode(req.getPassword()));
// 校验
if (!encoder.matches(rawPwd, user.getPassword())) throw new BizException(LOGIN_FAILED);
```

### 6.3 敏感字段加密与脱敏（P0）

**规则**：
- **存储加密**：薪资 / 身份证 / 银行卡 / 手机号必须 **AES-256** 加密存库（密钥来自 KMS 或环境变量），TypeHandler 透明加解密；
- **接口脱敏**：返回 DTO 时强制脱敏（`****1234` 形式），通过 `@Sensitive(type=...)` + Jackson Serializer 实现；
- **日志脱敏**：见 3.8。

反例：
```java
public class EmployeeVO {
    private String idCard; // ❌ 明文返前端
    private BigDecimal salary; // ❌ 任意角色都看
}
```

正例：
```java
public class EmployeeVO {
    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;       // 序列化为 110**********1234
    @Sensitive(type = SensitiveType.MOBILE)
    private String mobile;
    @Sensitive(type = SensitiveType.SALARY)
    private BigDecimal salary;   // 非 HR 角色返回 null 或 ****
}
```

### 6.4 权限注解（P0）

**规则**：每个 Controller 方法必须带 `@HasPermission("...")` 或 `@PreAuthorize("...")`；公开接口（登录、健康检查）必须显式 `@Anonymous` 标记，方便审核员识别"故意不带权限"。

反例：
```java
@GetMapping("/api/employee/{id}")
public Result<EmployeeVO> get(@PathVariable Long id) { ... } // ❌ 没权限
```

正例：
```java
@HasPermission("employee:view")
@GetMapping("/api/employee/{id}")
public Result<EmployeeVO> get(@PathVariable Long id) { ... }

@Anonymous
@PostMapping("/api/auth/login")
public Result<TokenVO> login(@Valid @RequestBody LoginReq req) { ... }
```

### 6.5 数据权限（P0）

**规则**：员工只看自己 / 经理看下属 / HR 看全公司。**Service 层**用 MyBatis-Plus 的 `DataPermissionInterceptor` 或显式条件实现；**禁止把数据权限放前端**。

反例：
```java
// ❌ 任何人 list 都能看全部
return employeeMapper.selectList(null);
```

正例：
```java
DataScope scope = SecurityUtils.currentDataScope();
LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
switch (scope.getType()) {
    case SELF -> wrapper.eq(Employee::getId, scope.getEmpId());
    case SUBORDINATE -> wrapper.in(Employee::getId, scope.getSubordinateIds());
    case ALL -> { /* HR 不加条件 */ }
    default -> throw new BizException(NO_PERMISSION);
}
return employeeMapper.selectList(wrapper);
```

### 6.6 CSRF（P1）

**规则**：JWT + Bearer 模式可关闭 CSRF（无 Cookie 隐式凭证），但必须在 `SecurityConfig` 注释说明并在 `architecture.md` ADR 记录决策；若以后引入 Cookie 会话，必须重新启用。

正例：
```java
// SecurityConfig
http.csrf(csrf -> csrf.disable()); // ADR-005: 全程 JWT Bearer，无 Cookie 凭证，关闭 CSRF
```

### 6.7 文件上传（P0）

**规则**：
- **白名单后缀**（如 `pdf,jpg,png,docx,xlsx`）；
- **大小限制**（默认 ≤10 MB，简历类 ≤5 MB）；
- **MIME 校验**（不能只看后缀，用 `Tika` 检测）；
- **存储路径不可猜**（UUID + 业务前缀，禁止用原文件名直接落盘）；
- 预留病毒扫描 hook（即使 MVP 不接 ClamAV 也要留接口）。

反例：
```java
@PostMapping("/upload")
public String upload(MultipartFile file) {
    Files.copy(file.getInputStream(), Path.of("/data/" + file.getOriginalFilename())); // ❌
    return file.getOriginalFilename();
}
```

正例：
```java
@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
    fileValidator.validate(file, ALLOWED_EXT, MAX_SIZE);   // 后缀+大小
    fileValidator.validateMime(file, ALLOWED_MIME);        // Tika MIME
    String key = "resume/" + UUID.randomUUID() + "." + ext;
    storage.put(key, file.getInputStream());
    virusScanHook.submitAsync(key);                        // 预留扫描
    return Result.ok(key);
}
```

### 6.8 SQL 日志（P1）

**规则**：开发环境可开 MyBatis SQL 打印；**生产环境必须关闭**（`mybatis-plus.configuration.log-impl` 不配置或配 `NoLogging`）；慢 SQL 阈值 1 s 走单独 logger。

反例：`application-prod.yml` 里 `log-impl: StdOutImpl` ❌

正例：profile 区分；prod 配置不含 `log-impl`；接慢 SQL 拦截器单独写 `log/slow-sql.log`。

---

## 七、代码质量与命名

### 7.1 业务术语一致（P1）

**规则**：英文术语以 `docs/data-model.md` 词汇表为准；同一概念不得在不同模块里出现 `employee` / `staff` / `worker` 三个名字。

反例：`Employee` 实体 + `StaffDTO` + `WorkerVO` 同指一个员工 ❌

正例：统一 `Employee` / `EmployeeDTO` / `EmployeeVO` / `EmployeeCreateReq`。

### 7.2 命名规范（P1）

| 元素 | 规范 | 示例 |
|---|---|---|
| 包名 | 全小写无下划线 | `com.hrms.employee.service` |
| 类名 | UpperCamelCase | `EmployeeService` |
| 接口/实现 | 接口无 `I` 前缀，实现加 `Impl` | `EmployeeService` / `EmployeeServiceImpl` |
| 方法名 | lowerCamelCase | `findByEmpNo` |
| 常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 枚举值 | UPPER_SNAKE_CASE | `EmployeeStatus.ON_LEAVE` |
| 前端组件 | PascalCase `.vue` | `EmployeeForm.vue` |
| Pinia store | `useXxxStore` | `useAuthStore` |

反例：`IEmployeeService` / `findByEmpno` / `MaxPageSize` ❌

### 7.3 重复代码（P2）

**规则**：相同或近似代码出现 **≥ 3 次**，必须抽方法 / 抽工具类 / 抽 composable；2 次可暂留并标记 `// TODO refactor`。

### 7.4 TODO / FIXME（P2）

**规则**：`// TODO` / `// FIXME` 必须包含：① 责任人通讯名 ② issue 链接或 Story id ③ 截止日期。无主 TODO 直接打回。

反例：`// TODO 这里以后优化` ❌

正例：
```java
// TODO(dev-be, story EPIC01-S03, 2026-06-30): replace mock tax rule with config-driven engine
```

---

## 八、Git 提交审核

### 8.1 提交信息格式（P1）

**规则**：`<type>(<scope>): <subject>`，type 取值：`feat | fix | docs | style | refactor | test | chore | build | ci`；subject 中文 / 英文均可，限 50 字内；body 描述"为什么"与"做了什么"。

反例：
```
update code   # ❌
修了个 bug    # ❌
```

正例：
```
feat(employee): 增加员工入职接口

- 新增 POST /api/employee 创建员工
- 关联生成入职合同与初始假期余额
- 关联 Story EPIC01-S02
```

### 8.2 不准提交的文件（P0）

**规则**：以下文件必须在 `.gitignore`，**任何一个出现在 commit 里都是 P0**：
- `.env` / `.env.*`（环境变量含密钥）
- `application-local.yml` / `application-prod.yml`（含真实数据库密码）
- IDE：`.idea/` `.vscode/`（除非显式约定）`*.iml`
- 构建产物：`target/` `dist/` `build/` `node_modules/`
- 密钥与证书：`*.pem` `*.key` `*.p12` `*.jks`
- 日志与临时：`*.log` `tmp/`

反例：commit 包含 `application-prod.yml` 且密钥明文 ❌（同时违反 6.1）

正例：`.gitignore` 完整覆盖；密钥统一从 `${ENV_VAR}` 注入。

---

## 九、审核报告模板

审核员把发现追加到 `docs/stories/<story>.md` 末尾（**不覆盖**，每轮一段，标轮次）：

```markdown
## Review Findings (reviewer) — Round 1 @ 2026-06-15

### P0（阻断，必修）
| #  | 文件:行号 | 问题描述 | 整改建议 | 状态 |
|----|----------|----------|----------|------|
| 01 | backend/src/main/java/com/hrms/employee/controller/EmployeeController.java:42 | Controller 直接注入 EmployeeMapper，违反分层（清单 3.1） | 改为注入 EmployeeService，移除 mapper 字段 | open |
| 02 | backend/src/main/resources/mapper/EmployeeMapper.xml:23 | `ORDER BY ${orderBy}` 直接拼接，存在 SQL 注入（清单 3.5） | 在 Service 层加白名单校验 `SqlInjectionUtils.check` + List<String> WHITE | open |

### P1（严重，必修）
| #  | 文件:行号 | 问题描述 | 整改建议 | 状态 |
|----|----------|----------|----------|------|
| 03 | frontend/src/views/EmployeeList.vue:1-380 | 单文件 380 行，超 300 行阈值（清单 4.1） | 抽 useEmployeeList composable，拆 EmployeeFilterBar / EmployeeTable | open |

### P2（建议，可入 backlog）
| #  | 文件:行号 | 建议 |
|----|----------|------|
| 04 | backend/src/.../EmployeeServiceImpl.java:88-115 | 28 行重复的字段映射逻辑可抽 EmployeeConverter | (backlog) |

### 复审记录
- Round 2 @ 2026-06-16: 01 → fixed → verified；02 → fixed → verified；03 → fixed → verified；新发现 P2 #05（已登记 backlog）。Story 可 close。
```

状态枚举：`open` / `fixed`（dev 声明已修）/ `verified`（reviewer 复核通过）/ `backlog`（P2 转 backlog）/ `wontfix`（与 pm 协商后保留，必须有理由）。

---

## 十、复审规则

1. **触发**：`dev-be` / `dev-fe` / `dev-db` 修复完成后 `SendMessage(reviewer, "已修复 R<n>，等复审")`。
2. **范围**：复审**只看本轮被改动的文件**（`git diff <prev-rev>..HEAD`）；未改动的文件不重审，避免审核员变刷新工具。
3. **新问题处理**：复审中**新引入**的问题：
   - 与本轮修复直接相关（如修 A 引发 B）：**降一级登记**（原 P0 → 新 P1，原 P1 → 新 P2），但仍登记进 Round n。
   - 与本轮无关偶然发现：按本清单原级登记，不降级。
4. **轮次上限**：同一 Story 复审超过 **3 轮**仍未 close，reviewer `SendMessage(pm, "...")` 升级，由 pm 介入定夺（可能是设计问题，需架构师参与）。
5. **close 动作**：所有 P0/P1 状态变 `verified` → reviewer 在 Story 末尾写 `Review Status: PASSED` → `SendMessage(pm, "<story-id> 审核 close")`。

---

## 变更记录

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-06-14 | reviewer | 初版，覆盖后端/前端/数据库/安全/质量/Git/报告/复审八大块，Sprint 1 起执行 |
