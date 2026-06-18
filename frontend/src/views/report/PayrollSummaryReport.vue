<script setup lang="ts">
/**
 * 薪资汇总报表页面
 * 部门薪资对比、薪资结构分析、年度薪资趋势
 */
import { ref, onMounted, nextTick } from 'vue'
import { getPayrollSummaryByDept, getPayrollYearlyTrend } from '@/api/report'
import type { PayrollSummaryVo } from '@/types/report'
import * as echarts from 'echarts'

/** 当前视图: monthly / trend */
const viewMode = ref<'monthly' | 'trend'>('monthly')

/** 查询月份 */
const selectedMonth = ref(new Date().toISOString().slice(0, 7))

/** 查询年份 */
const selectedYear = ref(new Date().getFullYear())

/** 表格数据 */
const tableData = ref<PayrollSummaryVo[]>([])
const loading = ref(false)

/** 图表DOM引用 */
const deptChartRef = ref<HTMLDivElement>()
const structureChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()

/** 加载月度数据 */
async function loadMonthlyData() {
  loading.value = true
  try {
    tableData.value = await getPayrollSummaryByDept(selectedMonth.value)
    await nextTick()
    renderDeptChart()
    renderStructureChart()
  } catch {
    // 错误已由http拦截器处理
  } finally {
    loading.value = false
  }
}

/** 加载年度趋势数据 */
async function loadTrendData() {
  loading.value = true
  try {
    tableData.value = await getPayrollYearlyTrend(selectedYear.value)
    await nextTick()
    renderTrendChart()
  } catch {
    // 错误已由http拦截器处理
  } finally {
    loading.value = false
  }
}

/** 渲染部门薪资对比柱状图 */
function renderDeptChart() {
  if (!deptChartRef.value || tableData.value.length === 0) return

  const chart = echarts.init(deptChartRef.value)
  const depts = tableData.value.map(d => d.deptName)
  const grossData = tableData.value.map(d => d.totalGrossPay)
  const netData = tableData.value.map(d => d.totalNetPay)
  const avgData = tableData.value.map(d => d.avgGrossPay)

  chart.setOption({
    title: { text: `${selectedMonth.value} 部门薪资对比`, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        let html = params[0].name + '<br/>'
        params.forEach((p: any) => {
          html += `${p.marker} ${p.seriesName}: ¥${(p.value / 10000).toFixed(2)}万<br/>`
        })
        return html
      },
    },
    legend: { data: ['应发总额', '实发总额', '人均薪资'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: depts, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', axisLabel: { formatter: (v: number) => (v / 10000).toFixed(0) + '万' } },
    series: [
      { name: '应发总额', type: 'bar', data: grossData, itemStyle: { color: '#409EFF' } },
      { name: '实发总额', type: 'bar', data: netData, itemStyle: { color: '#67C23A' } },
      { name: '人均薪资', type: 'line', data: avgData, itemStyle: { color: '#E6A23C' }, lineStyle: { width: 2 } },
    ],
  })
}

/** 渲染薪资结构分析饼图 */
function renderStructureChart() {
  if (!structureChartRef.value || tableData.value.length === 0) return

  const chart = echarts.init(structureChartRef.value)

  // 汇总所有部门的薪资结构
  let totalBase = 0, totalPosition = 0, totalPerf = 0, totalAllowance = 0
  tableData.value.forEach(d => {
    totalBase += d.totalBaseSalary || 0
    totalPosition += d.totalPositionSalary || 0
    totalPerf += d.totalPerformance || 0
    totalAllowance += d.totalAllowance || 0
  })

  chart.setOption({
    title: { text: `${selectedMonth.value} 薪资结构分析`, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.name}: ¥${(params.value / 10000).toFixed(2)}万 (${params.percent}%)`
      },
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: [
        { value: totalBase, name: '基本工资', itemStyle: { color: '#409EFF' } },
        { value: totalPosition, name: '岗位工资', itemStyle: { color: '#67C23A' } },
        { value: totalPerf, name: '绩效工资', itemStyle: { color: '#E6A23C' } },
        { value: totalAllowance, name: '津贴', itemStyle: { color: '#F56C6C' } },
      ],
    }],
  })
}

/** 渲染年度薪资趋势折线图 */
function renderTrendChart() {
  if (!trendChartRef.value || tableData.value.length === 0) return

  const chart = echarts.init(trendChartRef.value)

  // 按月份聚合
  const monthMap = new Map<string, { gross: number; net: number; count: number }>()
  tableData.value.forEach(d => {
    const existing = monthMap.get(d.month) || { gross: 0, net: 0, count: 0 }
    existing.gross += d.totalGrossPay || 0
    existing.net += d.totalNetPay || 0
    existing.count += d.employeeCount || 0
    monthMap.set(d.month, existing)
  })

  const months = Array.from(monthMap.keys()).sort()
  const grossData = months.map(m => monthMap.get(m)!.gross)
  const netData = months.map(m => monthMap.get(m)!.net)
  const countData = months.map(m => monthMap.get(m)!.count)

  chart.setOption({
    title: { text: `${selectedYear.value} 年度薪资趋势`, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        let html = params[0].name + '<br/>'
        params.forEach((p: any) => {
          const unit = p.seriesName === '员工数' ? '人' : '万'
          const val = p.seriesName === '员工数' ? p.value : (p.value / 10000).toFixed(2)
          html += `${p.marker} ${p.seriesName}: ${val}${unit}<br/>`
        })
        return html
      },
    },
    legend: { data: ['应发总额', '实发总额', '员工数'], bottom: 0 },
    grid: { left: '3%', right: '8%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: months },
    yAxis: [
      { type: 'value', name: '金额', axisLabel: { formatter: (v: number) => (v / 10000).toFixed(0) + '万' } },
      { type: 'value', name: '人数', position: 'right' },
    ],
    series: [
      { name: '应发总额', type: 'line', data: grossData, itemStyle: { color: '#409EFF' }, smooth: true },
      { name: '实发总额', type: 'line', data: netData, itemStyle: { color: '#67C23A' }, smooth: true },
      { name: '员工数', type: 'bar', yAxisIndex: 1, data: countData, itemStyle: { color: '#909399' }, barWidth: '30%' },
    ],
  })
}

/** 切换视图 */
function onViewModeChange() {
  tableData.value = []
  if (viewMode.value === 'monthly') {
    loadMonthlyData()
  } else {
    loadTrendData()
  }
}

/** 格式化金额 */
function formatMoney(value?: number): string {
  if (value == null) return '-'
  return '¥' + value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  loadMonthlyData()
})
</script>

<template>
  <div class="payroll-summary-report">
    <el-card shadow="never">
      <template #header>
        <span>薪资汇总报表</span>
      </template>

      <!-- 查询条件 -->
      <el-form :inline="true" class="query-form">
        <el-form-item label="模式">
          <el-radio-group v-model="viewMode" @change="onViewModeChange">
            <el-radio-button value="monthly">月度报表</el-radio-button>
            <el-radio-button value="trend">年度趋势</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="viewMode === 'monthly'" label="月份">
          <el-date-picker
            v-model="selectedMonth"
            type="month"
            placeholder="选择月份"
            format="YYYY-MM"
            value-format="YYYY-MM"
            @change="loadMonthlyData"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item v-if="viewMode === 'trend'" label="年份">
          <el-date-picker
            v-model="selectedYear"
            type="year"
            placeholder="选择年份"
            format="YYYY"
            value-format="YYYY"
            @change="loadTrendData"
            style="width: 120px"
          />
        </el-form-item>
      </el-form>

      <!-- 月度报表视图 -->
      <template v-if="viewMode === 'monthly'">
        <!-- 图表区域 -->
        <el-row :gutter="16" class="chart-row">
          <el-col :span="14">
            <el-card shadow="hover">
              <div ref="deptChartRef" class="chart-container"></div>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="hover">
              <div ref="structureChartRef" class="chart-container"></div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 数据表格 -->
        <el-table :data="tableData" v-loading="loading" border stripe show-summary style="width: 100%">
          <el-table-column label="部门" prop="deptName" min-width="120" />
          <el-table-column label="人数" prop="employeeCount" width="70" align="center" />
          <el-table-column label="基本工资" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalBaseSalary) }}</template>
          </el-table-column>
          <el-table-column label="岗位工资" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalPositionSalary) }}</template>
          </el-table-column>
          <el-table-column label="绩效工资" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalPerformance) }}</template>
          </el-table-column>
          <el-table-column label="津贴" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalAllowance) }}</template>
          </el-table-column>
          <el-table-column label="应发合计" width="130" align="right">
            <template #default="{ row }">
              <strong>{{ formatMoney(row.totalGrossPay) }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="社保" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalSocialInsurance) }}</template>
          </el-table-column>
          <el-table-column label="公积金" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalHousingFund) }}</template>
          </el-table-column>
          <el-table-column label="个税" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalIit) }}</template>
          </el-table-column>
          <el-table-column label="实发合计" width="130" align="right">
            <template #default="{ row }">
              <strong style="color: #67C23A">{{ formatMoney(row.totalNetPay) }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="人均薪资" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.avgGrossPay) }}</template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 年度趋势视图 -->
      <template v-if="viewMode === 'trend'">
        <el-card shadow="hover" class="chart-card">
          <div ref="trendChartRef" class="chart-container-lg"></div>
        </el-card>

        <!-- 按月汇总表格 -->
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%; margin-top: 16px">
          <el-table-column label="月份" prop="month" width="100" />
          <el-table-column label="部门" prop="deptName" min-width="120" />
          <el-table-column label="人数" prop="employeeCount" width="70" align="center" />
          <el-table-column label="应发合计" width="130" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalGrossPay) }}</template>
          </el-table-column>
          <el-table-column label="实发合计" width="130" align="right">
            <template #default="{ row }">{{ formatMoney(row.totalNetPay) }}</template>
          </el-table-column>
          <el-table-column label="人均薪资" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.avgGrossPay) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.payroll-summary-report {
  padding: 24px;
}
.query-form {
  margin-bottom: 16px;
}
.chart-row {
  margin-bottom: 16px;
}
.chart-card {
  margin-bottom: 16px;
}
.chart-container {
  width: 100%;
  height: 350px;
}
.chart-container-lg {
  width: 100%;
  height: 400px;
}
</style>
