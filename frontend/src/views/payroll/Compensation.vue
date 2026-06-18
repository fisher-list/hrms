<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { listCompensations, createCompensation } from '@/api/payroll';
import type { CompensationVo, CompensationCreateReq } from '@/types/org-payroll';

const list = ref<CompensationVo[]>([]);
const total = ref(0);
const loading = ref(false);

const current = ref(1);
const size = ref(10);

// ---- 新建薪资档案 ----
const formRef = ref<FormInstance>();
const dialogVisible = ref(false);
const form = reactive<CompensationCreateReq>({
  employeeId: 0,
  baseSalary: undefined,
  positionSalary: undefined,
  performanceBase: undefined,
  allowance: undefined,
  effectiveDate: '',
});

const rules: FormRules = {
  employeeId: [{ required: true, message: '请输入员工ID', trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listCompensations(current.value, size.value);
    list.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载薪资档案失败');
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  form.employeeId = 0;
  form.baseSalary = undefined;
  form.positionSalary = undefined;
  form.performanceBase = undefined;
  form.allowance = undefined;
  form.effectiveDate = '';
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    await createCompensation({ ...form });
    ElMessage.success('薪资档案已创建');
    dialogVisible.value = false;
    void fetchList();
  } catch {
    // toast already shown
  }
}

function formatMoney(val: number | undefined): string {
  return val != null ? val.toFixed(2) : '—';
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="compensation-page">
    <div class="header">
      <h2 class="title">薪资档案</h2>
      <el-button type="primary" @click="openCreate">新增档案</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="employeeId" label="员工ID" width="100" />
      <el-table-column prop="baseSalary" label="基本工资" width="140">
        <template #default="{ row }">{{ formatMoney(row.baseSalary) }}</template>
      </el-table-column>
      <el-table-column prop="positionSalary" label="岗位工资" width="140">
        <template #default="{ row }">{{ formatMoney(row.positionSalary) }}</template>
      </el-table-column>
      <el-table-column prop="performanceBase" label="绩效基数" width="140">
        <template #default="{ row }">{{ formatMoney(row.performanceBase) }}</template>
      </el-table-column>
      <el-table-column prop="allowance" label="津贴" width="120">
        <template #default="{ row }">{{ formatMoney(row.allowance) }}</template>
      </el-table-column>
      <el-table-column prop="effectiveDate" label="生效日期" width="140" />
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

    <el-dialog v-model="dialogVisible" title="新增薪资档案" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="员工ID" prop="employeeId">
          <el-input-number v-model="form.employeeId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="基本工资">
          <el-input-number v-model="form.baseSalary" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="岗位工资">
          <el-input-number v-model="form.positionSalary" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="绩效基数">
          <el-input-number v-model="form.performanceBase" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="津贴">
          <el-input-number v-model="form.allowance" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="form.effectiveDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
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
.compensation-page {
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
