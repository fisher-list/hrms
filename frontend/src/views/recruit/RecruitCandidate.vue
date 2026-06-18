<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { listCandidates, createCandidate } from '@/api/recruit';
import type { CandidateVo, CandidateCreateReq } from '@/types/talent';

const list = ref<CandidateVo[]>([]);
const total = ref(0);
const loading = ref(false);

const current = ref(1);
const size = ref(10);

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  NEW: 'info',
  INTERVIEW: 'warning',
  OFFER: 'warning',
  ACCEPTED: 'success',
  REJECTED: 'danger',
};

// ---- 新建候选人 ----
const formRef = ref<FormInstance>();
const dialogVisible = ref(false);
const form = reactive<CandidateCreateReq>({
  name: '',
  phone: '',
  email: '',
  idCardEnc: '',
  expectedSalary: undefined,
  jobRequisitionId: undefined,
});

const rules: FormRules = {
  name: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }],
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listCandidates(current.value, size.value);
    list.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载候选人列表失败');
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  form.name = '';
  form.phone = '';
  form.email = '';
  form.idCardEnc = '';
  form.expectedSalary = undefined;
  form.jobRequisitionId = undefined;
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    await createCandidate({ ...form });
    ElMessage.success('候选人已创建');
    dialogVisible.value = false;
    void fetchList();
  } catch {
    // toast already shown
  }
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="candidate-page">
    <div class="header">
      <h2 class="title">候选人管理</h2>
      <el-button type="primary" @click="openCreate">新增候选人</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="expectedSalary" label="期望薪资" width="140" />
      <el-table-column prop="jobRequisitionId" label="职位需求ID" width="120" />
      <el-table-column prop="currentStatus" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.currentStatus] ?? 'info'">
            {{ row.currentStatus }}
          </el-tag>
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

    <el-dialog v-model="dialogVisible" title="新增候选人" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="身份证">
          <el-input v-model="form.idCardEnc" />
        </el-form-item>
        <el-form-item label="期望薪资">
          <el-input-number v-model="form.expectedSalary" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="职位需求ID">
          <el-input-number v-model="form.jobRequisitionId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.candidate-page {
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
