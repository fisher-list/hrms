<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listAnomalies, detectAnomalies, handleAnomaly } from '@/api/attendance';
import type { AttendanceAnomalyVo } from '@/types/attendance';

const list = ref<AttendanceAnomalyVo[]>([]);
const total = ref(0);
const loading = ref(false);
const detectLoading = ref(false);

/** 查询条件 */
const query = reactive({
  employeeId: undefined as number | undefined,
  startDate: '',
  endDate: '',
  anomalyType: '',
  status: '',
  current: 1,
  size: 10,
});

/** 检测日期范围 */
const detectRange = reactive({
  startDate: '',
  endDate: '',
});

/** 异常类型选项 */
const TYPE_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'LATE', label: '迟到' },
  { value: 'EARLY', label: '早退' },
  { value: 'ABSENT', label: '旷工' },
  { value: 'MISSING', label: '漏打卡' },
];

/** 状态选项 */
const STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待处理' },
  { value: 'HANDLED', label: '已处理' },
  { value: 'IGNORED', label: '已忽略' },
];

/** 类型标签颜色 */
const TYPE_TAG: Record<string, 'danger' | 'warning' | 'info'> = {
  LATE: 'warning',
  EARLY: 'warning',
  ABSENT: 'danger',
  MISSING: 'info',
};

/** 状态标签颜色 */
const STATUS_TAG: Record<string, 'success' | 'warning' | 'info'> = {
  PENDING: 'warning',
  HANDLED: 'success',
  IGNORED: 'info',
};

/** 类型中文名 */
const TYPE_LABEL: Record<string, string> = {
  LATE: '迟到',
  EARLY: '早退',
  ABSENT: '旷工',
  MISSING: '漏打卡',
};

/** 加载异常列表 */
async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    const page = await listAnomalies({
      employeeId: query.employeeId || undefined,
      startDate: query.startDate || undefined,
      endDate: query.endDate || undefined,
      anomalyType: query.anomalyType || undefined,
      status: query.status || undefined,
      current: query.current,
      size: query.size,
    });
    list.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error('加载异常列表失败');
  } finally {
    loading.value = false;
  }
}

/** 触发异常检测 */
async function onDetect(): Promise<void> {
  if (!detectRange.startDate || !detectRange.endDate) {
    ElMessage.warning('请选择检测日期范围');
    return;
  }
  detectLoading.value = true;
  try {
    const result = await detectAnomalies(detectRange.startDate, detectRange.endDate);
    ElMessage.success(`检测完成，发现 ${result.length} 条新异常`);
    void fetchList();
  } catch {
    ElMessage.error('异常检测失败');
  } finally {
    detectLoading.value = false;
  }
}

/** 处理异常 */
async function onHandle(row: AttendanceAnomalyVo, action: string): Promise<void> {
  const actionLabel = action === 'HANDLED' ? '处理' : '忽略';
  try {
    const { value: remark } = await ElMessageBox.prompt(`请输入${actionLabel}说明（可选）`, `${actionLabel}确认`, {
      inputPlaceholder: '说明...',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValidator: () => true,
    });
    await handleAnomaly({ anomalyId: row.id, action, remark: remark || undefined });
    ElMessage.success(`已${actionLabel}`);
    void fetchList();
  } catch {
    // 用户取消
  }
}

function onSearch(): void {
  query.current = 1;
  void fetchList();
}

function onReset(): void {
  query.employeeId = undefined;
  query.startDate = '';
  query.endDate = '';
  query.anomalyType = '';
  query.status = '';
  query.current = 1;
  void fetchList();
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="anomaly-page">
    <h2 class="title">考勤异常管理</h2>

    <!-- 异常检测区域 -->
    <el-card shadow="never" class="detect-card">
      <template #header>
        <span style="font-weight: 600">异常检测</span>
      </template>
      <el-form :inline="true">
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="detectRange.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择开始日期"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="detectRange.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择结束日期"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="detectLoading" @click="onDetect">
            开始检测
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 筛选条件 -->
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="员工ID">
        <el-input-number
          v-model="query.employeeId"
          :min="1"
          placeholder="全部"
          controls-position="right"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="query.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始"
          style="width: 140px"
        />
        <span style="margin: 0 4px">-</span>
        <el-date-picker
          v-model="query.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item label="异常类型">
        <el-select v-model="query.anomalyType" placeholder="全部" style="width: 120px">
          <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" style="width: 120px">
          <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 异常列表 -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="employeeId" label="员工ID" width="100" />
      <el-table-column prop="anomalyDate" label="异常日期" width="130" />
      <el-table-column prop="anomalyType" label="异常类型" width="110">
        <template #default="{ row }">
          <el-tag :type="TYPE_TAG[row.anomalyType] ?? 'info'">
            {{ TYPE_LABEL[row.anomalyType] ?? row.anomalyType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] ?? 'info'">
            {{ row.status === 'PENDING' ? '待处理' : row.status === 'HANDLED' ? '已处理' : '已忽略' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handleRemark" label="处理说明" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button type="success" link @click="onHandle(row, 'HANDLED')">处理</el-button>
            <el-button type="info" link @click="onHandle(row, 'IGNORED')">忽略</el-button>
          </template>
          <span v-else style="color: #999; font-size: 12px">已处理</span>
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
  </div>
</template>

<style scoped>
.anomaly-page {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.detect-card {
  margin-bottom: 16px;
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
