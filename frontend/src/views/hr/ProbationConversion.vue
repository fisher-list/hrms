<script setup lang="ts">
/**
 * 试用期转正管理页面。
 * 展示即将到期试用期员工列表，红色(URGENT)标记7天内到期，黄色(WARNING)标记30天内到期。
 * 支持创建转正申请并提交审批。
 */
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  listExpiringProbations,
  createProbationConversion,
  submitProbationConversion,
  listProbationConversions,
} from '@/api/employee';
import type {
  ProbationEmployeeVo,
  ProbationConversionVo,
} from '@/api/employee';

// ===== 预警列表 =====
const expiringList = ref<ProbationEmployeeVo[]>([]);
const loading = ref(false);
const daysAhead = ref(30);

async function fetchExpiring(): Promise<void> {
  loading.value = true;
  try {
    expiringList.value = await listExpiringProbations(daysAhead.value);
  } catch {
    ElMessage.error('加载试用期员工列表失败');
  } finally {
    loading.value = false;
  }
}

// ===== 创建转正申请弹窗 =====
const createVisible = ref(false);
const createForm = reactive({
  employeeId: null as number | null,
  evaluationRemark: '',
  plannedConversionDate: '',
  evaluationScore: null as number | null,
});

function openCreateDialog(row: ProbationEmployeeVo): void {
  createForm.employeeId = row.employeeId;
  createForm.evaluationRemark = '';
  createForm.plannedConversionDate = row.probationEndDate || '';
  createForm.evaluationScore = null;
  createVisible.value = true;
}

async function handleCreate(): Promise<void> {
  if (!createForm.employeeId) {
    ElMessage.warning('请选择员工');
    return;
  }
  try {
    const conversion = await createProbationConversion({
      employeeId: createForm.employeeId,
      evaluationRemark: createForm.evaluationRemark || undefined,
      plannedConversionDate: createForm.plannedConversionDate || undefined,
      evaluationScore: createForm.evaluationScore ?? undefined,
    });
    // 自动提交审批
    await submitProbationConversion(conversion.id);
    ElMessage.success('转正申请已提交审批');
    createVisible.value = false;
    void fetchExpiring();
    void fetchConversions();
  } catch {
    // 错误已由拦截器处理
  }
}

// ===== 转正申请列表 =====
const conversionList = ref<ProbationConversionVo[]>([]);
const conversionTotal = ref(0);
const conversionQuery = reactive({ status: '', current: 1, size: 10 });
const conversionLoading = ref(false);

const CONVERSION_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'info',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

async function fetchConversions(): Promise<void> {
  conversionLoading.value = true;
  try {
    const page = await listProbationConversions(
      conversionQuery.status || undefined,
      conversionQuery.current,
      conversionQuery.size,
    );
    conversionList.value = page.records;
    conversionTotal.value = page.total;
  } catch {
    ElMessage.error('加载转正申请列表失败');
  } finally {
    conversionLoading.value = false;
  }
}

onMounted(() => {
  void fetchExpiring();
  void fetchConversions();
});
</script>

<template>
  <div class="probation-conversion-page">
    <h2 class="title">试用期转正管理</h2>

    <!-- 预警列表 -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600">⚠️ 试用期到期预警</span>
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
        <el-table-column prop="hireDate" label="入职日期" width="120" />
        <el-table-column prop="probationEndDate" label="试用期结束" width="120" />
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
        <el-table-column label="已有申请" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.hasPendingConversion" type="warning">待处理</el-tag>
            <el-tag v-else type="success">无</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="row.hasPendingConversion"
              @click="openCreateDialog(row)"
            >
              申请转正
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && expiringList.length === 0" description="暂无即将到期的试用期员工" />
    </el-card>

    <!-- 转正申请列表 -->
    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600">转正申请记录</span>
      </template>

      <el-table v-loading="conversionLoading" :data="conversionList" border stripe>
        <el-table-column prop="conversionNo" label="申请单号" width="200" />
        <el-table-column prop="employeeId" label="员工ID" width="100" />
        <el-table-column prop="probationStartDate" label="试用期开始" width="120" />
        <el-table-column prop="probationEndDate" label="试用期结束" width="120" />
        <el-table-column prop="plannedConversionDate" label="计划转正日期" width="120" />
        <el-table-column prop="evaluationScore" label="评估得分" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="CONVERSION_STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" min-width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="conversionQuery.current"
        v-model:page-size="conversionQuery.size"
        :total="conversionTotal"
        layout="total, prev, pager, next"
        style="margin-top: 16px; display: flex; justify-content: flex-end"
        @current-change="fetchConversions"
      />
    </el-card>

    <!-- 创建转正申请弹窗 -->
    <el-dialog v-model="createVisible" title="创建转正申请" width="500px">
      <el-form :model="createForm" label-width="120px">
        <el-form-item label="计划转正日期">
          <el-date-picker v-model="createForm.plannedConversionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评估得分">
          <el-input-number v-model="createForm.evaluationScore" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="自评说明">
          <el-input v-model="createForm.evaluationRemark" type="textarea" :rows="4" placeholder="请输入试用期工作自评" />
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
.probation-conversion-page {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
