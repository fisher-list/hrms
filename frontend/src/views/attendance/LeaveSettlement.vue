<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listSettlements,
  createSettlement,
  getSettlementDetails,
  reverseSettlement,
  queryLeaveBalances,
} from '@/api/leave-settlement';
import type {
  LeaveSettlementVo,
  SettlementDetailVo,
  LeaveBalanceVo,
  CarryStrategy,
  SettlementStatus,
  BalanceQuery,
} from '@/api/leave-settlement';

/* ===== 结算批次列表 ===== */
const settlements = ref<LeaveSettlementVo[]>([]);
const loadingSettlements = ref(false);

const STRATEGY_LABEL: Record<CarryStrategy, string> = {
  CARRY_MAX5: '最多结转5天',
  NONE: '不结转',
  EXPIRE_2M: '过期2个月作废',
};

const STATUS_TAG: Record<SettlementStatus, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
  DRAFT: 'info',
  EXECUTING: 'warning',
  COMPLETED: 'success',
  REVERSED: 'danger',
};

async function loadSettlements(): Promise<void> {
  loadingSettlements.value = true;
  try {
    settlements.value = await listSettlements();
  } catch {
    ElMessage.error('加载结算批次失败');
  } finally {
    loadingSettlements.value = false;
  }
}

/* ===== 发起结算弹窗 ===== */
const createDialogVisible = ref(false);
const createLoading = ref(false);
const createForm = reactive<{ year: number; carryStrategy: CarryStrategy }>({
  year: new Date().getFullYear(),
  carryStrategy: 'CARRY_MAX5',
});

function openCreateDialog(): void {
  createForm.year = new Date().getFullYear();
  createForm.carryStrategy = 'CARRY_MAX5';
  createDialogVisible.value = true;
}

async function onSubmitCreate(): Promise<void> {
  createLoading.value = true;
  try {
    await createSettlement({ year: createForm.year, carryStrategy: createForm.carryStrategy });
    ElMessage.success('结算批次已创建');
    createDialogVisible.value = false;
    await loadSettlements();
  } catch {
    // api error already handled
  } finally {
    createLoading.value = false;
  }
}

/* ===== 结算明细弹窗 ===== */
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailTitle = ref('');
const details = ref<SettlementDetailVo[]>([]);

async function showDetails(row: LeaveSettlementVo): Promise<void> {
  detailTitle.value = `批次 #${row.id} 结算明细 (${row.year}年 / ${STRATEGY_LABEL[row.carryStrategy]})`;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    details.value = await getSettlementDetails(row.id);
  } catch {
    ElMessage.error('加载结算明细失败');
  } finally {
    detailLoading.value = false;
  }
}

/* ===== 冲销结算 ===== */
async function onReverse(row: LeaveSettlementVo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认冲销批次 #${row.id}？冲销后结转/作废数据将回滚。`,
      '冲销确认',
      { type: 'warning' },
    );
    await reverseSettlement(row.id);
    ElMessage.success('已冲销');
    await loadSettlements();
  } catch {
    // cancelled or error
  }
}

/* ===== 假期余额查询 ===== */
const balanceQuery = reactive<BalanceQuery>({
  year: new Date().getFullYear(),
  leaveTypeName: '',
  employeeName: '',
});
const balances = ref<LeaveBalanceVo[]>([]);
const loadingBalances = ref(false);

async function loadBalances(): Promise<void> {
  loadingBalances.value = true;
  try {
    balances.value = await queryLeaveBalances({
      year: balanceQuery.year,
      leaveTypeName: balanceQuery.leaveTypeName || undefined,
      employeeName: balanceQuery.employeeName || undefined,
    });
  } catch {
    ElMessage.error('加载假期余额失败');
  } finally {
    loadingBalances.value = false;
  }
}

onMounted(async () => {
  await Promise.all([loadSettlements(), loadBalances()]);
});
</script>

<template>
  <div class="leave-settlement-page">
    <h2 class="page-title">假期结算</h2>

    <!-- 结算批次列表 -->
    <el-card shadow="never" class="section">
      <template #header>
        <div class="card-header">
          <span>结算批次</span>
          <el-button type="primary" @click="openCreateDialog">发起结算</el-button>
        </div>
      </template>
      <el-table v-loading="loadingSettlements" :data="settlements" border stripe>
        <el-table-column prop="id" label="批次ID" width="100" />
        <el-table-column prop="year" label="年度" width="80" />
        <el-table-column label="结转策略" width="160">
          <template #default="{ row }">
            <el-tag>{{ STRATEGY_LABEL[row.carryStrategy as CarryStrategy] ?? row.carryStrategy }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status as SettlementStatus] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="employeeCount" label="员工数" width="90" />
        <el-table-column prop="carriedDays" label="结转总天数" width="110" />
        <el-table-column prop="expiredDays" label="作废总天数" width="110" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetails(row)">明细</el-button>
            <el-button
              v-if="row.status === 'COMPLETED'"
              type="danger"
              size="small"
              @click="onReverse(row)"
            >
              冲销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 假期余额查询 -->
    <el-card shadow="never" class="section">
      <template #header><span>假期余额查询</span></template>
      <div class="query-bar">
        <el-input-number
          v-model="balanceQuery.year"
          :min="2000"
          :max="2100"
          controls-position="right"
          placeholder="年度"
          style="width: 130px"
        />
        <el-input
          v-model="balanceQuery.leaveTypeName"
          placeholder="假期类型"
          clearable
          style="width: 160px"
        />
        <el-input
          v-model="balanceQuery.employeeName"
          placeholder="员工姓名"
          clearable
          style="width: 160px"
        />
        <el-button type="primary" @click="loadBalances">查询</el-button>
      </div>
      <el-table v-loading="loadingBalances" :data="balances" border stripe style="margin-top: 12px">
        <el-table-column prop="empNo" label="工号" width="110" />
        <el-table-column prop="employeeName" label="姓名" width="100" />
        <el-table-column prop="leaveTypeName" label="假期类型" width="130" />
        <el-table-column prop="year" label="年度" width="80" />
        <el-table-column prop="totalDays" label="总天数" width="100" />
        <el-table-column prop="usedDays" label="已用天数" width="100" />
        <el-table-column prop="remainingDays" label="剩余天数" width="100">
          <template #default="{ row }">
            <strong :class="{ 'text-danger': row.remainingDays <= 0 }">{{ row.remainingDays }}</strong>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发起结算弹窗 -->
    <el-dialog v-model="createDialogVisible" title="发起假期结算" width="480px">
      <el-form label-width="100px">
        <el-form-item label="结算年度">
          <el-input-number
            v-model="createForm.year"
            :min="2000"
            :max="2100"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="结转策略">
          <el-select v-model="createForm.carryStrategy" style="width: 100%">
            <el-option label="最多结转5天" value="CARRY_MAX5" />
            <el-option label="不结转" value="NONE" />
            <el-option label="过期2个月作废" value="EXPIRE_2M" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="onSubmitCreate">
          确认创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 结算明细弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="960px">
      <el-table v-loading="detailLoading" :data="details" border stripe height="480">
        <el-table-column prop="empNo" label="工号" width="110" />
        <el-table-column prop="employeeName" label="姓名" width="100" />
        <el-table-column prop="department" label="部门" width="130" />
        <el-table-column prop="leaveTypeName" label="假期类型" width="120" />
        <el-table-column prop="carriedDays" label="结转天数" width="110">
          <template #default="{ row }">
            <span class="text-success">{{ row.carriedDays }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expiredDays" label="作废天数" width="110">
          <template #default="{ row }">
            <span class="text-danger">{{ row.expiredDays }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.leave-settlement-page {
  padding: 24px;
}
.page-title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.section {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.query-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.text-success {
  color: #67c23a;
  font-weight: 600;
}
.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
