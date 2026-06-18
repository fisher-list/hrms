<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { getCompany, getDeptTree, createDept, updateDept, deleteDept } from '@/api/org';
import type { DepartmentTreeVo, CompanyVo, DepartmentReq } from '@/types/org-payroll';

const company = ref<CompanyVo | null>(null);
const tree = ref<DepartmentTreeVo[]>([]);
const loading = ref(false);

const formRef = ref<FormInstance>();
const dialogVisible = ref(false);
const dialogTitle = ref('');
const editingId = ref<number | null>(null);

const form = reactive<DepartmentReq>({
  companyId: 0,
  parentId: undefined,
  name: '',
  code: '',
  headId: undefined,
  sortOrder: 0,
});

const rules: FormRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入部门编码', trigger: 'blur' }],
};

async function loadTree(): Promise<void> {
  if (!company.value) return;
  loading.value = true;
  try {
    tree.value = await getDeptTree(company.value.id);
  } catch {
    ElMessage.error('加载部门树失败');
  } finally {
    loading.value = false;
  }
}

function openCreate(parent?: DepartmentTreeVo): void {
  if (!company.value) return;
  dialogTitle.value = parent ? `新增子部门 (父: ${parent.name})` : '新增部门';
  editingId.value = null;
  form.companyId = company.value.id;
  form.parentId = parent?.id;
  form.name = '';
  form.code = '';
  form.headId = undefined;
  form.sortOrder = 0;
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

function openEdit(dept: DepartmentTreeVo): void {
  dialogTitle.value = `编辑部门：${dept.name}`;
  editingId.value = dept.id;
  form.companyId = dept.companyId;
  form.parentId = dept.parentId;
  form.name = dept.name;
  form.code = dept.code;
  form.headId = dept.headId;
  form.sortOrder = dept.sortOrder ?? 0;
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

async function onSave(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    if (editingId.value != null) {
      await updateDept(editingId.value, { ...form });
      ElMessage.success('部门已更新');
    } else {
      await createDept({ ...form });
      ElMessage.success('部门已创建');
    }
    dialogVisible.value = false;
    void loadTree();
  } catch {
    // toast already shown
  }
}

async function onDelete(dept: DepartmentTreeVo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认删除部门「${dept.name}」？子部门会一并被影响`,
      '删除确认',
      { type: 'warning' },
    );
    await deleteDept(dept.id);
    ElMessage.success('已删除');
    void loadTree();
  } catch {
    // user cancelled
  }
}

onMounted(async () => {
  try {
    company.value = await getCompany();
    void loadTree();
  } catch {
    ElMessage.error('加载公司信息失败');
  }
});
</script>

<template>
  <div class="dept-manage">
    <div class="header">
      <h2 class="title">部门管理</h2>
      <span class="company-info">公司：{{ company?.name ?? '—' }}</span>
      <el-button type="primary" :disabled="!company" @click="openCreate()">
        新增根部门
      </el-button>
    </div>

    <el-card v-loading="loading" shadow="never">
      <el-tree
        :data="tree"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span class="node-name">{{ data.name }}</span>
            <span class="node-code">{{ data.code }}</span>
            <span class="node-actions">
              <el-button type="primary" link @click.stop="openCreate(data)">
                新增子部门
              </el-button>
              <el-button type="primary" link @click.stop="openEdit(data)">
                编辑
              </el-button>
              <el-button type="danger" link @click.stop="onDelete(data)">
                删除
              </el-button>
            </span>
          </span>
        </template>
      </el-tree>
      <el-empty v-if="!loading && tree.length === 0" description="暂无部门，请先新增" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="部门编码" prop="code">
          <el-input v-model="form.code" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dept-manage {
  padding: 24px;
}
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.company-info {
  color: #606266;
  font-size: 14px;
  flex: 1;
}
.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 12px;
}
.node-name {
  font-weight: 500;
}
.node-code {
  color: #909399;
  font-size: 12px;
}
.node-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}
</style>
