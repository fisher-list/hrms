<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listLeaveRequests } from '@/api/attendance';
import type { LeaveRequestListVo } from '@/types/attendance';

const list = ref<LeaveRequestListVo[]>([]);
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
  { value: 'CANCELLED', label: '已取消' },
];

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'info',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'info',
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listLeaveRequests({
      employeeId: query.employeeId || undefined,
      status: query.status || undefined,
      current: query.current,
      size: query.size,
    });
    list.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载请假列表失败');
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

onMounted(() => void fetchList());
</script>

<template>
  <div class="leave-request">
    <h2 class="title">请假申请</h2>

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
      <el-table-column prop="leaveTypeId" label="假期类型ID" width="120" />
      <el-table-column prop="startDate" label="开始日期" width="140" />
      <el-table-column prop="endDate" label="结束日期" width="140" />
      <el-table-column prop="days" label="天数" width="80" />
      <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">
            {{ row.status }}
          </el-tag>
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
  </div>
</template>

<style scoped>
.leave-request {
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
