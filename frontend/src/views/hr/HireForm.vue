<script setup lang="ts">
/**
 * 入职管理页面。
 * 展示入职申请列表，支持新建入职单、提交审批。
 */
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  createHireForm,
  submitHireForm,
  listHireForms,
} from '@/api/employee';
import type { HireFormVo, HireFormCreateReq } from '@/api/employee';
import { getCompany, getDeptTree, listPositions } from '@/api/org';
import type { DepartmentTreeVo, PositionVo } from '@/types/org-payroll';

// ===== 入职申请列表 =====
const formList = ref<HireFormVo[]>([]);
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

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listHireForms(
      query.status || undefined,
      query.current,
      query.size,
    );
    formList.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载入职申请列表失败');
  } finally {
    loading.value = false;
  }
}

async function handleSubmit(row: HireFormVo): Promise<void> {
  try {
    await submitHireForm(row.id);
    ElMessage.success('已提交审批');
    void fetchList();
  } catch {
    // 拦截器已处理
  }
}

// ===== 新建入职单弹窗 =====
const dialogRef = ref<FormInstance>();
const dialogVisible = ref(false);
const submitting = ref(false);

const createForm = reactive({
  name: '',
  gender: 'M',
  birthDate: '',
  email: '',
  idCard: '',
  phone: '',
  deptId: undefined as number | undefined,
  positionId: undefined as number | undefined,
  hireDate: '',
  contractStart: '',
  contractEnd: '',
  probationEnd: '',
  emergencyContact: '',
  emergencyPhone: '',
});

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  positionId: [{ required: true, message: '请选择岗位', trigger: 'change' }],
  hireDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
};

// 部门列表（扁平化）
const positionList = ref<PositionVo[]>([]);

function flattenDepts(nodes: DepartmentTreeVo[], prefix = ''): { id: number; label: string }[] {
  const result: { id: number; label: string }[] = [];
  for (const n of nodes) {
    const label = prefix ? `${prefix} / ${n.name}` : n.name;
    result.push({ id: n.id, label });
    if (n.children && n.children.length) {
      result.push(...flattenDepts(n.children, label));
    }
  }
  return result;
}

const flatDepts = ref<{ id: number; label: string }[]>([]);

async function loadDepts(): Promise<void> {
  try {
    const company = await getCompany();
    const tree = await getDeptTree(company.id);
    flatDepts.value = flattenDepts(tree);
  } catch {
    ElMessage.error('加载部门列表失败');
  }
}

async function loadPositions(): Promise<void> {
  try {
    positionList.value = await listPositions(createForm.deptId ?? undefined);
  } catch {
    positionList.value = [];
  }
}

function onDeptChange(): void {
  createForm.positionId = undefined;
  void loadPositions();
}

function openCreate(): void {
  createForm.name = '';
  createForm.gender = 'M';
  createForm.birthDate = '';
  createForm.email = '';
  createForm.idCard = '';
  createForm.phone = '';
  createForm.deptId = undefined;
  createForm.positionId = undefined;
  createForm.hireDate = '';
  createForm.contractStart = '';
  createForm.contractEnd = '';
  createForm.probationEnd = '';
  createForm.emergencyContact = '';
  createForm.emergencyPhone = '';
  dialogVisible.value = true;
  nextTick(() => dialogRef.value?.clearValidate());
  void loadDepts();
}

async function handleCreate(): Promise<void> {
  if (!dialogRef.value) return;
  const valid = await dialogRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const snapshot = JSON.stringify({
      name: createForm.name,
      gender: createForm.gender,
      birthDate: createForm.birthDate || undefined,
      email: createForm.email || undefined,
      idCard: createForm.idCard || undefined,
      phone: createForm.phone || undefined,
      contractStart: createForm.contractStart || undefined,
      contractEnd: createForm.contractEnd || undefined,
      probationEnd: createForm.probationEnd || undefined,
      emergencyContact: createForm.emergencyContact || undefined,
      emergencyPhone: createForm.emergencyPhone || undefined,
    });

    const data: HireFormCreateReq = {
      employeeSnapshot: snapshot,
      positionId: createForm.positionId!,
      deptId: createForm.deptId!,
      hireDate: createForm.hireDate,
      accountName: createForm.name,
    };

    const form = await createHireForm(data);
    // 自动提交审批
    await submitHireForm(form.id);
    ElMessage.success('入职申请已创建并提交审批');
    dialogVisible.value = false;
    void fetchList();
  } catch {
    // 拦截器已处理
  } finally {
    submitting.value = false;
  }
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="hire-form-page">
    <div class="header">
      <h2 class="title">入职管理</h2>
      <el-button type="primary" @click="openCreate">新建入职单</el-button>
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
      <el-table-column prop="accountName" label="姓名" width="120" />
      <el-table-column prop="hireDate" label="入职日期" width="140" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" min-width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            type="primary"
            size="small"
            @click="handleSubmit(row)"
          >提交审批</el-button>
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

    <!-- 新建入职单弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建入职单" width="680px">
      <el-form ref="dialogRef" :model="createForm" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="createForm.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="createForm.gender" style="width: 100%">
                <el-option value="M" label="男" />
                <el-option value="F" label="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="createForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="createForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="createForm.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="createForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-select v-model="createForm.deptId" placeholder="请选择部门" filterable style="width: 100%" @change="onDeptChange">
                <el-option v-for="d in flatDepts" :key="d.id" :value="d.id" :label="d.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位" prop="positionId">
              <el-select v-model="createForm.positionId" placeholder="请选择岗位" filterable style="width: 100%">
                <el-option v-for="p in positionList" :key="p.id" :value="p.id" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="入职日期" prop="hireDate">
              <el-date-picker v-model="createForm.hireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试用期截止日">
              <el-date-picker v-model="createForm.probationEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="合同开始日期">
              <el-date-picker v-model="createForm.contractStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同结束日期">
              <el-date-picker v-model="createForm.contractEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="createForm.emergencyContact" placeholder="请输入紧急联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系电话">
              <el-input v-model="createForm.emergencyPhone" placeholder="请输入紧急联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建并提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.hire-form-page {
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
</style>
