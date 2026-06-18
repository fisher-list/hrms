<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getCompLeaveBalance,
  listCompLeaveBalances,
  listCompLeaveLogs,
  convertOvertimeToCompLeave,
} from '@/api/attendance';
import type { CompensatoryLeaveVo, CompensatoryLeaveLogVo } from '@/types/attendance';

/** 当前余额 */
const balance = ref<CompensatoryLeaveVo | null>(null);
/** 历史余额列表 */
const balances = ref<CompensatoryLeaveVo[]>([]);
/** 变动日志 */
const logs = ref<CompensatoryLeaveLogVo[]>([]);
const loading = ref(false);
const logLoading = ref(false);
const showConvertDialog = ref(false);

/** 加班转调休表单 */
const convertForm = reactive({
  overtimeRequestId: undefined as number | undefined,
  convertRate: 1.0,
  remark: '',
});

/** 日志类型标签 */
const LOG_TYPE_TAG: Record<string, 'success' | 'danger' | 'warning'> = {
  CONVERT: 'success',
  DEDUCT: 'danger',
  REFUND: 'warning',
};

const LOG_TYPE_LABEL: Record<string, string> = {
  CONVERT: '转入',
  DEDUCT: '扣减',
  REFUND: '退回',
};

/** 加载当前年度余额 */
async function fetchBalance(): Promise<void> {
  loading.value = true;
  try {
    balance.value = await getCompLeaveBalance();
  } catch {
    ElMessage.error('加载调休余额失败');
  } finally {
    loading.value = false;
  }
}

/** 加载历史余额 */
async function fetchBalances(): Promise<void> {
  try {
    balances.value = await listCompLeaveBalances();
  } catch {
    ElMessage.error('加载历史余额失败');
  }
}

/** 加载变动日志 */
async function fetchLogs(compLeaveId: number): Promise<void> {
  logLoading.value = true;
  try {
    logs.value = await listCompLeaveLogs(compLeaveId);
  } catch {
    ElMessage.error('加载变动日志失败');
  } finally {
    logLoading.value = false;
  }
}

/** 打开日志 */
function onViewLogs(compLeaveId: number): void {
  void fetchLogs(compLeaveId);
}

/** 提交加班转调休 */
async function onSubmitConvert(): Promise<void> {
  if (!convertForm.overtimeRequestId) {
    ElMessage.warning('请输入加班申请ID');
    return;
  }
  try {
    await convertOvertimeToCompLeave({
      overtimeRequestId: convertForm.overtimeRequestId,
      convertRate: convertForm.convertRate,
      remark: convertForm.remark || undefined,
    });
    ElMessage.success('转换成功');
    showConvertDialog.value = false;
    convertForm.overtimeRequestId = undefined;
    convertForm.convertRate = 1.0;
    convertForm.remark = '';
    void fetchBalance();
    void fetchBalances();
  } catch {
    ElMessage.error('转换失败');
  }
}

onMounted(() => {
  void fetchBalance();
  void fetchBalances();
});
</script>

<template>
  <div class="comp-leave-page">
    <h2 class="title">调休管理</h2>

    <!-- 当前余额概览 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">总额度（天）</div>
            <div class="stat-value">{{ balance?.totalQuota ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">已使用（天）</div>
            <div class="stat-value" style="color: #e6a23c">{{ balance?.used ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">剩余可用（天）</div>
            <div class="stat-value" style="color: #67c23a">{{ balance?.remaining ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作按钮 -->
    <el-button type="primary" style="margin-bottom: 16px" @click="showConvertDialog = true">
      加班转调休
    </el-button>

    <!-- 历史余额 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>
        <span style="font-weight: 600">历史余额</span>
      </template>
      <el-table :data="balances" border stripe>
        <el-table-column prop="year" label="年度" width="100" />
        <el-table-column prop="totalQuota" label="总额度" width="120" />
        <el-table-column prop="used" label="已使用" width="120" />
        <el-table-column prop="remaining" label="剩余" width="120" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="onViewLogs(row.id)">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 变动日志 -->
    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600">变动日志</span>
      </template>
      <el-table v-loading="logLoading" :data="logs" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="changeType" label="变动类型" width="100">
          <template #default="{ row }">
            <el-tag :type="LOG_TYPE_TAG[row.changeType] ?? 'info'">
              {{ LOG_TYPE_LABEL[row.changeType] ?? row.changeType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeValue" label="变动值" width="100" />
        <el-table-column prop="convertRate" label="转换倍率" width="100" />
        <el-table-column prop="overtimeRequestId" label="加班单ID" width="110" />
        <el-table-column prop="leaveRequestId" label="请假单ID" width="110" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-card>

    <!-- 加班转调休对话框 -->
    <el-dialog v-model="showConvertDialog" title="加班转调休" width="480px">
      <el-form :model="convertForm" label-width="100px">
        <el-form-item label="加班申请ID" required>
          <el-input-number
            v-model="convertForm.overtimeRequestId"
            :min="1"
            placeholder="输入已审批的加班申请ID"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="转换倍率" required>
          <el-input-number
            v-model="convertForm.convertRate"
            :min="0.1"
            :max="10"
            :step="0.5"
            :precision="1"
            style="width: 100%"
          />
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            1.0 = 加班1天转1天调休，1.5 = 加班1天转1.5天调休
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="convertForm.remark"
            type="textarea"
            :rows="3"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showConvertDialog = false">取消</el-button>
        <el-button type="primary" @click="onSubmitConvert">确认转换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.comp-leave-page {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.summary-row {
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
  padding: 12px 0;
}
.stat-label {
  color: #999;
  font-size: 14px;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
}
</style>
