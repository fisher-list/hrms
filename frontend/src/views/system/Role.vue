<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type TreeNodeData,
} from 'element-plus';
import { Plus, Edit, Delete, Setting, User } from '@element-plus/icons-vue';
import {
  getRoles,
  createRole,
  updateRole,
  deleteRole,
  getAllPermissions,
  getRolePermissions,
  updateRolePermissions,
  getAllUsers,
  getRoleUsers,
  assignRoleUsers,
} from '@/api/role';
import type { RoleVo, RoleReq, PermissionVo, SimpleUserVo } from '@/types/api';

// --- Role list ---
const roles = ref<RoleVo[]>([]);
const loading = ref(false);

async function fetchRoles(): Promise<void> {
  loading.value = true;
  try {
    roles.value = await getRoles();
  } finally {
    loading.value = false;
  }
}

// --- Role form dialog ---
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<RoleReq>({ code: '', name: '', description: '', enabled: true });
const rules: FormRules<RoleReq> = {
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { max: 32, message: '不超过 32 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 64, message: '不超过 64 个字符', trigger: 'blur' },
  ],
};

function openCreate(): void {
  editingId.value = null;
  form.code = '';
  form.name = '';
  form.description = '';
  form.enabled = true;
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

function openEdit(row: RoleVo): void {
  editingId.value = row.id;
  form.code = row.code;
  form.name = row.name;
  form.description = row.description;
  form.enabled = row.enabled;
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    if (editingId.value != null) {
      await updateRole(editingId.value, { ...form });
      ElMessage.success('角色已更新');
    } else {
      await createRole({ ...form });
      ElMessage.success('角色已创建');
    }
    dialogVisible.value = false;
    await fetchRoles();
  } catch {
    // Error already toasted by interceptor
  }
}

async function handleDelete(row: RoleVo): Promise<void> {
  if (row.builtin) return;
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await deleteRole(row.id);
    ElMessage.success('角色已删除');
    await fetchRoles();
  } catch {
    // Cancelled or error
  }
}

// --- Permission tree dialog ---
const permDialogVisible = ref(false);
const permRoleId = ref<number | null>(null);
const permTree = ref<PermissionVo[]>([]);
const permChecked = ref<number[]>([]);
const permTreeRef = ref();

async function openPermissions(row: RoleVo): Promise<void> {
  permRoleId.value = row.id;
  permDialogVisible.value = true;
  try {
    const [tree, selected] = await Promise.all([
      getAllPermissions(),
      getRolePermissions(row.id),
    ]);
    permTree.value = tree;
    permChecked.value = selected.map((p) => p.id);
  } catch {
    // Error already toasted
  }
}

function handlePermCheck(): void {
  if (!permTreeRef.value) return;
  const checked: number[] = permTreeRef.value.getCheckedKeys(false);
  const halfChecked: number[] = permTreeRef.value.getHalfCheckedKeys();
  permChecked.value = [...checked, ...halfChecked];
}

async function savePermissions(): Promise<void> {
  if (permRoleId.value == null) return;
  try {
    const leafKeys: number[] = permTreeRef.value.getCheckedKeys(false);
    await updateRolePermissions(permRoleId.value, leafKeys);
    ElMessage.success('权限已更新');
    permDialogVisible.value = false;
  } catch {
    // Error already toasted
  }
}

function renderPermNode(data: TreeNodeData): string {
  return data.name ?? data.code ?? '';
}

// --- User assignment dialog ---
const userDialogVisible = ref(false);
const userRoleId = ref<number | null>(null);
const allUsers = ref<SimpleUserVo[]>([]);
const selectedUserIds = ref<number[]>([]);

async function openUsers(row: RoleVo): Promise<void> {
  userRoleId.value = row.id;
  userDialogVisible.value = true;
  try {
    const [users, roleUsers] = await Promise.all([getAllUsers(), getRoleUsers(row.id)]);
    allUsers.value = users;
    selectedUserIds.value = roleUsers.map((u) => u.id);
  } catch {
    // Error already toasted
  }
}

async function saveUsers(): Promise<void> {
  if (userRoleId.value == null) return;
  try {
    await assignRoleUsers(userRoleId.value, selectedUserIds.value);
    ElMessage.success('用户分配已更新');
    userDialogVisible.value = false;
  } catch {
    // Error already toasted
  }
}

onMounted(() => {
  fetchRoles();
});
</script>

<template>
  <div class="role-page">
    <div class="role-header">
      <h2>角色管理</h2>
      <el-button
        v-permission="'role:create'"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        新建角色
      </el-button>
    </div>

    <el-table v-loading="loading" :data="roles" stripe border>
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="name" label="名称" width="160" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="builtin" label="内置" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.builtin ? 'info' : 'success'" size="small">
            {{ row.builtin ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
            {{ row.enabled ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'role:edit'"
            type="primary"
            text
            :icon="Edit"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-permission="'role:permission'"
            type="warning"
            text
            :icon="Setting"
            @click="openPermissions(row)"
          >
            权限
          </el-button>
          <el-button
            v-permission="'role:user'"
            type="success"
            text
            :icon="User"
            @click="openUsers(row)"
          >
            用户
          </el-button>
          <el-button
            v-permission="'role:delete'"
            type="danger"
            text
            :icon="Delete"
            :disabled="row.builtin"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create / Edit dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId != null ? '编辑角色' : '新建角色'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="如 admin" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如 管理员" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- Permission tree dialog -->
    <el-dialog v-model="permDialogVisible" title="权限配置" width="520px" destroy-on-close>
      <el-tree
        ref="permTreeRef"
        :data="permTree"
        :props="{ children: 'children', label: renderPermNode }"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="permChecked"
        @check="handlePermCheck"
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions">保存</el-button>
      </template>
    </el-dialog>

    <!-- User assignment dialog -->
    <el-dialog v-model="userDialogVisible" title="用户分配" width="580px" destroy-on-close>
      <el-transfer
        v-model="selectedUserIds"
        :data="allUsers"
        :titles="['待选用户', '已选用户']"
        :props="{ key: 'id', label: 'nickname' }"
        filterable
        filter-placeholder="搜索用户"
      />
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUsers">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-page {
  padding: 24px;
}
.role-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.role-header h2 {
  margin: 0;
}
</style>
