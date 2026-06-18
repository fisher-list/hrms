<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { listOvertimeRequests, createOvertimeRequest, submitOvertimeRequest } from '@/api/attendance';
import type { OvertimeRequestVo, OvertimeRequestCreateReq } from '@/types/attendance';

const list = ref<OvertimeRequestVo[]>([]);
const total = ref(0);
const loading = ref(false);

const query = reactive({
  employeeId: undefined as number | undefined,
  status: '',
  current: 1,
  size: 10,
});

const STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '草稿' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
];

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'info',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

// ---- 新建加班表单 ----
const formRef = ref<FormInstance>();
const dialogVisible = ref(false);
const form = reactive<OvertimeRequestCreateReq>({
  overtimeDate: '',
  hours: 0,
  reason: '',
});

const rules: FormRules = {
  overtimeDate: [{ required: true, message: '请选择加班日期', trigger: 'change' }],
  hours: [{ required: true, message: '请输入加班时长', trigger: 'blur' }],
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listOvertimeRequests({
      employeeId: query.employeeId || undefined,
      status: query.status || undefined,
      current: query.current,
      size: query.size,
    });
    list.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载加班列表失败');
  } finally {
    loading.value = false;
  }
}

function onSearch(): void {
  query.current = 1;
  void fetchList();
}

function onReset(): void {
  query.employeeId = undefined;
  query.status = '';
  query.current = 1;
  void fetchList();
}

function openCreate(): void {
  form.overtimeDate = '';
  form.hours = 0;
  form.reason = '';
  dialogVisible.value = true;
  nextTick(() => formRef.value?.clearValidate());
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  try {
    const created = await createOvertimeRequest({ ...form });
    await submitOvertimeRequest(created.id);
    ElMessage.success('加班申请已提交审批');
    dialogVisible.value = false;
    void fetchList();
  } catch {
    // toast already shown
  }
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="overtime-request">
    <div class="header">
      <h2 class="title">加班申请</h2>
      <el-button type="primary" @click="openCreate">新建加班</el-button>
    </div>

    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="员工ID">
        <el-input-number
          v-model="query.employeeId"
          :min="1"
          placeholder="全部"
          controls-position="right"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" style="width: 140px">
          <el-option
            v-for="s in STATUS_OPTIONS"
            :key="s.value"
            :label="s.label"
            :value="s.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="employeeId" label="员工ID" width="100" />
      <el-table-column prop="overtimeDate" label="加班日期" width="140" />
      <el-table-column prop="hours" label="时长(h)" width="100" />
      <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="approvalInstanceId" label="审批单ID" width="110" />
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :page-sizes="[10, 20, 50]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="fetchList"
      @current-change="fetchList"
    />

    <el-dialog v-model="dialogVisible" title="新建加班申请" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="加班日期" prop="overtimeDate">
          <el-date-picker
            v-model="form.overtimeDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时长(小时)" prop="hours">
          <el-input-number v-model="form.hours" :min="0.5" :step="0.5" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写加班原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.overtime-request {
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
.filter-bar {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
  display: flex;
}
</style>
