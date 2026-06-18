<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listPerformanceCycles,
} from '@/api/performance';
import {
  listSalaryAdjustments,
  batchGenerateSalaryAdjustments,
  approveSalaryAdjustment,
  rejectSalaryAdjustment,
  executeSalaryAdjustment,
  type SalaryAdjustmentVo,
} from '@/api/performance-enhanced';
import type { PerformanceCycleVo } from '@/types/talent';

const cycles = ref<PerformanceCycleVo[]>([]);
const selectedCycleId = ref<number>();
const adjustments = ref<SalaryAdjustmentVo[]>([]);
const loading = ref(false);
const filterStatus = ref<string>();

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', EXECUTED: 'info',
};

const GRADE_COLOR: Record<string, string> = {
  S: '#f56c6c', A: '#e6a23c', B: '#409eff', C: '#909399', D: '#909399',
};

async function loadCycles(): Promise<void> {
  try { cycles.value = await listPerformanceCycles(); } catch { /* ignore */ }
}

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    adjustments.value = await listSalaryAdjustments({
      cycleId: selectedCycleId.value,
      status: filterStatus.value || undefined,
    });
  } catch {
    ElMessage.error('加载调薪建议失败');
  } finally {
    loading.value = false;
  }
}

async function onBatchGenerate(): Promise<void> {
  if (!selectedCycleId.value) return;
  try {
    await ElMessageBox.confirm('确认为该周期所有已完成考核生成调薪建议？', '批量生成', { type: 'warning' });
    const result = await batchGenerateSalaryAdjustments(selectedCycleId.value);
    ElMessage.success(`已生成 ${result.length} 条调薪建议`);
    await loadData();
  } catch { /* ignore */ }
}

async function onApprove(row: SalaryAdjustmentVo): Promise<void> {
  try {
    await approveSalaryAdjustment(row.id);
    ElMessage.success('已审批通过');
    await loadData();
  } catch { /* ignore */ }
}

async function onReject(row: SalaryAdjustmentVo): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝调薪建议', { inputType: 'textarea' });
    await rejectSalaryAdjustment(row.id, value);
    ElMessage.success('已拒绝');
    await loadData();
  } catch { /* ignore */ }
}

async function onExecute(row: SalaryAdjustmentVo): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认执行调薪？新工资: ${row.suggestedSalary}`, '执行调薪', { type: 'warning' });
    await executeSalaryAdjustment(row.id);
    ElMessage.success('调薪已执行');
    await loadData();
  } catch { /* ignore */ }
}

onMounted(async () => {
  await loadCycles();
});
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h2 class="title">薪酬调整建议</h2>
      <div>
        <el-select v-model="selectedCycleId" placeholder="选择周期" style="width: 180px; margin-right: 8px;" @change="loadData">
          <el-option v-for="c in cycles" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px; margin-right: 8px;" @change="loadData">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已执行" value="EXECUTED" />
        </el-select>
        <el-button type="primary" :disabled="!selectedCycleId" @click="onBatchGenerate">批量生成</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="adjustments" border stripe>
        <el-table-column prop="employeeName" label="员工" width="120" />
        <el-table-column prop="empNo" label="工号" width="120" />
        <el-table-column prop="grade" label="绩效等级" width="100">
          <template #default="{ row }">
            <el-tag :color="GRADE_COLOR[row.grade]" style="color: #fff;">{{ row.grade }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="finalScore" label="最终得分" width="100" />
        <el-table-column prop="currentBaseSalary" label="当前工资" width="120" />
        <el-table-column prop="adjustmentPct" label="调薪比例" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.adjustmentPct > 0 ? '#67c23a' : row.adjustmentPct < 0 ? '#f56c6c' : '#909399' }">
              {{ row.adjustmentPct > 0 ? '+' : '' }}{{ row.adjustmentPct }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="suggestedSalary" label="建议工资" width="120" />
        <el-table-column prop="effectiveDate" label="生效日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="onApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="onReject(row)">拒绝</el-button>
            <el-button v-if="row.status === 'APPROVED'" size="small" type="primary" @click="onExecute(row)">执行调薪</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; }
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.title { margin: 0; font-size: 18px; font-weight: 600; }
</style>
