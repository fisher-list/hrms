<script setup lang="ts">
/**
 * 人事花名册报表页面
 * 支持自定义列、筛选条件、排序、分页、导出Excel
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoster } from '@/api/report'
import type { RosterRowVo } from '@/types/report'
import type { PageVo } from '@/types/portal'

/** 表格数据 */
const tableData = ref<RosterRowVo[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

/** 查询条件 */
const query = reactive({
  keyword: '',
  status: '',
  deptId: undefined as number | undefined,
})

/** 可选列定义 */
const allColumns = [
  { key: 'empNo', label: '工号', width: 120 },
  { key: 'name', label: '姓名', width: 100 },
  { key: 'gender', label: '性别', width: 60 },
  { key: 'birthDate', label: '出生日期', width: 120 },
  { key: 'email', label: '邮箱', width: 180 },
  { key: 'deptName', label: '部门', width: 120 },
  { key: 'hireDate', label: '入职日期', width: 120 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'contractStart', label: '合同开始', width: 120 },
  { key: 'contractEnd', label: '合同结束', width: 120 },
  { key: 'probationEnd', label: '试用期结束', width: 120 },
  { key: 'emergencyContact', label: '紧急联系人', width: 120 },
]

/** 已选列 */
const selectedColumns = ref<string[]>(allColumns.map(c => c.key))

/** 当前显示的列 */
const visibleColumns = ref(allColumns)

/** 状态选项 */
const statusOptions = [
  { label: '全部', value: '' },
  { label: '在职', value: 'ACTIVE' },
  { label: '试用期', value: 'PROBATION' },
  { label: '休假中', value: 'ON_LEAVE' },
  { label: '已离职', value: 'TERMINATED' },
]

/** 状态标签映射 */
const statusMap: Record<string, { label: string; type: string }> = {
  ACTIVE: { label: '在职', type: 'success' },
  PROBATION: { label: '试用期', type: 'warning' },
  ON_LEAVE: { label: '休假中', type: 'info' },
  TERMINATED: { label: '已离职', type: 'danger' },
  PENDING_HIRE: { label: '待入职', type: 'info' },
}

/** 加载数据 */
async function loadData() {
  loading.value = true
  try {
    const result: PageVo<RosterRowVo> = await getRoster(query, currentPage.value, pageSize.value)
    tableData.value = result.records
    total.value = result.total
  } catch {
    // 错误已由http拦截器处理
  } finally {
    loading.value = false
  }
}

/** 列选择变更 */
function onColumnChange(cols: string[]) {
  visibleColumns.value = allColumns.filter(c => cols.includes(c.key))
}

/** 分页变更 */
function onPageChange(page: number) {
  currentPage.value = page
  loadData()
}

function onSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

/** 导出Excel */
function exportExcel() {
  // 构造CSV导出（简单实现，无需后端额外依赖）
  const headers = visibleColumns.value.map(c => c.label)
  const rows = tableData.value.map(row =>
    visibleColumns.value.map(c => {
      const val = (row as any)[c.key]
      return val != null ? String(val) : ''
    })
  )

  // 加入BOM头解决Excel中文乱码
  let csv = '\uFEFF' + headers.join(',') + '\n'
  rows.forEach(r => {
    csv += r.map(v => `"${v}"`).join(',') + '\n'
  })

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `人事花名册_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

/** 重置查询 */
function resetQuery() {
  query.keyword = ''
  query.status = ''
  query.deptId = undefined
  currentPage.value = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="roster-report">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>人事花名册报表</span>
          <el-button type="primary" @click="exportExcel">导出Excel</el-button>
        </div>
      </template>

      <!-- 查询条件 -->
      <el-form :inline="true" class="query-form">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="姓名/工号"
            clearable
            @keyup.enter="loadData"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="currentPage = 1; loadData()">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 自定义列选择 -->
      <div class="column-selector">
        <span class="selector-label">显示列：</span>
        <el-checkbox-group v-model="selectedColumns" @change="onColumnChange">
          <el-checkbox
            v-for="col in allColumns"
            :key="col.key"
            :label="col.key"
            :value="col.key"
          >
            {{ col.label }}
          </el-checkbox>
        </el-checkbox-group>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column
          v-for="col in visibleColumns"
          :key="col.key"
          :prop="col.key"
          :label="col.label"
          :width="col.width"
          sortable
        >
          <template #default="{ row }">
            <template v-if="col.key === 'status'">
              <el-tag
                v-if="statusMap[row.status]"
                :type="statusMap[row.status].type as any"
                size="small"
              >
                {{ statusMap[row.status].label }}
              </el-tag>
              <span v-else>{{ row.status }}</span>
            </template>
            <template v-else-if="col.key === 'gender'">
              {{ row.gender === 'M' ? '男' : row.gender === 'F' ? '女' : row.gender }}
            </template>
            <template v-else>
              {{ row[col.key as keyof RosterRowVo] ?? '-' }}
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.roster-report {
  padding: 24px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.query-form {
  margin-bottom: 16px;
}
.column-selector {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.selector-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
