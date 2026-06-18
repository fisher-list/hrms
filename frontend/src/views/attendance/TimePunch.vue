<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listTimePunches } from '@/api/attendance';
import type { TimePunchVo } from '@/types/attendance';

const list = ref<TimePunchVo[]>([]);
const loading = ref(false);

const query = reactive({
  employeeId: undefined as number | undefined,
  startDate: '',
  endDate: '',
  range: [] as string[],
});

const PUNCH_TYPE_TAG: Record<string, 'success' | 'info'> = {
  CLOCK_IN: 'success',
  CLOCK_OUT: 'info',
};

async function fetchList(): Promise<void> {
  loading.value = true;
  try {
    list.value = await listTimePunches({
      employeeId: query.employeeId || undefined,
      startDate: query.range[0] || undefined,
      endDate: query.range[1] || undefined,
    });
  } catch {
    ElMessage.error('加载打卡记录失败');
  } finally {
    loading.value = false;
  }
}

function onSearch(): void {
  void fetchList();
}

function onReset(): void {
  query.employeeId = undefined;
  query.range = [];
  void fetchList();
}

onMounted(() => void fetchList());
</script>

<template>
  <div class="time-punch">
    <h2 class="title">打卡记录</h2>

    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="员工ID">
        <el-input-number
          v-model="query.employeeId"
          :min="1"
          placeholder="全部"
          controls-position="right"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="query.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 280px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="employeeId" label="员工ID" width="100" />
      <el-table-column prop="employeeName" label="姓名" width="120" />
      <el-table-column prop="punchTime" label="打卡时间" min-width="200" />
      <el-table-column prop="punchType" label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="PUNCH_TYPE_TAG[row.punchType] ?? 'info'">
            {{ row.punchType === 'CLOCK_IN' ? '上班' : '下班' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="120" />
    </el-table>
  </div>
</template>

<style scoped>
.time-punch {
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
</style>
