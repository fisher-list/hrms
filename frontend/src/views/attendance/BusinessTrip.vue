<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listBusinessTrips, createBusinessTrip, submitBusinessTrip } from '@/api/attendance';
import type { BusinessTripVo } from '@/types/attendance';

const list = ref<BusinessTripVo[]>([]);
const total = ref(0);
const loading = ref(false);
const showCreateDialog = ref(false);
const createLoading = ref(false);

/** 查询条件 */
const query = reactive({
  employeeId: undefined as number | undefined,
  status: '',
  current: 1,
  size: 10,
});

/** 新建出差表单 */
const form = reactive({
  destination: '',
  reason: '',
  startDate: '',
  endDate: '',
  days: undefined as number | undefined,
});

/** 状态选项 */
const STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '草稿' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'CANCELLED', label: '已取消' },
];

/** 状态标签 */
const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'info',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'info',
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: '草稿',
  SUBMITTED: '已提交',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
};

/** 加载列表 */
async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listBusinessTrips({
      employeeId: query.employeeId || undefined,
      status: query.status || undefined,
      current: query.current,
      size: query.size,
    });
    list.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载出差列表失败');
  } finally {
    loading.value = false;
  }
}

/** 创建出差申请 */
async function onCreate(): Promise<void> {
  if (!form.destination || !form.reason || !form.startDate || !form.endDate || !form.days) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  createLoading.value = true;
  try {
    await createBusinessTrip({
      destination: form.destination,
      reason: form.reason,
      startDate: form.startDate,
      endDate: form.endDate,
      days: form.days,
    });
    ElMessage.success('出差申请创建成功');
    showCreateDialog.value = false;
    resetForm();
    void fetchList();
  } catch {
    ElMessage.error('创建失败');
  } finally {
    createLoading.value = false;
  }
}

/** 提交审批 */
async function onSubmit(row: BusinessTripVo): Promise<void> {
  try {
    await submitBusinessTrip(row.id);
    ElMessage.success('已提交审批');
    void fetchList();
  } catch {
    ElMessage.error('提交失败');
  }
}

function resetForm(): void {
  form.destination = '';
  form.reason = '';
  form.startDate = '';
  form.endDate = '';
  form.days = undefined;
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

onMounted(() => void fetchList());
</script>

<template>
  <div class="trip-page">
    <h2 class="title">出差管理</h2>

    <!-- 筛选条件 -->
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="员工ID">
        <el-input-number
          v-model="query.employeeId"
          :min="1"
          placeholder="全部"
          controls-position="right"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" style="width: 140px">
          <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
        <el-button type="success" @click="showCreateDialog = true">新建出差</el-button>
      </el-form-item>
    </el-form>

    <!-- 出差列表 -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="employeeId" label="员工ID" width="100" />
      <el-table-column prop="destination" label="目的地" width="150" show-overflow-tooltip />
      <el-table-column prop="reason" label="事由" min-width="180" show-overflow-tooltip />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="days" label="天数" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">
            {{ STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="primary" link @click="onSubmit(row)">
            提交审批
          </el-button>
          <span v-else-if="row.status === 'SUBMITTED'" style="color: #999; font-size: 12px">
            审批中
          </span>
          <span v-else style="color: #999; font-size: 12px">
            {{ STATUS_LABEL[row.status] }}
          </span>
        </template>
      </el-table-column>
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

    <!-- 新建出差对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建出差申请" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="目的地" required>
          <el-input v-model="form.destination" placeholder="如：北京、上海" />
        </el-form-item>
        <el-form-item label="事由" required>
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            placeholder="请填写出差事由"
          />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择开始日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择结束日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="出差天数" required>
          <el-input-number
            v-model="form.days"
            :min="0.5"
            :step="0.5"
            :precision="1"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.trip-page {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
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
