<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { listPositions, createPosition, updatePosition, deletePosition } from '@/api/org';
import type { PositionVo, PositionReq } from '@/types/org-payroll';

const list = ref<PositionVo[]>([]);
const loading = ref(false);

const formRef = ref<FormInstance>();
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);

const form = reactive<PositionReq>({
  deptId: 0,
  jobId: 0,
  name: '',
  code: '',
  headcount: undefined,
});

const rules: FormRules = {
  deptId: [{ required: true, message: '请输入部门ID', trigger: 'blur' }],
  jobId: [{ required: true, message: '请输入职位族ID', trigger: 'blur' }],
  name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    list.value = await listPositions();
  } catch {
    ElMessage.error('加载岗位列表失败');
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  editingId.value = null;
  form.deptId = 0;
  form.jobId = 0;
  form.name = '';
  form.code = '';
  form.headcount = undefined;
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

function openEdit(row: PositionVo): void {
  editingId.value = row.id;
  form.deptId = row.deptId;
  form.jobId = row.jobId;
  form.name = row.name;
  form.code = row.code;
  form.headcount = row.headcount;
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    if (editingId.value != null) {
      await updatePosition(editingId.value, { ...form });
      ElMessage.success('岗位已更新');
    } else {
      await createPosition({ ...form });
      ElMessage.success('岗位已创建');
    }
    dialogVisible.value = false;
    await fetchList();
  } catch {
    // toast already shown
  }
}

async function handleDelete(row: PositionVo): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除岗位「${row.name}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await deletePosition(row.id);
    ElMessage.success('岗位已删除');
    await fetchList();
  } catch {
    // cancelled
  }
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="position-manage">
    <div class="header">
      <h2 class="title">岗位管理</h2>
      <el-button type="primary" @click="openCreate">新增岗位</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="岗位名称" min-width="160" />
      <el-table-column prop="code" label="编码" width="140" />
      <el-table-column prop="deptId" label="部门ID" width="100" />
      <el-table-column prop="jobId" label="职位族ID" width="100" />
      <el-table-column prop="headcount" label="编制" width="100" />
      <el-table-column prop="occupied" label="在岗" width="100" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId != null ? '编辑岗位' : '新增岗位'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="部门ID" prop="deptId">
          <el-input-number v-model="form.deptId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="职位族ID" prop="jobId">
          <el-input-number v-model="form.jobId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="form.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="编制人数">
          <el-input-number v-model="form.headcount" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.position-manage {
  padding: 24px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
</style>
