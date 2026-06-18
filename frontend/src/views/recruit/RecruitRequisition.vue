<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { listRequisitions, createRequisition } from '@/api/recruit';
import type { RequisitionVo, RequisitionCreateReq } from '@/types/talent';

const list = ref<RequisitionVo[]>([]);
const total = ref(0);
const loading = ref(false);

const current = ref(1);
const size = ref(10);

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  OPEN: 'success',
  CLOSED: 'info',
};

// ---- 新建表单 ----
const formRef = ref<FormInstance>();
const dialogVisible = ref(false);
const form = reactive<RequisitionCreateReq>({
  positionId: undefined,
  title: '',
  description: '',
  headcount: 1,
  deadline: '',
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入职位标题', trigger: 'blur' }],
  headcount: [{ required: true, message: '请输入招聘人数', trigger: 'blur' }],
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listRequisitions(current.value, size.value);
    list.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载职位需求失败');
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  form.positionId = undefined;
  form.title = '';
  form.description = '';
  form.headcount = 1;
  form.deadline = '';
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    await createRequisition({ ...form });
    ElMessage.success('职位需求已创建');
    dialogVisible.value = false;
    void fetchList();
  } catch {
    // toast already shown
  }
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="requisition-page">
    <div class="header">
      <h2 class="title">职位需求</h2>
      <el-button type="primary" @click="openCreate">新增需求</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="职位标题" min-width="180" />
      <el-table-column prop="positionId" label="岗位ID" width="100" />
      <el-table-column prop="headcount" label="招聘人数" width="100" />
      <el-table-column prop="deadline" label="截止日期" width="140" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="current"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="fetchList"
      @current-change="fetchList"
    />

    <el-dialog v-model="dialogVisible" title="新增职位需求" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="职位标题" prop="title">
          <el-input v-model="form.title" placeholder="如：高级 Java 开发工程师" maxlength="128" />
        </el-form-item>
        <el-form-item label="岗位ID">
          <el-input-number v-model="form.positionId" :min="1" placeholder="可选" style="width: 100%" />
        </el-form-item>
        <el-form-item label="招聘人数" prop="headcount">
          <el-input-number v-model="form.headcount" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="form.deadline"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="可选"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="职位描述及要求"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.requisition-page {
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
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
  display: flex;
}
</style>
