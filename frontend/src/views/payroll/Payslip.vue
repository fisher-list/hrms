<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listMyPayslips } from '@/api/payroll';
import type { PayrollDetailVo } from '@/types/org-payroll';

const list = ref<PayrollDetailVo[]>([]);
const loading = ref(false);

const detailVisible = ref(false);
const currentPayslip = ref<PayrollDetailVo | null>(null);

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    list.value = await listMyPayslips();
  } catch {
    ElMessage.error('加载工资条失败');
  } finally {
    loading.value = false;
  }
}

function showDetail(row: PayrollDetailVo): void {
  currentPayslip.value = row;
  detailVisible.value = true;
}

function formatMoney(val: number | undefined): string {
  return val != null ? `¥${val.toFixed(2)}` : '—';
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="payslip-page">
    <h2 class="title">我的工资条</h2>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="runId" label="批次ID" width="100" />
      <el-table-column prop="empNo" label="工号" width="120" />
      <el-table-column prop="employeeName" label="姓名" width="120" />
      <el-table-column prop="grossPay" label="应发工资" width="140">
        <template #default="{ row }">{{ formatMoney(row.grossPay) }}</template>
      </el-table-column>
      <el-table-column prop="socialInsurance" label="社保扣除" width="140">
        <template #default="{ row }">{{ formatMoney(row.socialInsurance) }}</template>
      </el-table-column>
      <el-table-column prop="housingFund" label="公积金扣除" width="140">
        <template #default="{ row }">{{ formatMoney(row.housingFund) }}</template>
      </el-table-column>
      <el-table-column prop="iit" label="个税" width="120">
        <template #default="{ row }">{{ formatMoney(row.iit) }}</template>
      </el-table-column>
      <el-table-column prop="netPay" label="实发工资" width="140">
        <template #default="{ row }">
          <strong style="color: #409eff">{{ formatMoney(row.netPay) }}</strong>
        </template>
      </el-table-column>
      <el-table-column prop="isException" label="异常" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isException" type="danger" size="small">异常</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="detailVisible"
      title="工资条详情"
      width="600px"
    >
      <template v-if="currentPayslip">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工号">{{ currentPayslip.empNo ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ currentPayslip.employeeName ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="批次ID">{{ currentPayslip.runId }}</el-descriptions-item>
          <el-descriptions-item label="应发工资">{{ formatMoney(currentPayslip.grossPay) }}</el-descriptions-item>
          <el-descriptions-item label="社保扣除">{{ formatMoney(currentPayslip.socialInsurance) }}</el-descriptions-item>
          <el-descriptions-item label="公积金扣除">{{ formatMoney(currentPayslip.housingFund) }}</el-descriptions-item>
          <el-descriptions-item label="个人所得税">{{ formatMoney(currentPayslip.iit) }}</el-descriptions-item>
          <el-descriptions-item label="实发工资">
            <strong style="color: #409eff; font-size: 16px">{{ formatMoney(currentPayslip.netPay) }}</strong>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.payslip-page {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
