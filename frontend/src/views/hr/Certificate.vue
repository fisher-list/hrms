<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  applyCertificate,
  myCertificates,
  listCertificates,
  issueCertificate,
  type CertificateVo,
  type CertificateCreateReq,
} from '@/api/certificate';


const certificates = ref<CertificateVo[]>([]);
const loading = ref(false);
const filterStatus = ref<string>();

// 申请对话框
const applyDialog = ref(false);
const applyForm = ref<CertificateCreateReq>({
  type: 'EMPLOYMENT',
  purpose: '',
  copies: 1,
  incomeStartDate: undefined,
  incomeEndDate: undefined,
});

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', ISSUED: 'info',
};

const TYPE_LABEL: Record<string, string> = {
  EMPLOYMENT: '在职证明', INCOME: '收入证明',
};

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    certificates.value = await listCertificates({ status: filterStatus.value || undefined });
  } catch {
    ElMessage.error('加载证明列表失败');
  } finally {
    loading.value = false;
  }
}

async function loadMyData(): Promise<void> {
  loading.value = true;
  try {
    certificates.value = await myCertificates(filterStatus.value || undefined);
  } catch {
    ElMessage.error('加载我的证明失败');
  } finally {
    loading.value = false;
  }
}

async function onApply(): Promise<void> {
  if (!applyForm.value.purpose) {
    ElMessage.warning('请填写用途');
    return;
  }
  if (applyForm.value.type === 'INCOME' && (!applyForm.value.incomeStartDate || !applyForm.value.incomeEndDate)) {
    ElMessage.warning('收入证明需要选择起止日期');
    return;
  }
  try {
    await applyCertificate(applyForm.value);
    ElMessage.success('申请已提交，等待审批');
    applyDialog.value = false;
    await loadMyData();
  } catch { /* ignore */ }
}

async function onIssue(row: CertificateVo): Promise<void> {
  try {
    await ElMessageBox.confirm('确认签发该证明？', '签发确认', { type: 'warning' });
    await issueCertificate(row.id);
    ElMessage.success('证明已签发');
    await loadData();
  } catch { /* ignore */ }
}

function openApply(): void {
  applyForm.value = { type: 'EMPLOYMENT', purpose: '', copies: 1, incomeStartDate: undefined, incomeEndDate: undefined };
  applyDialog.value = true;
}

onMounted(async () => {
  await loadData();
});
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h2 class="title">证明开具</h2>
      <div>
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px; margin-right: 8px;" @change="loadData">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已签发" value="ISSUED" />
        </el-select>
        <el-button type="primary" @click="openApply">申请证明</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="certificates" border stripe>
        <el-table-column prop="employeeName" label="员工" width="120" />
        <el-table-column prop="empNo" label="工号" width="120" />
        <el-table-column prop="type" label="证明类型" width="120">
          <template #default="{ row }">{{ TYPE_LABEL[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" min-width="200" />
        <el-table-column prop="copies" label="份数" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="issuedAt" label="签发时间" width="180" />
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'APPROVED'" size="small" type="success" @click="onIssue(row)">签发</el-button>
            <el-button v-if="row.status === 'ISSUED'" size="small" type="primary">
              <a :href="`/api/certificates/${row.id}/download`" target="_blank" style="color: #fff; text-decoration: none;">下载</a>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 申请证明对话框 -->
    <el-dialog v-model="applyDialog" title="申请证明" width="560px">
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="证明类型">
          <el-radio-group v-model="applyForm.type">
            <el-radio label="EMPLOYMENT">在职证明</el-radio>
            <el-radio label="INCOME">收入证明</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="applyForm.purpose" placeholder="请填写用途，如：办理签证、银行贷款等" />
        </el-form-item>
        <el-form-item label="份数">
          <el-input-number v-model="applyForm.copies" :min="1" :max="10" />
        </el-form-item>
        <template v-if="applyForm.type === 'INCOME'">
          <el-form-item label="收入起始">
            <el-date-picker v-model="applyForm.incomeStartDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="收入截止">
            <el-date-picker v-model="applyForm.incomeEndDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button type="primary" @click="onApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; }
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.title { margin: 0; font-size: 18px; font-weight: 600; }
</style>
