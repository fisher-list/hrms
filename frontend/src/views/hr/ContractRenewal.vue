<script setup lang="ts">
/**
 * 合同续签预警页面。
 * 展示即将到期的合同列表，红色(URGENT)标记7天内到期，黄色(WARNING)标记30天内到期。
 * 支持创建续签申请并提交审批。
 */
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  listExpiringContracts,
  createContractRenewal,
  submitContractRenewal,
  listContractRenewals,
} from '@/api/employee';
import type {
  ExpiringContractVo,
  ContractRenewalVo,
} from '@/api/employee';

// ===== 预警列表 =====
const expiringList = ref<ExpiringContractVo[]>([]);
const loading = ref(false);
const daysAhead = ref(30);

async function fetchExpiring(): Promise<void> {
  loading.value = true;
  try {
    expiringList.value = await listExpiringContracts(daysAhead.value);
  } catch {
    ElMessage.error('加载合同预警列表失败');
  } finally {
    loading.value = false;
  }
}

// ===== 创建续签申请弹窗 =====
const createVisible = ref(false);
const createForm = reactive({
  employeeId: null as number | null,
  originalContractId: null as number | null,
  newContractType: 'FIXED',
  newStartDate: '',
  newEndDate: '',
  remark: '',
});

function openCreateDialog(row: ExpiringContractVo): void {
  createForm.employeeId = row.employeeId;
  createForm.originalContractId = row.contractId;
  createForm.newContractType = 'FIXED';
  // 默认新合同从原合同结束日次日开始
  if (row.contractEndDate) {
    const end = new Date(row.contractEndDate);
    end.setDate(end.getDate() + 1);
    createForm.newStartDate = end.toISOString().slice(0, 10);
    // 默认续签3年
    const newEnd = new Date(end);
    newEnd.setFullYear(newEnd.getFullYear() + 3);
    newEnd.setDate(newEnd.getDate() - 1);
    createForm.newEndDate = newEnd.toISOString().slice(0, 10);
  }
  createForm.remark = '';
  createVisible.value = true;
}

async function handleCreate(): Promise<void> {
  if (!createForm.employeeId || !createForm.originalContractId || !createForm.newStartDate) {
    ElMessage.warning('请填写必填项');
    return;
  }
  try {
    const renewal = await createContractRenewal({
      employeeId: createForm.employeeId,
      originalContractId: createForm.originalContractId,
      newContractType: createForm.newContractType,
      newStartDate: createForm.newStartDate,
      newEndDate: createForm.newEndDate || undefined,
      remark: createForm.remark || undefined,
    });
    // 自动提交审批
    await submitContractRenewal(renewal.id);
    ElMessage.success('续签申请已提交审批');
    createVisible.value = false;
    void fetchExpiring();
    void fetchRenewals();
  } catch {
    // 错误已由拦截器处理
  }
}

// ===== 续签申请列表 =====
const renewalList = ref<ContractRenewalVo[]>([]);
const renewalTotal = ref(0);
const renewalQuery = reactive({ status: '', current: 1, size: 10 });
const renewalLoading = ref(false);

const RENEWAL_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'info',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

async function fetchRenewals(): Promise<void> {
  renewalLoading.value = true;
  try {
    const page = await listContractRenewals(
      renewalQuery.status || undefined,
      renewalQuery.current,
      renewalQuery.size,
    );
    renewalList.value = page.records;
    renewalTotal.value = page.total;
  } catch {
    ElMessage.error('加载续签申请列表失败');
  } finally {
    renewalLoading.value = false;
  }
}

onMounted(() => {
  void fetchExpiring();
  void fetchRenewals();
});
</script>

<template>
  <div class="contract-renewal-page">
    <h2 class="title">合同续签管理</h2>

    <!-- 预警列表 -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600">⚠️ 合同到期预警</span>
          <div>
            <el-select v-model="daysAhead" style="width: 120px; margin-right: 8px" @change="fetchExpiring">
              <el-option :value="7" label="7天内" />
              <el-option :value="15" label="15天内" />
              <el-option :value="30" label="30天内" />
              <el-option :value="60" label="60天内" />
              <el-option :value="90" label="90天内" />
            </el-select>
            <el-button type="primary" @click="fetchExpiring">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="expiringList" border stripe>
        <el-table-column prop="empNo" label="工号" width="100" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="contractNo" label="合同编号" width="160" />
        <el-table-column prop="contractType" label="合同类型" width="100" />
        <el-table-column prop="contractStartDate" label="合同开始" width="120" />
        <el-table-column prop="contractEndDate" label="合同结束" width="120" />
        <el-table-column prop="daysUntilExpiry" label="剩余天数" width="100">
          <template #default="{ row }">
            <el-tag :type="row.alertLevel === 'URGENT' ? 'danger' : 'warning'" effect="dark">
              {{ row.daysUntilExpiry <= 0 ? '已过期' : row.daysUntilExpiry + '天' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预警等级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.alertLevel === 'URGENT' ? 'danger' : 'warning'">
              {{ row.alertLevel === 'URGENT' ? '🔴 紧急' : '🟡 预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openCreateDialog(row)">续签</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && expiringList.length === 0" description="暂无即将到期的合同" />
    </el-card>

    <!-- 续签申请列表 -->
    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600">续签申请记录</span>
      </template>

      <el-table v-loading="renewalLoading" :data="renewalList" border stripe>
        <el-table-column prop="renewalNo" label="申请单号" width="200" />
        <el-table-column prop="employeeId" label="员工ID" width="100" />
        <el-table-column prop="newContractType" label="新合同类型" width="120" />
        <el-table-column prop="newStartDate" label="新合同开始" width="120" />
        <el-table-column prop="newEndDate" label="新合同结束" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="RENEWAL_STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" min-width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="renewalQuery.current"
        v-model:page-size="renewalQuery.size"
        :total="renewalTotal"
        layout="total, prev, pager, next"
        style="margin-top: 16px; display: flex; justify-content: flex-end"
        @current-change="fetchRenewals"
      />
    </el-card>

    <!-- 创建续签申请弹窗 -->
    <el-dialog v-model="createVisible" title="创建续签申请" width="500px">
      <el-form :model="createForm" label-width="120px">
        <el-form-item label="新合同类型">
          <el-select v-model="createForm.newContractType" style="width: 100%">
            <el-option value="FIXED" label="固定期限" />
            <el-option value="PERMANENT" label="无固定期限" />
          </el-select>
        </el-form-item>
        <el-form-item label="新合同开始日期">
          <el-date-picker v-model="createForm.newStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="createForm.newContractType === 'FIXED'" label="新合同结束日期">
          <el-date-picker v-model="createForm.newEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="续签备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="请输入续签原因或备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建并提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.contract-renewal-page {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
