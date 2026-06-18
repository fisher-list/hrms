<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listPeriods,
  createPeriod,
  listRuns,
  createRun,
  calculateRun,
  lockRun,
  reverseRun,
  getRunDetails,
} from '@/api/payroll';
import type {
  PayrollPeriodVo,
  PayrollRunVo,
  PayrollDetailVo,
} from '@/types/org-payroll';

const periods = ref<PayrollPeriodVo[]>([]);
const runs = ref<PayrollRunVo[]>([]);
const selectedPeriodId = ref<number | undefined>();
const loadingPeriods = ref(false);
const loadingRuns = ref(false);

const newPeriodMonth = ref('');

const detailsVisible = ref(false);
const detailsLoading = ref(false);
const details = ref<PayrollDetailVo[]>([]);
const detailRunId = ref<number | null>(null);

const RUN_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
  DRAFT: 'info',
  CALCULATED: 'warning',
  LOCKED: 'success',
  REVERSED: 'danger',
};

const filteredRuns = computed(() =>
  selectedPeriodId.value == null
    ? runs.value
    : runs.value.filter((r) => r.periodId === selectedPeriodId.value),
);

async function loadPeriods(): Promise<void> {
  loadingPeriods.value = true;
  try {
    periods.value = await listPeriods();
  } catch {
    ElMessage.error('加载薪酬周期失败');
  } finally {
    loadingPeriods.value = false;
  }
}

async function loadRuns(): Promise<void> {
  loadingRuns.value = true;
  try {
    runs.value = await listRuns(selectedPeriodId.value);
  } catch {
    ElMessage.error('加载薪酬批次失败');
  } finally {
    loadingRuns.value = false;
  }
}

async function onCreatePeriod(): Promise<void> {
  if (!/^\d{4}-\d{2}$/.test(newPeriodMonth.value)) {
    ElMessage.warning('请输入 YYYY-MM 格式，如 2026-06');
    return;
  }
  try {
    await createPeriod(newPeriodMonth.value);
    ElMessage.success('周期已创建');
    newPeriodMonth.value = '';
    void loadPeriods();
  } catch {
    // ignore
  }
}

async function onCreateRun(): Promise<void> {
  if (selectedPeriodId.value == null) {
    ElMessage.warning('请先选择薪酬周期');
    return;
  }
  try {
    await createRun(selectedPeriodId.value);
    ElMessage.success('批次已创建');
    void loadRuns();
  } catch {
    // ignore
  }
}

async function onCalculate(run: PayrollRunVo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认对批次 #${run.id} 执行工资计算？`,
      '计算确认',
      { type: 'info' },
    );
    const ids = await calculateRun(run.id);
    ElMessage.success(`计算完成，生成 ${ids.length} 条明细`);
    void loadRuns();
  } catch {
    // cancelled
  }
}

async function onLock(run: PayrollRunVo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `锁定后批次不可再次计算，确认锁定 #${run.id}？`,
      '锁定确认',
      { type: 'warning' },
    );
    await lockRun(run.id);
    ElMessage.success('已锁定');
    void loadRuns();
  } catch {
    // ignore
  }
}

async function onReverse(run: PayrollRunVo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `冲销将创建一笔反向批次，原批次保留。确认对 #${run.id} 冲销？`,
      '冲销确认',
      { type: 'warning' },
    );
    await reverseRun(run.id);
    ElMessage.success('已冲销');
    void loadRuns();
  } catch {
    // ignore
  }
}

async function showDetails(run: PayrollRunVo): Promise<void> {
  detailRunId.value = run.id;
  detailsVisible.value = true;
  detailsLoading.value = true;
  try {
    details.value = await getRunDetails(run.id);
  } catch {
    ElMessage.error('加载明细失败');
  } finally {
    detailsLoading.value = false;
  }
}

onMounted(async () => {
  await loadPeriods();
  await loadRuns();
});
</script>

<template>
  <div class="payroll-page">
    <h2 class="title">薪酬批次管理</h2>

    <el-card shadow="never" class="section">
      <template #header><span>薪酬周期</span></template>
      <div class="period-bar">
        <el-input
          v-model="newPeriodMonth"
          placeholder="YYYY-MM,如 2026-06"
          style="width: 200px"
          maxlength="7"
        />
        <el-button type="primary" @click="onCreatePeriod">新建周期</el-button>
        <el-divider direction="vertical" />
        <el-select
          v-model="selectedPeriodId"
          placeholder="筛选批次的周期"
          clearable
          style="width: 220px"
          @change="loadRuns"
        >
          <el-option
            v-for="p in periods"
            :key="p.id"
            :label="`${p.periodMonth} (${p.status})`"
            :value="p.id"
          />
        </el-select>
        <el-button :disabled="selectedPeriodId == null" @click="onCreateRun">
          为该周期新建批次
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="section">
      <template #header><span>批次列表</span></template>
      <el-table v-loading="loadingRuns" :data="filteredRuns" border stripe>
        <el-table-column prop="id" label="批次ID" width="160" />
        <el-table-column prop="periodId" label="周期ID" width="120" />
        <el-table-column prop="runType" label="类型" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="RUN_STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="employeeCount" label="员工数" width="100" />
        <el-table-column prop="totalGross" label="总应发" width="140" />
        <el-table-column prop="totalNet" label="总实发" width="140" />
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'DRAFT'"
              type="primary"
              size="small"
              @click="onCalculate(row)"
            >
              计算
            </el-button>
            <el-button
              v-if="row.status === 'CALCULATED'"
              type="warning"
              size="small"
              @click="onLock(row)"
            >
              锁定
            </el-button>
            <el-button
              v-if="row.status === 'LOCKED'"
              type="danger"
              size="small"
              @click="onReverse(row)"
            >
              冲销
            </el-button>
            <el-button size="small" @click="showDetails(row)">明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="detailsVisible"
      :title="`批次 #${detailRunId} 工资明细`"
      width="960px"
    >
      <el-table v-loading="detailsLoading" :data="details" border stripe height="500">
        <el-table-column prop="empNo" label="工号" width="120" />
        <el-table-column prop="employeeName" label="姓名" width="120" />
        <el-table-column prop="grossPay" label="应发" width="140" />
        <el-table-column prop="socialInsurance" label="社保" width="120" />
        <el-table-column prop="housingFund" label="公积金" width="120" />
        <el-table-column prop="iit" label="个税" width="120" />
        <el-table-column prop="netPay" label="实发" width="140">
          <template #default="{ row }">
            <strong>{{ row.netPay }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="isException" label="异常" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isException" type="danger">是</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.payroll-page {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.section {
  margin-bottom: 16px;
}
.period-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
