<script setup lang="ts">
/**
 * 考勤月报页面
 * 展示月度出勤率、迟到、早退、加班、请假统计
 * 支持按部门/个人维度查看
 */
import { ref, onMounted } from 'vue'
import { getAttendanceMonthlyByDept, getAttendanceMonthlyByEmployee } from '@/api/report'
import type { AttendanceMonthlyVo } from '@/types/report'
import * as echarts from 'echarts'

/** 当前维度: dept / employee */
const dimension = ref<'dept' | 'employee'>('dept')

/** 查询月份 */
const selectedMonth = ref(new Date().toISOString().slice(0, 7))

/** 表格数据 */
const tableData = ref<AttendanceMonthlyVo[]>([])
const loading = ref(false)

/** 图表DOM引用 */
const chartRef = ref<HTMLDivElement>()

/** 月份选择器选项限制（不能选未来月份） */
function disabledDate(date: Date): boolean {
  return date.getTime() > Date.now()
}

/** 加载数据 */
async function loadData() {
  loading.value = true
  try {
    if (dimension.value === 'dept') {
      tableData.value = await getAttendanceMonthlyByDept(selectedMonth.value)
    } else {
      tableData.value = await getAttendanceMonthlyByEmployee(selectedMonth.value)
    }
    // 数据加载后渲染图表
    setTimeout(() => renderChart(), 100)
  } catch {
    // 错误已由http拦截器处理
  } finally {
    loading.value = false
  }
}

/** 渲染图表 */
function renderChart() {
  if (!chartRef.value || tableData.value.length === 0) return

  const chart = echarts.init(chartRef.value)
  const names = tableData.value.map(d => d.dimensionName || '-')
  const lateCounts = tableData.value.map(d => d.lateCount)
  const earlyCounts = tableData.value.map(d => d.earlyLeaveCount)
  const absentCounts = tableData.value.map(d => d.absentDays)
  const overtimeHours = tableData.value.map(d => d.overtimeHours)
  const leaveDays = tableData.value.map(d => d.leaveDays)

  chart.setOption({
    title: { text: `${selectedMonth.value} 考勤月报`, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['迟到', '早退', '旷工', '加班(时)', '请假(天)'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series: [
      { name: '迟到', type: 'bar', stack: '异常', data: lateCounts, itemStyle: { color: '#E6A23C' } },
      { name: '早退', type: 'bar', stack: '异常', data: earlyCounts, itemStyle: { color: '#F56C6C' } },
      { name: '旷工', type: 'bar', stack: '异常', data: absentCounts, itemStyle: { color: '#909399' } },
      { name: '加班(时)', type: 'bar', data: overtimeHours, itemStyle: { color: '#409EFF' } },
      { name: '请假(天)', type: 'bar', data: leaveDays, itemStyle: { color: '#67C23A' } },
    ],
  })
}

/** 切换维度 */
function onDimensionChange() {
  tableData.value = []
  loadData()
}

/** 切换月份 */
function onMonthChange() {
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="attendance-monthly-report">
    <el-card shadow="never">
      <template #header>
        <span>考勤月报</span>
      </template>

      <!-- 查询条件 -->
      <el-form :inline="true" class="query-form">
        <el-form-item label="月份">
          <el-date-picker
            v-model="selectedMonth"
            type="month"
            placeholder="选择月份"
            format="YYYY-MM"
            value-format="YYYY-MM"
            :disabled-date="disabledDate"
            @change="onMonthChange"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="维度">
          <el-radio-group v-model="dimension" @change="onDimensionChange">
            <el-radio-button value="dept">按部门</el-radio-button>
            <el-radio-button value="employee">按个人</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <!-- 图表区域 -->
      <el-card shadow="hover" class="chart-card">
        <div ref="chartRef" class="chart-container"></div>
      </el-card>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%; margin-top: 16px">
        <el-table-column
          :label="dimension === 'dept' ? '部门' : '姓名'"
          prop="dimensionName"
          min-width="120"
        />
        <el-table-column v-if="dimension === 'employee'" label="工号" prop="empNo" width="120" />
        <el-table-column label="应出勤" prop="scheduledDays" width="90" align="center" />
        <el-table-column label="实出勤" prop="attendedDays" width="90" align="center" />
        <el-table-column label="出勤率" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.attendanceRate >= 95 ? 'success' : row.attendanceRate >= 85 ? 'warning' : 'danger'" size="small">
              {{ row.attendanceRate }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="迟到" prop="lateCount" width="70" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-warn': row.lateCount > 0 }">{{ row.lateCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="早退" prop="earlyLeaveCount" width="70" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-warn': row.earlyLeaveCount > 0 }">{{ row.earlyLeaveCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="旷工" prop="absentDays" width="70" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.absentDays > 0 }">{{ row.absentDays }}</span>
          </template>
        </el-table-column>
        <el-table-column label="加班(时)" width="100" align="center">
          <template #default="{ row }">
            {{ row.overtimeHours }}h
          </template>
        </el-table-column>
        <el-table-column label="请假(天)" width="100" align="center">
          <template #default="{ row }">
            {{ row.leaveDays }}天
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.attendance-monthly-report {
  padding: 24px;
}
.query-form {
  margin-bottom: 16px;
}
.chart-card {
  margin-bottom: 16px;
}
.chart-container {
  width: 100%;
  height: 350px;
}
.text-warn {
  color: #E6A23C;
  font-weight: 600;
}
.text-danger {
  color: #F56C6C;
  font-weight: 600;
}
</style>
