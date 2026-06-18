# 前端修复报告

> 生成时间: 2026-06-18
> 项目: HRMS 前端 (Vue 3 + Element Plus + Pinia + TypeScript)

---

## 【P1-FE1】Token 存储在 localStorage — XSS 风险

### 修复描述
将 `accessToken` 从 localStorage 迁移至 Pinia 内存（`ref`），防止 XSS 攻击窃取 token。`refreshToken` 暂保留在 localStorage（后续改为 HttpOnly cookie）。页面刷新时通过 `/api/auth/refresh` 重新获取 accessToken。

### 修改文件

#### 1. `src/store/auth.ts`
- **移除** `ACCESS_TOKEN_KEY` 的导入（第4行）
- **第20行**: `accessToken` 初始化从 `localStorage.getItem(ACCESS_TOKEN_KEY)` 改为 `ref('')`（纯内存）
- **第26-31行**: `persist()` 中移除 accessToken 的 localStorage 读写
- **新增** `setTokens(access, refresh)` 方法 — 供 axios 拦截器刷新后同步
- **新增** `tryRestore()` 方法 — 页面刷新时通过 refreshToken 恢复 accessToken

```diff
-import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '@/api/http';
+import { REFRESH_TOKEN_KEY } from '@/api/http';

-  const accessToken = ref<string>(localStorage.getItem(ACCESS_TOKEN_KEY) ?? '');
+  // accessToken 仅存于 Pinia 内存，不持久化到 localStorage，防止 XSS 窃取
+  const accessToken = ref<string>('');

   function persist(): void {
-    if (accessToken.value) {
-      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken.value);
-    } else {
-      localStorage.removeItem(ACCESS_TOKEN_KEY);
-    }
+    // accessToken 不再持久化到 localStorage
     // ... refreshToken 和 user 保持不变

+  function setTokens(access: string, refresh: string): void { ... }
+  async function tryRestore(): Promise<boolean> { ... }
```

#### 2. `src/api/http.ts`
- **移除** `ACCESS_TOKEN_KEY` 常量（不再导出）
- **第11行**: 注释说明 refreshToken 暂存 localStorage，后续改为 HttpOnly cookie
- **第37-59行**: `doRefresh()` 成功后调用 `authStore.setTokens()` 同步 Pinia
- **第61-74行**: 请求拦截器改为从 Pinia store 读取 accessToken（动态 import）

```diff
-export const ACCESS_TOKEN_KEY = 'hrms_access';
 export const REFRESH_TOKEN_KEY = 'hrms_refresh';

 async function doRefresh(): Promise<string | null> {
-  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
+  const { useAuthStore } = await import('@/store/auth');
+  const authStore = useAuthStore();
+  const refreshToken = authStore.refreshToken || localStorage.getItem(REFRESH_TOKEN_KEY);
   // ... refresh 成功后:
-  localStorage.setItem(ACCESS_TOKEN_KEY, resp.data.data.accessToken);
-  localStorage.setItem(REFRESH_TOKEN_KEY, resp.data.data.refreshToken);
+  authStore.setTokens(newAccess, newRefresh);

 http.interceptors.request.use(async (config) => {
-  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
+  const { useAuthStore } = await import('@/store/auth');
+  const token = useAuthStore().accessToken;
```

#### 3. `src/router/index.ts`
- **第103-109行**: 未认证时先调用 `auth.tryRestore()` 尝试恢复 token，失败才跳转登录

```diff
   if (requiresAuth && !auth.isAuthenticated) {
-    return { path: '/login', query: { redirect: to.fullPath } };
+    const restored = await auth.tryRestore();
+    if (!restored) {
+      return { path: '/login', query: { redirect: to.fullPath } };
+    }
   }
```

---

## 【P2-FE1】权限 store 不支持通配符匹配

### 修复描述
`has()` 方法改为支持三级匹配：精确匹配 → 全局通配符 `*` → 模块级通配符 `foo:*`，与后端 `PermissionService` 通配符语义一致。

### 修改文件

#### `src/store/permission.ts` — 第18-29行

```diff
   function has(code: string): boolean {
-    return codes.value.has(code);
+    const set = codes.value;
+    // 精确匹配
+    if (set.has(code)) return true;
+    // 全局通配符
+    if (set.has('*')) return true;
+    // 模块级通配符：取冒号前的模块前缀
+    const colonIdx = code.indexOf(':');
+    if (colonIdx > 0) {
+      const moduleWildcard = code.slice(0, colonIdx) + ':*';
+      if (set.has(moduleWildcard)) return true;
+    }
+    return false;
   }
```

---

## 【P2-FE2】v-permission 指令仅在 mounted 时检查一次

### 修复描述
添加 `updated` 钩子，当权限变化（组件重新渲染）时重新检查权限并移除无权限元素。抽取公共检查逻辑到 `checkPermission` 函数。

### 修改文件

#### `src/directives/permission.ts` — 全文重写

```diff
+function checkPermission(el: HTMLElement, binding: { value: string | string[] }): void {
+  const store = usePermissionStore();
+  const required = Array.isArray(binding.value) ? binding.value : [binding.value];
+  const allowed = required.every((code) => store.has(code));
+  if (!allowed) {
+    el.remove();
+  }
+}
+
 const vPermission: Directive<HTMLElement, string | string[]> = {
   mounted(el, binding) {
-    const store = usePermissionStore();
-    const required = Array.isArray(binding.value) ? binding.value : [binding.value];
-    const allowed = required.every((code) => store.has(code));
-    if (!allowed) {
-      el.remove();
-    }
+    checkPermission(el, binding);
+  },
+  updated(el, binding) {
+    checkPermission(el, binding);
   },
 };
```

---

## 【P2-FE3】axios 拦截器 401 刷新逻辑与 Pinia store 不同步

### 修复描述
`doRefresh()` 成功后通过 `authStore.setTokens()` 同步更新 Pinia store 的 accessToken 和 refreshToken。使用动态 import 避免循环依赖。刷新失败时调用 `authStore.logout()` 清理状态。

### 修改文件

#### `src/api/http.ts` — 第37-59行 (doRefresh) 和 第111-113行 (刷新失败处理)

```diff
 async function doRefresh(): Promise<string | null> {
-  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
+  const { useAuthStore } = await import('@/store/auth');
+  const authStore = useAuthStore();
+  const refreshToken = authStore.refreshToken || localStorage.getItem(REFRESH_TOKEN_KEY);
   // ...
   if (resp.data.code === 0 && resp.data.data) {
-    localStorage.setItem(ACCESS_TOKEN_KEY, resp.data.data.accessToken);
-    localStorage.setItem(REFRESH_TOKEN_KEY, resp.data.data.refreshToken);
-    return resp.data.data.accessToken;
+    const { accessToken: newAccess, refreshToken: newRefresh } = resp.data.data;
+    authStore.setTokens(newAccess, newRefresh);
+    return newAccess;
   }

 // 刷新失败时:
-localStorage.removeItem(ACCESS_TOKEN_KEY);
-localStorage.removeItem(REFRESH_TOKEN_KEY);
+const { useAuthStore } = await import('@/store/auth');
+useAuthStore().logout();
```

---

## 【P2-FE4】Element Plus 全量引入

### 修复描述
移除 `main.ts` 中的 `app.use(ElementPlus)` 全量注册，改为使用 `unplugin-vue-components` 和 `unplugin-auto-import` 按需自动引入。

### 修改文件

#### 1. `src/main.ts`

```diff
 import { createApp } from 'vue';
 import { createPinia } from 'pinia';
-import ElementPlus from 'element-plus';
-import 'element-plus/dist/index.css';
 import './styles.css';

 // ...
 app.use(createPinia());
 app.use(router);
-app.use(ElementPlus);
+// Element Plus 已通过 unplugin-vue-components 和 unplugin-auto-import 按需引入
 app.directive('permission', vPermission);
```

#### 2. `vite.config.ts`

```diff
 import { defineConfig } from 'vite';
 import vue from '@vitejs/plugin-vue';
+import AutoImport from 'unplugin-auto-import/vite';
+import Components from 'unplugin-vue-components/vite';
+import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
 import { fileURLToPath, URL } from 'node:url';

 export default defineConfig({
-  plugins: [vue()],
+  plugins: [
+    vue(),
+    AutoImport({
+      resolvers: [ElementPlusResolver()],
+    }),
+    Components({
+      resolvers: [ElementPlusResolver()],
+    }),
+  ],
```

#### 3. `package.json` — 新增 devDependencies

```diff
+    "unplugin-auto-import": "^0.18.x",
+    "unplugin-vue-components": "^0.27.x",
```

---

## 【P2-FE5】EmployeeList 详情弹窗显示 [object Object]

### 修复描述
详情弹窗改为结构化展示：基础信息用 `el-descriptions`，教育经历、工作经历、合同信息分别用独立 `el-table` 展示，避免对象直接渲染为 `[object Object]`。

### 修改文件

#### `src/views/hr/EmployeeList.vue` — 第65-171行

**script 部分变更:**
```diff
+const educationList = ref<Record<string, unknown>[]>([]);
+const workExpList = ref<Record<string, unknown>[]>([]);
+const contractList = ref<Record<string, unknown>[]>([]);
+
+const BASIC_FIELDS: Record<string, string> = {
+  empNo: '工号', name: '姓名', gender: '性别', ...
+};

 async function showDetail(row: EmployeeListVo): Promise<void> {
-  detail.value = await getEmployee(row.id);
+  const data = await getEmployee(row.id) as Record<string, unknown>;
+  detail.value = data;
+  educationList.value = Array.isArray(data.educationList) ? data.educationList : [];
+  workExpList.value = Array.isArray(data.workExpList) ? data.workExpList : [];
+  contractList.value = Array.isArray(data.contractList) ? data.contractList : [];
```

**template 部分变更:**
```diff
 <el-dialog v-model="detailVisible" title="员工详情" width="720px">
-  <el-descriptions v-if="detail" :column="2" border>
-    <el-descriptions-item v-for="(value, key) in detail" :key="key" :label="String(key)">
-      {{ value == null ? '—' : String(value) }}
-    </el-descriptions-item>
-  </el-descriptions>
+  <template v-if="detail">
+    <h4>基本信息</h4>
+    <el-descriptions :column="2" border>
+      <el-descriptions-item v-for="(label, key) in BASIC_FIELDS" :key="key" :label="label">
+        {{ detail[key] ?? '—' }}
+      </el-descriptions-item>
+    </el-descriptions>
+
+    <template v-if="educationList.length">
+      <h4>教育经历</h4>
+      <el-table :data="educationList" border size="small">
+        <el-table-column prop="school" label="学校" />
+        <el-table-column prop="major" label="专业" />
+        <el-table-column prop="degree" label="学位" />
+        <el-table-column prop="startDate" label="开始日期" />
+        <el-table-column prop="endDate" label="结束日期" />
+      </el-table>
+    </template>
+
+    <template v-if="workExpList.length">
+      <h4>工作经历</h4>
+      <el-table :data="workExpList" border size="small"> ... </el-table>
+    </template>
+
+    <template v-if="contractList.length">
+      <h4>合同信息</h4>
+      <el-table :data="contractList" border size="small"> ... </el-table>
+    </template>
+  </template>
 </el-dialog>
```

---

## 修复文件汇总

| 问题编号 | 文件 | 修改类型 |
|---------|------|---------|
| P1-FE1 | `src/store/auth.ts` | 重构 token 存储 + 新增 setTokens/tryRestore |
| P1-FE1 | `src/api/http.ts` | 移除 ACCESS_TOKEN_KEY，请求拦截器改读 Pinia |
| P1-FE1 | `src/router/index.ts` | 添加 tryRestore 逻辑 |
| P2-FE1 | `src/store/permission.ts` | has() 支持通配符匹配 |
| P2-FE2 | `src/directives/permission.ts` | 添加 updated 钩子 |
| P2-FE3 | `src/api/http.ts` | doRefresh 同步 Pinia store |
| P2-FE4 | `src/main.ts` | 移除 Element Plus 全量引入 |
| P2-FE4 | `vite.config.ts` | 添加 unplugin 配置 |
| P2-FE4 | `package.json` | 添加 unplugin 依赖 |
| P2-FE5 | `src/views/hr/EmployeeList.vue` | 详情弹窗结构化展示 |
