<script setup lang="ts">
/**
 * HR仪表盘页面
 * 展示在职人数、离职率、招聘进度、考勤异常率、薪资总额等关键指标
 * 使用ECharts图表展示数据可视化
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { getDashboard } from '@/api/report'
import type { DashboardVo } from '@/types/report'
import * as echarts from 'echarts'

const router = useRouter()
const auth = useAuthStore()

/** 仪表盘数据 */
const dashboard = ref<DashboardVo | null>(null)
const loading = ref(false)

/** 图表DOM引用 */
const recruitChartRef = ref<HTMLDivElement>()
const attendanceChartRef = ref<HTMLDivElement>()
const payrollChartRef = ref<HTMLDivElement>()
const turnoverChartRef = ref<HTMLDivElement>()

function onLogout(): void {
  auth.logout()
  ElMessage.success('已退出登录')
  void router.replace('/login')
}

/** 格式化金额 */
function formatMoney(value?: number): string {
  if (value == null) return '0'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 加载仪表盘数据 */
async function loadData() {
  loading.value = true
  try {
    dashboard.value = await getDashboard()
    // 数据加载后初始化图表
    setTimeout(() => initCharts(), 100)
  } catch {
    // 错误已由http拦截器处理
  } finally {
    loading.value = false
  }
}

/** 初始化图表 */
function initCharts() {
  if (!dashboard.value) return

  // 招聘进度饼图
  if (recruitChartRef.value) {
    const chart = echarts.init(recruitChartRef.value)
    chart.setOption({
      title: { text: '招聘进度', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { value: dashboard.value.openRequisitionCount, name: '招聘中需求', itemStyle: { color: '#409EFF' } },
          { value: dashboard.value.candidateCount, name: '候选人', itemStyle: { color: '#67C23A' } },
          { value: dashboard.value.offerCount, name: '待入职(Offer)', itemStyle: { color: '#E6A23C' } },
        ],
      }],
    })
  }

  // 考勤概况饼图
  if (attendanceChartRef.value) {
    const chart = echarts.init(attendanceChartRef.value)
    chart.setOption({
      title: { text: '本月考勤概况', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { value: dashboard.value.leaveRequestCount, name: '请假人次', itemStyle: { color: '#E6A23C' } },
          { value: dashboard.value.overtimeRequestCount, name: '加班人次', itemStyle: { color: '#F56C6C' } },
          { value: Number(dashboard.value.attendanceAnomalyRate), name: '异常率(%)', itemStyle: { color: '#909399' } },
        ],
      }],
    })
  }

  // 薪资概况柱状图
  if (payrollChartRef.value) {
    const chart = echarts.init(payrollChartRef.value)
    chart.setOption({
      title: { text: `薪资概况 (${dashboard.value.latestPayrollMonth || '暂无'})`, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['应发总额', '实发总额'],
      },
      yAxis: { type: 'value', axisLabel: { formatter: (v: number) => (v / 10000).toFixed(0) + '万' } },
      series: [{
        type: 'bar',
        data: [
          { value: dashboard.value.latestPayrollGross || 0, itemStyle: { color: '#409EFF' } },
          { value: dashboard.value.latestPayrollNet || 0, itemStyle: { color: '#67C23A' } },
        ],
        barWidth: '40%',
      }],
    })
  }

  // 人员构成环形图
  if (turnoverChartRef.value) {
    const chart = echarts.init(turnoverChartRef.value)
    chart.setOption({
      title: { text: '人员概况', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { value: dashboard.value.activeCount, name: '在职人数', itemStyle: { color: '#67C23A' } },
          { value: dashboard.value.newHireThisMonth, name: '本月新入职', itemStyle: { color: '#409EFF' } },
          { value: dashboard.value.terminatedThisMonth, name: '本月离职', itemStyle: { color: '#F56C6C' } },
        ],
      }],
    })
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <header class="dashboard-header">
      <span class="title">HRMS Dashboard</span>
      <span class="user">
        <span v-if="auth.user">欢迎,{{ auth.user.nickname || auth.user.username }}</span>
        <el-button type="primary" link @click="onLogout">退出登录</el-button>
      </span>
    </header>

    <main class="dashboard-body">
      <!-- KPI 卡片行 -->
      <el-row :gutter="16" class="kpi-row">
        <el-col :span="6">
          <el-card shadow="hover" class="kpi-card">
            <div class="kpi-label">在职人数</div>
            <div class="kpi-value green">{{ dashboard?.activeCount ?? '-' }}</div>
            <div class="kpi-sub">
              本月新入职 <strong>{{ dashboard?.newHireThisMonth ?? '-' }}</strong>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="kpi-card">
            <div class="kpi-label">年度离职率</div>
            <div class="kpi-value red">{{ dashboard?.turnoverRate ?? '-' }}%</div>
            <div class="kpi-sub">
              年度累计离职 <strong>{{ dashboard?.terminatedThisYear ?? '-' }}</strong> 人
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="kpi-card">
            <div class="kpi-label">考勤异常率</div>
            <div class="kpi-value orange">{{ dashboard?.attendanceAnomalyRate ?? '-' }}%</div>
            <div class="kpi-sub">
              请假 <strong>{{ dashboard?.leaveRequestCount ?? '-' }}</strong> / 加班 <strong>{{ dashboard?.overtimeRequestCount ?? '-' }}</strong> 人次
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="kpi-card">
            <div class="kpi-label">最新薪资总额</div>
            <div class="kpi-value blue">¥{{ formatMoney(dashboard?.latestPayrollGross) }}</div>
            <div class="kpi-sub">
              {{ dashboard?.latestPayrollMonth ?? '-' }} · {{ dashboard?.latestPayrollEmployeeCount ?? '-' }} 人
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="12">
          <el-card shadow="hover">
            <div ref="recruitChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <div ref="attendanceChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :span="12">
          <el-card shadow="hover">
            <div ref="payrollChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <div ref="turnoverChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 快捷入口 -->
      <el-card shadow="hover" class="shortcut-card">
        <template #header>
          <span>快捷入口</span>
        </template>
        <div class="quick-links">
          <el-button @click="$router.push('/report/roster')">人事花名册</el-button>
          <el-button @click="$router.push('/report/attendance-monthly')">考勤月报</el-button>
          <el-button @click="$router.push('/report/payroll-summary')">薪资报表</el-button>
          <el-button @click="$router.push('/hr/employees')">员工档案</el-button>
          <el-button @click="$router.push('/attendance/leave/new')">请假申请</el-button>
          <el-button @click="$router.push('/payroll/runs')">薪酬批次</el-button>
          <el-button @click="$router.push('/recruit/offers')">招聘 Offer</el-button>
          <el-button @click="$router.push('/performance/reviews')">绩效评审</el-button>
          <el-button @click="$router.push('/system/roles')">角色管理</el-button>
        </div>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: #f5f7fa;
}
.dashboard-header {
  height: 56px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.user {
  display: flex;
  gap: 16px;
  align-items: center;
}
.dashboard-body {
  padding: 24px;
}
.kpi-row {
  margin-bottom: 16px;
}
.kpi-card {
  text-align: center;
}
.kpi-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.kpi-value {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
}
.kpi-value.green { color: #67C23A; }
.kpi-value.red { color: #F56C6C; }
.kpi-value.orange { color: #E6A23C; }
.kpi-value.blue { color: #409EFF; }
.kpi-sub {
  font-size: 12px;
  color: #909399;
}
.kpi-sub strong {
  color: #303133;
}
.chart-row {
  margin-bottom: 16px;
}
.chart-container {
  width: 100%;
  height: 300px;
}
.shortcut-card {
  margin-top: 8px;
}
.quick-links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
