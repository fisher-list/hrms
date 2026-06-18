<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getApprovalTodo, approveTask, rejectTask, getApprovalHistory } from '@/api/approval';
import type { ApprovalTaskVo, ApprovalHistoryVo } from '@/types/portal';

const tasks = ref<ApprovalTaskVo[]>([]);
const loading = ref(false);

// ---- 审批历史弹窗 ----
const historyVisible = ref(false);
const historyLoading = ref(false);
const historyList = ref<ApprovalHistoryVo[]>([]);
const currentInstanceId = ref<number | null>(null);

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  REVOKED: 'info',
};

async function fetchTodo(): Promise<void> {
  loading.value = true;
  try {
    tasks.value = await getApprovalTodo();
  } catch {
    ElMessage.error('加载审批待办失败');
  } finally {
    loading.value = false;
  }
}

async function onApprove(task: ApprovalTaskVo): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入审批意见', '通过审批', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      inputValidator: (v) => (v && v.trim().length > 0 ? true : '审批意见不能为空'),
    });
    await approveTask(task.id, value);
    ElMessage.success('审批已通过');
    void fetchTodo();
  } catch {
    // cancelled or error
  }
}

async function onReject(task: ApprovalTaskVo): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputValidator: (v) => (v && v.trim().length > 0 ? true : '驳回原因不能为空'),
    });
    await rejectTask(task.id, value);
    ElMessage.success('已驳回');
    void fetchTodo();
  } catch {
    // cancelled or error
  }
}

async function showHistory(task: ApprovalTaskVo): Promise<void> {
  currentInstanceId.value = task.instanceId;
  historyVisible.value = true;
  historyLoading.value = true;
  try {
    historyList.value = await getApprovalHistory(task.instanceId);
  } catch {
    ElMessage.error('加载审批历史失败');
  } finally {
    historyLoading.value = false;
  }
}

onMounted(() => void fetchTodo());
</script>

<template>
  <div class="approval-page">
    <h2 class="title">审批待办</h2>

    <el-table v-loading="loading" :data="tasks" border stripe>
      <el-table-column prop="id" label="任务ID" width="100" />
      <el-table-column prop="instanceId" label="审批单ID" width="110" />
      <el-table-column prop="assigneeId" label="审批人ID" width="110" />
      <el-table-column prop="nodeSeq" label="节点序号" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assignedAt" label="分配时间" min-width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            type="success"
            size="small"
            @click="onApprove(row)"
          >
            通过
          </el-button>
          <el-button
            v-if="row.status === 'PENDING'"
            type="danger"
            size="small"
            @click="onReject(row)"
          >
            驳回
          </el-button>
          <el-button type="info" size="small" @click="showHistory(row)">
            历史
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="historyVisible"
      :title="`审批单 #${currentInstanceId} 历史`"
      width="720px"
    >
      <el-table v-loading="historyLoading" :data="historyList" border stripe size="small">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="nodeSeq" label="节点" width="80" />
        <el-table-column prop="actorId" label="操作人ID" width="100" />
        <el-table-column prop="action" label="动作" width="120">
          <template #default="{ row }">
            <el-tag
              :type="row.action === 'APPROVE' ? 'success' : row.action === 'REJECT' ? 'danger' : 'info'"
              size="small"
            >
              {{ row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="意见" min-width="180" show-overflow-tooltip />
        <el-table-column prop="actedAt" label="时间" width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.approval-page {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
