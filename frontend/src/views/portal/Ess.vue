<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getMyProfile,
  getMyLeaveBalances,
  getMyLeaveRequests,
  getMyTodo,
  getMyPayslips,
} from '@/api/portal';
import type {
  LeaveBalanceVo,
  LeaveRequestVo,
  ApprovalTaskVo,
  PayslipVo,
} from '@/types/portal';

const activeTab = ref('profile');
const loading = ref(false);

const profile = ref<Record<string, unknown> | null>(null);
const balances = ref<LeaveBalanceVo[]>([]);
const leaveRequests = ref<LeaveRequestVo[]>([]);
const todoTasks = ref<ApprovalTaskVo[]>([]);
const payslips = ref<PayslipVo[]>([]);

async function loadProfile(): Promise<void> {
  loading.value = true;
  try {
    profile.value = await getMyProfile();
  } catch {
    ElMessage.error('加载个人信息失败');
  } finally {
    loading.value = false;
  }
}

async function loadBalances(): Promise<void> {
  loading.value = true;
  try {
    balances.value = await getMyLeaveBalances();
  } catch {
    ElMessage.error('加载假期余额失败');
  } finally {
    loading.value = false;
  }
}

async function loadLeaveRequests(): Promise<void> {
  loading.value = true;
  try {
    const page = await getMyLeaveRequests();
    leaveRequests.value = page.records;
  } catch {
    ElMessage.error('加载请假记录失败');
  } finally {
    loading.value = false;
  }
}

async function loadTodo(): Promise<void> {
  loading.value = true;
  try {
    todoTasks.value = await getMyTodo();
  } catch {
    ElMessage.error('加载待办失败');
  } finally {
    loading.value = false;
  }
}

async function loadPayslips(): Promise<void> {
  loading.value = true;
  try {
    payslips.value = await getMyPayslips();
  } catch {
    ElMessage.error('加载工资条失败');
  } finally {
    loading.value = false;
  }
}

function onTabChange(name: string | number): void {
  const key = String(name);
  if (key === 'profile') void loadProfile();
  else if (key === 'balances') void loadBalances();
  else if (key === 'leave') void loadLeaveRequests();
  else if (key === 'todo') void loadTodo();
  else if (key === 'payslips') void loadPayslips();
}

const statusTagType: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  APPROVED: 'success',
  PENDING: 'warning',
  REJECTED: 'danger',
  DRAFT: 'info',
};

onMounted(() => {
  void loadProfile();
});
</script>

<template>
  <div class="ess-portal">
    <h2 class="title">员工自助 (ESS)</h2>
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="个人信息" name="profile">
        <el-card v-loading="loading" shadow="never">
          <template v-if="profile">
            <el-descriptions :column="2" border>
              <el-descriptions-item
                v-for="(value, key) in profile"
                :key="key"
                :label="String(key)"
              >
                {{ value == null ? '—' : String(value) }}
              </el-descriptions-item>
            </el-descriptions>
          </template>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="假期余额" name="balances">
        <el-table v-loading="loading" :data="balances" border stripe>
          <el-table-column prop="year" label="年度" width="100" />
          <el-table-column prop="leaveTypeId" label="假期类型ID" width="120" />
          <el-table-column prop="totalDays" label="总天数" width="120" />
          <el-table-column prop="usedDays" label="已用天数" width="120" />
          <el-table-column prop="remainingDays" label="剩余天数" width="120" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="我的请假" name="leave">
        <el-table v-loading="loading" :data="leaveRequests" border stripe>
          <el-table-column prop="id" label="ID" width="120" />
          <el-table-column prop="leaveTypeId" label="假期类型" width="120" />
          <el-table-column prop="startDate" label="开始日期" width="140" />
          <el-table-column prop="endDate" label="结束日期" width="140" />
          <el-table-column prop="totalDays" label="天数" width="100" />
          <el-table-column prop="reason" label="原因" min-width="160" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType[row.status] ?? 'info'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="待办审批" name="todo">
        <el-table v-loading="loading" :data="todoTasks" border stripe>
          <el-table-column prop="id" label="任务ID" width="160" />
          <el-table-column prop="instanceId" label="审批单ID" width="160" />
          <el-table-column prop="nodeName" label="节点" min-width="140" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="createdAt" label="创建时间" width="200" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="工资条" name="payslips">
        <el-table v-loading="loading" :data="payslips" border stripe>
          <el-table-column prop="runId" label="工资单批次" width="160" />
          <el-table-column prop="grossPay" label="应发" width="140" />
          <el-table-column prop="socialInsurance" label="社保" width="120" />
          <el-table-column prop="housingFund" label="公积金" width="120" />
          <el-table-column prop="iitTax" label="个税" width="120" />
          <el-table-column prop="netPay" label="实发" width="140">
            <template #default="{ row }">
              <strong>{{ row.netPay }}</strong>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.ess-portal {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
