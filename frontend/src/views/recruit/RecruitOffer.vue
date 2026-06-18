<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  listCandidates,
  createCandidate,
  createOffer,
  submitOffer,
  acceptOffer,
  declineOffer,
} from '@/api/recruit';
import type { CandidateVo } from '@/types/talent';

const candidates = ref<CandidateVo[]>([]);
const total = ref(0);
const current = ref(1);
const size = ref(10);
const loading = ref(false);

const candidateDialog = ref(false);
const candidateFormRef = ref<FormInstance>();
const candidateForm = reactive({
  name: '',
  phone: '',
  email: '',
  idCardEnc: '',
  expectedSalary: undefined as number | undefined,
  jobRequisitionId: undefined as number | undefined,
});

const offerDialog = ref(false);
const offerFormRef = ref<FormInstance>();
const offerCandidate = ref<CandidateVo | null>(null);
const offerForm = reactive({
  salary: undefined as number | undefined,
  onboardDate: '',
});

const candidateRules: FormRules = {
  name: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }],
};

const offerRules: FormRules = {
  salary: [{ required: true, message: '请输入 Offer 薪资', trigger: 'blur' }],
  onboardDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
};

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  NEW: 'info',
  INTERVIEW: 'warning',
  OFFER: 'warning',
  ACCEPTED: 'success',
  REJECTED: 'danger',
};

async function fetchCandidates(): Promise<void> {
  loading.value = true;
  try {
    const page = await listCandidates(current.value, size.value);
    candidates.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载候选人失败');
  } finally {
    loading.value = false;
  }
}

function openCandidateDialog(): void {
  candidateForm.name = '';
  candidateForm.phone = '';
  candidateForm.email = '';
  candidateForm.idCardEnc = '';
  candidateForm.expectedSalary = undefined;
  candidateForm.jobRequisitionId = undefined;
  candidateDialog.value = true;
}

async function onCreateCandidate(): Promise<void> {
  const valid = await candidateFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  try {
    await createCandidate({ ...candidateForm });
    ElMessage.success('候选人已创建');
    candidateDialog.value = false;
    void fetchCandidates();
  } catch {
    // backend toast already shown
  }
}

function openOfferDialog(row: CandidateVo): void {
  offerCandidate.value = row;
  offerForm.salary = row.expectedSalary;
  offerForm.onboardDate = '';
  offerDialog.value = true;
}

async function onCreateOffer(): Promise<void> {
  const valid = await offerFormRef.value?.validate().catch(() => false);
  if (!valid || !offerCandidate.value || offerForm.salary == null) return;
  try {
    const offer = await createOffer({
      candidateId: offerCandidate.value.id,
      jobRequisitionId: offerCandidate.value.jobRequisitionId,
      salary: offerForm.salary,
      onboardDate: offerForm.onboardDate,
    });
    await submitOffer(offer.id);
    ElMessage.success('Offer 已创建并提交审批');
    offerDialog.value = false;
    void fetchCandidates();
  } catch {
    // ignore
  }
}

async function onAcceptOffer(_row: CandidateVo): Promise<void> {
  try {
    const offerId = await ElMessageBox.prompt('请输入 Offer ID', '接受 Offer', {
      confirmButtonText: '接受',
      cancelButtonText: '取消',
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入数字 ID',
    });
    await acceptOffer(Number(offerId.value));
    ElMessage.success('Offer 已接受，已生成待入职员工');
    void fetchCandidates();
  } catch {
    // ignore
  }
}

async function onDeclineOffer(_row: CandidateVo): Promise<void> {
  try {
    const offerId = await ElMessageBox.prompt('请输入 Offer ID', '拒绝 Offer', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入数字 ID',
    });
    await declineOffer(Number(offerId.value));
    ElMessage.success('Offer 已拒绝');
    void fetchCandidates();
  } catch {
    // ignore
  }
}

onMounted(() => void fetchCandidates());
</script>

<template>
  <div class="recruit-page">
    <div class="header">
      <h2 class="title">招聘与 Offer</h2>
      <el-button type="primary" @click="openCandidateDialog">新增候选人</el-button>
    </div>

    <el-table v-loading="loading" :data="candidates" border stripe>
      <el-table-column prop="id" label="候选人ID" width="120" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="expectedSalary" label="期望薪资" width="140" />
      <el-table-column prop="jobRequisitionId" label="职位需求ID" width="130" />
      <el-table-column prop="currentStatus" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.currentStatus] ?? 'info'">
            {{ row.currentStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openOfferDialog(row)">
            创建 Offer
          </el-button>
          <el-button type="success" size="small" @click="onAcceptOffer(row)">
            接受
          </el-button>
          <el-button type="danger" size="small" @click="onDeclineOffer(row)">
            拒绝
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="current"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="fetchCandidates"
      @current-change="fetchCandidates"
    />

    <el-dialog v-model="candidateDialog" title="新增候选人" width="560px">
      <el-form ref="candidateFormRef" :model="candidateForm" :rules="candidateRules" label-width="110px">
        <el-form-item label="姓名" prop="name"><el-input v-model="candidateForm.name" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="candidateForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="candidateForm.email" /></el-form-item>
        <el-form-item label="身份证"><el-input v-model="candidateForm.idCardEnc" /></el-form-item>
        <el-form-item label="期望薪资">
          <el-input-number v-model="candidateForm.expectedSalary" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="职位需求ID">
          <el-input-number v-model="candidateForm.jobRequisitionId" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="candidateDialog = false">取消</el-button>
        <el-button type="primary" @click="onCreateCandidate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="offerDialog" title="创建 Offer" width="520px">
      <el-form ref="offerFormRef" :model="offerForm" :rules="offerRules" label-width="100px">
        <el-form-item label="候选人">
          <el-input :model-value="offerCandidate?.name" disabled />
        </el-form-item>
        <el-form-item label="Offer薪资" prop="salary">
          <el-input-number v-model="offerForm.salary" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="入职日期" prop="onboardDate">
          <el-date-picker v-model="offerForm.onboardDate" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offerDialog = false">取消</el-button>
        <el-button type="primary" @click="onCreateOffer">创建并提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.recruit-page {
  padding: 24px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
  display: flex;
}
</style>
