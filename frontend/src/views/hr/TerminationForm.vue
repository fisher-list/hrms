<script setup lang="ts">
/**
 * 离职管理页面。
 * 展示离职申请列表，支持新建离职单、提交审批、查看交接清单。
 */
import { ref, reactive, onMounted, nextTick, computed } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  createTerminationForm,
  submitTerminationForm,
  listTerminationForms,
  listHandoverItems,
  createHandoverItem,
  updateHandoverItemStatus,
} from '@/api/termination';
import type {
  TerminationFormVo,
  TerminationFormCreateReq,
  HandoverItemVo,
  HandoverItemCreateReq,
  TerminationType,
} from '@/api/termination';
import { listEmployees } from '@/api/employee';
import type { EmployeeListVo } from '@/types/hr';

// ===== 离职申请列表 =====
const formList = ref<TerminationFormVo[]>([]);
const total = ref(0);
const loading = ref(false);

const query = reactive({
  status: '',
  current: 1,
  size: 10,
});

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'info',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: '待提交',
  SUBMITTED: '审批中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
};

const TERMINATION_TYPE_OPTIONS: { value: TerminationType; label: string }[] = [
  { value: 'RESIGNATION', label: '辞职' },
  { value: 'DISMISSAL', label: '辞退' },
  { value: 'MUTUAL', label: '协商解除' },
  { value: 'CONTRACT_EXPIRY', label: '合同到期' },
  { value: 'RETIREMENT', label: '退休' },
];

const TERMINATION_TYPE_MAP: Record<string, string> = {
  RESIGNATION: '辞职',
  DISMISSAL: '辞退',
  MUTUAL: '协商解除',
  CONTRACT_EXPIRY: '合同到期',
  RETIREMENT: '退休',
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listTerminationForms(
      query.status || undefined,
      query.current,
      query.size,
    );
    formList.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载离职申请列表失败');
  } finally {
    loading.value = false;
  }
}

async function handleSubmit(row: TerminationFormVo): Promise<void> {
  try {
    await submitTerminationForm(row.id);
    ElMessage.success('已提交审批');
    void fetchList();
  } catch {
    // 拦截器已处理
  }
}

// ===== 员工选择 =====
const employeeList = ref<EmployeeListVo[]>([]);
const employeeLoading = ref(false);
const employeeKeyword = ref('');

async function searchEmployees(keyword?: string): Promise<void> {
  employeeLoading.value = true;
  try {
    const page = await listEmployees(keyword, 'ACTIVE', 1, 100);
    employeeList.value = page.records;
  } catch {
    employeeList.value = [];
  } finally {
    employeeLoading.value = false;
  }
}

function onEmployeeSearch(query: string): void {
  employeeKeyword.value = query;
  void searchEmployees(query || undefined);
}

// ===== 新建离职单弹窗 =====
const dialogRef = ref<FormInstance>();
const dialogVisible = ref(false);
const submitting = ref(false);

const createForm = reactive({
  employeeId: undefined as number | undefined,
  terminationType: 'RESIGNATION' as TerminationType,
  reason: '',
  lastWorkingDay: '',
});

const rules: FormRules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  terminationType: [{ required: true, message: '请选择离职类型', trigger: 'change' }],
  lastWorkingDay: [{ required: true, message: '请选择最后工作日', trigger: 'change' }],
};

function openCreate(): void {
  createForm.employeeId = undefined;
  createForm.terminationType = 'RESIGNATION';
  createForm.reason = '';
  createForm.lastWorkingDay = '';
  dialogVisible.value = true;
  nextTick(() => dialogRef.value?.clearValidate());
  void searchEmployees();
}

async function handleCreate(): Promise<void> {
  if (!dialogRef.value) return;
  const valid = await dialogRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const data: TerminationFormCreateReq = {
      employeeId: createForm.employeeId!,
      terminationType: createForm.terminationType,
      reason: createForm.reason || undefined,
      lastWorkingDay: createForm.lastWorkingDay,
    };

    const form = await createTerminationForm(data);
    // 自动提交审批
    await submitTerminationForm(form.id);
    ElMessage.success('离职申请已创建并提交审批');
    dialogVisible.value = false;
    void fetchList();
  } catch {
    // 拦截器已处理
  } finally {
    submitting.value = false;
  }
}

// ===== 交接清单抽屉 =====
const handoverDrawerVisible = ref(false);
const handoverFormId = ref<number | null>(null);
const handoverItems = ref<HandoverItemVo[]>([]);
const handoverLoading = ref(false);

const HANDOVER_CATEGORIES = [
  { value: 'ASSET', label: '资产归还' },
  { value: 'DOCUMENT', label: '文档资料' },
  { value: 'PROJECT', label: '项目交接' },
  { value: 'ACCOUNT', label: '账号权限' },
  { value: 'OTHER', label: '其他' },
];

const HANDOVER_STATUS_TAG: Record<string, 'success' | 'warning' | 'info'> = {
  PENDING: 'warning',
  COMPLETED: 'success',
};

// 新建交接项
const handoverDialogVisible = ref(false);
const handoverFormRef = ref<FormInstance>();
const handoverForm = reactive({
  category: 'ASSET',
  description: '',
  handoverTo: '',
  remark: '',
});

const handoverRules: FormRules = {
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  description: [{ required: true, message: '请输入交接内容', trigger: 'blur' }],
};

async function openHandover(row: TerminationFormVo): Promise<void> {
  handoverFormId.value = row.id;
  handoverDrawerVisible.value = true;
  await fetchHandoverItems();
}

async function fetchHandoverItems(): Promise<void> {
  if (!handoverFormId.value) return;
  handoverLoading.value = true;
  try {
    handoverItems.value = await listHandoverItems(handoverFormId.value);
  } catch {
    handoverItems.value = [];
  } finally {
    handoverLoading.value = false;
  }
}

function openAddHandover(): void {
  handoverForm.category = 'ASSET';
  handoverForm.description = '';
  handoverForm.handoverTo = '';
  handoverForm.remark = '';
  handoverDialogVisible.value = true;
  nextTick(() => handoverFormRef.value?.clearValidate());
}

async function handleAddHandover(): Promise<void> {
  if (!handoverFormRef.value) return;
  const valid = await handoverFormRef.value.validate().catch(() => false);
  if (!valid) return;

  try {
    const data: HandoverItemCreateReq = {
      formId: handoverFormId.value!,
      category: handoverForm.category,
      description: handoverForm.description,
      handoverTo: handoverForm.handoverTo || undefined,
      remark: handoverForm.remark || undefined,
    };
    await createHandoverItem(data);
    ElMessage.success('交接项已添加');
    handoverDialogVisible.value = false;
    await fetchHandoverItems();
  } catch {
    // 拦截器已处理
  }
}

async function handleCompleteHandover(item: HandoverItemVo): Promise<void> {
  try {
    await ElMessageBox.confirm('确认将该交接项标记为已完成？', '确认');
    await updateHandoverItemStatus(item.id, 'COMPLETED');
    ElMessage.success('已标记完成');
    await fetchHandoverItems();
  } catch {
    // 用户取消或请求失败
  }
}

// ===== 辅助方法 =====
function getEmployeeName(employeeId: number): string {
  const emp = employeeList.value.find((e) => e.id === employeeId);
  return emp ? `${emp.name}（${emp.empNo}）` : `ID: ${employeeId}`;
}

const handoverPendingCount = computed(() =>
  handoverItems.value.filter((i) => i.status === 'PENDING').length,
);

onMounted(() => {
  void fetchList();
  void searchEmployees();
});
</script>

<template>
  <div class="termination-form-page">
    <div class="header">
      <h2 class="title">离职管理</h2>
      <el-button type="primary" @click="openCreate">新建离职单</el-button>
    </div>

    <!-- 筛选栏 -->
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" style="width: 160px" @change="fetchList">
          <el-option value="" label="全部" />
          <el-option value="PENDING" label="待提交" />
          <el-option value="SUBMITTED" label="审批中" />
          <el-option value="APPROVED" label="已通过" />
          <el-option value="REJECTED" label="已拒绝" />
        </el-select>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="formList" border stripe>
      <el-table-column prop="formNo" label="申请单号" width="220" />
      <el-table-column label="员工" width="180">
        <template #default="{ row }">
          {{ getEmployeeName(row.employeeId) }}
        </template>
      </el-table-column>
      <el-table-column label="离职类型" width="140">
        <template #default="{ row }">
          {{ TERMINATION_TYPE_MAP[row.terminationType] ?? row.terminationType }}
        </template>
      </el-table-column>
      <el-table-column prop="lastWorkingDay" label="最后工作日" width="140" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">
            {{ STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" min-width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            type="primary"
            size="small"
            @click="handleSubmit(row)"
          >提交审批</el-button>
          <el-button
            v-if="row.status !== 'REJECTED'"
            type="info"
            size="small"
            @click="openHandover(row)"
          >交接清单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="fetchList"
    />

    <!-- 新建离职单弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建离职单" width="600px">
      <el-form ref="dialogRef" :model="createForm" :rules="rules" label-width="120px">
        <el-form-item label="离职员工" prop="employeeId">
          <el-select
            v-model="createForm.employeeId"
            placeholder="请输入姓名或工号搜索"
            filterable
            remote
            :remote-method="onEmployeeSearch"
            :loading="employeeLoading"
            style="width: 100%"
          >
            <el-option
              v-for="emp in employeeList"
              :key="emp.id"
              :value="emp.id"
              :label="`${emp.name}（${emp.empNo}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="离职类型" prop="terminationType">
          <el-select v-model="createForm.terminationType" style="width: 100%">
            <el-option
              v-for="opt in TERMINATION_TYPE_OPTIONS"
              :key="opt.value"
              :value="opt.value"
              :label="opt.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="最后工作日" prop="lastWorkingDay">
          <el-date-picker
            v-model="createForm.lastWorkingDay"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="离职原因">
          <el-input
            v-model="createForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入离职原因（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建并提交审批</el-button>
      </template>
    </el-dialog>

    <!-- 交接清单抽屉 -->
    <el-drawer
      v-model="handoverDrawerVisible"
      title="工作交接清单"
      size="680px"
    >
      <div class="handover-header">
        <span v-if="handoverPendingCount > 0" class="handover-count">
          待完成：{{ handoverPendingCount }} 项
        </span>
        <span v-else class="handover-count done">全部完成 ✓</span>
        <el-button type="primary" size="small" @click="openAddHandover">添加交接项</el-button>
      </div>

      <el-table v-loading="handoverLoading" :data="handoverItems" border stripe>
        <el-table-column label="类别" width="100">
          <template #default="{ row }">
            {{ HANDOVER_CATEGORIES.find(c => c.value === row.category)?.label ?? row.category }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="交接内容" min-width="180" />
        <el-table-column prop="handoverTo" label="交接人" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="HANDOVER_STATUS_TAG[row.status] ?? 'info'" size="small">
              {{ row.status === 'COMPLETED' ? '已完成' : '待完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              link
              @click="handleCompleteHandover(row)"
            >标记完成</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 添加交接项弹窗 -->
      <el-dialog v-model="handoverDialogVisible" title="添加交接项" width="480px" append-to-body>
        <el-form ref="handoverFormRef" :model="handoverForm" :rules="handoverRules" label-width="100px">
          <el-form-item label="类别" prop="category">
            <el-select v-model="handoverForm.category" style="width: 100%">
              <el-option
                v-for="cat in HANDOVER_CATEGORIES"
                :key="cat.value"
                :value="cat.value"
                :label="cat.label"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="交接内容" prop="description">
            <el-input v-model="handoverForm.description" placeholder="请输入交接内容" />
          </el-form-item>
          <el-form-item label="交接人">
            <el-input v-model="handoverForm.handoverTo" placeholder="请输入交接接收人" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="handoverForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="handoverDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddHandover">确认添加</el-button>
        </template>
      </el-dialog>
    </el-drawer>
  </div>
</template>

<style scoped>
.termination-form-page {
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
.filter-bar {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.handover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.handover-count {
  font-size: 14px;
  color: #e6a23c;
  font-weight: 500;
}
.handover-count.done {
  color: #67c23a;
}
</style>
