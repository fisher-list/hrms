<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import {
  listEmployees,
  getEmployee,
  batchImportEmployees,
  exportEmployees,
} from '@/api/employee';
import type { BatchImportResultVo } from '@/api/employee';
import type { EmployeeListVo } from '@/types/hr';

const list = ref<EmployeeListVo[]>([]);
const total = ref(0);
const loading = ref(false);

const query = reactive({
  keyword: '',
  status: '',
  current: 1,
  size: 10,
});

const STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'PENDING_HIRE', label: '待入职' },
  { value: 'PROBATION', label: '试用期' },
  { value: 'ACTIVE', label: '在职' },
  { value: 'ON_LEAVE', label: '请假中' },
  { value: 'TERMINATED', label: '已离职' },
];

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  ACTIVE: 'success',
  PROBATION: 'warning',
  ON_LEAVE: 'info',
  PENDING_HIRE: 'info',
  TERMINATED: 'danger',
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listEmployees(
      query.keyword || undefined,
      query.status || undefined,
      query.current,
      query.size,
    );
    list.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载员工列表失败');
  } finally {
    loading.value = false;
  }
}

function onSearch(): void {
  query.current = 1;
  void fetchList();
}

function onReset(): void {
  query.keyword = '';
  query.status = '';
  query.current = 1;
  void fetchList();
}

const detailVisible = ref(false);
const detail = ref<Record<string, unknown> | null>(null);

// 子表数据
const educationList = ref<Record<string, unknown>[]>([]);
const workExpList = ref<Record<string, unknown>[]>([]);
const contractList = ref<Record<string, unknown>[]>([]);

// 基础信息字段（排除子表和复杂对象）
const BASIC_FIELDS: Record<string, string> = {
  empNo: '工号',
  name: '姓名',
  gender: '性别',
  birthDate: '出生日期',
  email: '邮箱',
  deptName: '部门',
  hireDate: '入职日期',
  status: '状态',
  contractStart: '合同开始',
  contractEnd: '合同结束',
  probationEnd: '试用期结束',
  emergencyContact: '紧急联系人',
  phoneMasked: '手机',
  idCardMasked: '身份证',
};

async function showDetail(row: EmployeeListVo): Promise<void> {
  try {
    const data = await getEmployee(row.id) as Record<string, unknown>;
    detail.value = data;
    // 解析子表数据
    educationList.value = Array.isArray(data.educationList) ? data.educationList as Record<string, unknown>[] : [];
    workExpList.value = Array.isArray(data.workExpList) ? data.workExpList as Record<string, unknown>[] : [];
    contractList.value = Array.isArray(data.contractList) ? data.contractList as Record<string, unknown>[] : [];
    detailVisible.value = true;
  } catch {
    ElMessage.error('加载员工详情失败');
  }
}

// ==================== 批量导入 ====================
const importLoading = ref(false);
const importResultVisible = ref(false);
const importResult = ref<BatchImportResultVo | null>(null);

async function handleImport(options: { file: File }): Promise<void> {
  importLoading.value = true;
  try {
    const result = await batchImportEmployees(options.file);
    importResult.value = result;
    importResultVisible.value = true;
    if (result.allSuccess) {
      ElMessage.success(`导入成功，共${result.successCount}条`);
    } else {
      ElMessage.warning(`导入完成：成功${result.successCount}条，失败${result.failCount}条`);
    }
    void fetchList();
  } catch {
    ElMessage.error('批量导入失败');
  } finally {
    importLoading.value = false;
  }
}

// ==================== 导出 ====================
const exportLoading = ref(false);

async function handleExport(): Promise<void> {
  exportLoading.value = true;
  try {
    await exportEmployees(query.status || undefined);
    ElMessage.success('导出成功');
  } catch {
    ElMessage.error('导出失败');
  } finally {
    exportLoading.value = false;
  }
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="employee-list">
    <h2 class="title">员工档案</h2>

    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="关键字">
        <el-input
          v-model="query.keyword"
          placeholder="工号 / 姓名 / 邮箱"
          clearable
          style="width: 220px"
          @keydown.enter.prevent="onSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" style="width: 160px">
          <el-option
            v-for="s in STATUS_OPTIONS"
            :key="s.value"
            :label="s.label"
            :value="s.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
      <el-form-item style="float: right">
        <el-upload
          :show-file-list="false"
          accept=".xlsx,.xls"
          :http-request="handleImport"
        >
          <el-button :loading="importLoading" :icon="Upload" type="success">批量导入</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item style="float: right">
        <el-button :loading="exportLoading" type="warning" @click="handleExport">导出花名册</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="empNo" label="工号" width="120" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="deptName" label="部门" min-width="140" />
      <el-table-column prop="hireDate" label="入职日期" width="140" />
      <el-table-column prop="phoneMasked" label="手机" width="160" />
      <el-table-column prop="idCardMasked" label="身份证" min-width="180" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :page-sizes="[10, 20, 50]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="fetchList"
      @current-change="fetchList"
    />

    <el-dialog v-model="detailVisible" title="员工详情" width="780px">
      <template v-if="detail">
        <!-- 基础信息 -->
        <h4 style="margin: 0 0 8px">基本信息</h4>
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item
            v-for="(label, key) in BASIC_FIELDS"
            :key="key"
            :label="label"
          >
            {{ detail[key] ?? '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 教育经历 -->
        <template v-if="educationList.length">
          <h4 style="margin: 0 0 8px">教育经历</h4>
          <el-table :data="educationList" border size="small" style="margin-bottom: 16px">
            <el-table-column prop="school" label="学校" />
            <el-table-column prop="major" label="专业" />
            <el-table-column prop="degree" label="学位" />
            <el-table-column prop="startDate" label="开始日期" />
            <el-table-column prop="endDate" label="结束日期" />
          </el-table>
        </template>

        <!-- 工作经历 -->
        <template v-if="workExpList.length">
          <h4 style="margin: 0 0 8px">工作经历</h4>
          <el-table :data="workExpList" border size="small" style="margin-bottom: 16px">
            <el-table-column prop="company" label="公司" />
            <el-table-column prop="position" label="职位" />
            <el-table-column prop="startDate" label="开始日期" />
            <el-table-column prop="endDate" label="结束日期" />
            <el-table-column prop="description" label="描述" />
          </el-table>
        </template>

        <!-- 合同信息 -->
        <template v-if="contractList.length">
          <h4 style="margin: 0 0 8px">合同信息</h4>
          <el-table :data="contractList" border size="small">
            <el-table-column prop="contractNo" label="合同编号" />
            <el-table-column prop="type" label="类型" />
            <el-table-column prop="startDate" label="开始日期" />
            <el-table-column prop="endDate" label="结束日期" />
            <el-table-column prop="status" label="状态" />
          </el-table>
        </template>
      </template>
    </el-dialog>

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="600px">
      <template v-if="importResult">
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="总行数">{{ importResult.totalRows }}</el-descriptions-item>
          <el-descriptions-item label="成功数">
            <el-tag type="success">{{ importResult.successCount }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败数">
            <el-tag :type="importResult.failCount > 0 ? 'danger' : 'success'">{{ importResult.failCount }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="importResult.allSuccess ? 'success' : 'warning'">
              {{ importResult.allSuccess ? '全部成功' : '部分失败' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="importResult.errors && importResult.errors.length > 0">
          <h4 style="margin: 0 0 8px">错误详情</h4>
          <el-table :data="importResult.errors" border size="small" max-height="300">
            <el-table-column prop="rowNum" label="行号" width="80" />
            <el-table-column prop="message" label="错误信息" />
          </el-table>
        </template>
      </template>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.employee-list {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.filter-bar {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
  display: flex;
}
</style>
