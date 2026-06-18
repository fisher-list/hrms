<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  listShifts, createShift, updateShift, deleteShift,
  listSchedules, createSchedule,
  listTimePunches,
} from '@/api/attendance';
import { listEmployees } from '@/api/employee';
import type { ShiftVo, ScheduleVo, TimePunchVo } from '@/types/attendance';
import type { EmployeeListVo } from '@/types/hr';

/* ================================================================
   Tab 管理
   ================================================================ */
const activeTab = ref<'shift' | 'schedule' | 'punch'>('shift');

/* ================================================================
   1. 班次管理 Tab
   ================================================================ */
const shifts = ref<ShiftVo[]>([]);
const shiftLoading = ref(false);
const shiftDialogVisible = ref(false);
const shiftEditingId = ref<number | null>(null);
const shiftFormRef = ref<FormInstance>();

const shiftForm = reactive({
  name: '',
  startTime: '',
  endTime: '',
  crossDay: false,
  status: 'ACTIVE',
});

const shiftRules: FormRules = {
  name: [{ required: true, message: '请输入班次名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择上班时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择下班时间', trigger: 'change' }],
};

async function loadShifts(): Promise<void> {
  shiftLoading.value = true;
  try {
    const page = await listShifts(1, 100);
    shifts.value = page.records ?? [];
  } catch { /* http interceptor handles */ } finally {
    shiftLoading.value = false;
  }
}

function openShiftCreate(): void {
  shiftEditingId.value = null;
  Object.assign(shiftForm, { name: '', startTime: '', endTime: '', crossDay: false, status: 'ACTIVE' });
  shiftDialogVisible.value = true;
}

function openShiftEdit(row: ShiftVo): void {
  shiftEditingId.value = row.id;
  Object.assign(shiftForm, {
    name: row.name,
    startTime: row.startTime,
    endTime: row.endTime,
    crossDay: row.crossDay,
    status: row.status,
  });
  shiftDialogVisible.value = true;
}

async function submitShift(): Promise<void> {
  const valid = await shiftFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  try {
    if (shiftEditingId.value) {
      await updateShift(shiftEditingId.value, { ...shiftForm });
      ElMessage.success('班次已更新');
    } else {
      await createShift({ ...shiftForm });
      ElMessage.success('班次已创建');
    }
    shiftDialogVisible.value = false;
    await loadShifts();
  } catch { /* handled */ }
}

async function handleShiftDelete(row: ShiftVo): Promise<void> {
  await ElMessageBox.confirm(`确定删除班次「${row.name}」？`, '提示', { type: 'warning' });
  try {
    await deleteShift(row.id);
    ElMessage.success('已删除');
    await loadShifts();
  } catch { /* handled */ }
}

/* ================================================================
   2. 排班管理 Tab
   ================================================================ */
const _d = new Date();
const scheduleMonth = ref<string>(`${_d.getFullYear()}-${String(_d.getMonth() + 1).padStart(2, '0')}`);
const schedules = ref<ScheduleVo[]>([]);
const scheduleLoading = ref(false);

// 分配排班对话框
const assignDialogVisible = ref(false);
const assignFormRef = ref<FormInstance>();
const employees = ref<EmployeeListVo[]>([]);
const assignForm = reactive({
  employeeIds: [] as number[],
  shiftId: undefined as number | undefined,
  dates: [] as string[],
});
const assignRules: FormRules = {
  employeeIds: [{ type: 'array', required: true, message: '请选择员工', trigger: 'change' }],
  shiftId: [{ required: true, message: '请选择班次', trigger: 'change' }],
  dates: [{ type: 'array', required: true, message: '请选择日期', trigger: 'change' }],
};

const monthDays = computed(() => {
  const [y, m] = scheduleMonth.value.split('-').map(Number);
  const days = new Date(y, m, 0).getDate();
  const arr: string[] = [];
  for (let d = 1; d <= days; d++) {
    arr.push(`${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`);
  }
  return arr;
});

// schedule map: employeeId -> { date -> shiftName }
const scheduleMap = computed(() => {
  const map: Record<number, Record<string, string>> = {};
  for (const s of schedules.value) {
    if (!map[s.employeeId]) map[s.employeeId] = {};
    map[s.employeeId][s.workDate] = s.shiftName ?? `班次${s.shiftId}`;
  }
  return map;
});

const scheduleEmployeeIds = computed(() => Object.keys(scheduleMap.value).map(Number));

async function loadSchedules(): Promise<void> {
  if (!scheduleMonth.value) return;
  const [y, m] = scheduleMonth.value.split('-').map(Number);
  const startDate = `${y}-${String(m).padStart(2, '0')}-01`;
  const endDate = `${y}-${String(m).padStart(2, '0')}-${new Date(y, m, 0).getDate()}`;
  scheduleLoading.value = true;
  try {
    schedules.value = await listSchedules({ startDate, endDate });
  } catch { /* handled */ } finally {
    scheduleLoading.value = false;
  }
}

async function loadEmployees(): Promise<void> {
  try {
    const page = await listEmployees('', 'ACTIVE', 1, 200);
    employees.value = page.records ?? [];
  } catch { /* handled */ }
}

function openAssignDialog(): void {
  Object.assign(assignForm, { employeeIds: [], shiftId: undefined, dates: [] });
  assignDialogVisible.value = true;
}

async function submitAssign(): Promise<void> {
  const valid = await assignFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  try {
    await createSchedule({
      employeeIds: assignForm.employeeIds,
      shiftId: assignForm.shiftId!,
      dates: assignForm.dates,
    });
    ElMessage.success('排班成功');
    assignDialogVisible.value = false;
    await loadSchedules();
  } catch { /* handled */ }
}

function getEmployeeName(id: number): string {
  const emp = employees.value.find((e) => e.id === id);
  return emp ? `${emp.name}（${emp.empNo}）` : `员工${id}`;
}

/* ================================================================
   3. 打卡记录 Tab
   ================================================================ */
const punches = ref<TimePunchVo[]>([]);
const punchLoading = ref(false);
const punchRange = ref<string[]>([]);

async function loadPunches(): Promise<void> {
  punchLoading.value = true;
  try {
    punches.value = await listTimePunches({
      startDate: punchRange.value?.[0] || undefined,
      endDate: punchRange.value?.[1] || undefined,
    });
  } catch { /* handled */ } finally {
    punchLoading.value = false;
  }
}

/* ================================================================
   Init
   ================================================================ */
onMounted(() => {
  loadShifts();
  loadEmployees();
});
</script>

<template>
  <div class="shift-schedule">
    <h2 class="page-title">排班管理</h2>

    <el-tabs v-model="activeTab" @tab-change="(val: string) => { if (val === 'schedule') loadSchedules(); if (val === 'punch') loadPunches(); }">
      <!-- ==================== 班次管理 ==================== -->
      <el-tab-pane label="班次管理" name="shift">
        <div class="toolbar">
          <el-button type="primary" @click="openShiftCreate">新增班次</el-button>
        </div>

        <el-table :data="shifts" v-loading="shiftLoading" border stripe>
          <el-table-column prop="name" label="班次名称" width="150" />
          <el-table-column prop="startTime" label="上班时间" width="120" />
          <el-table-column prop="endTime" label="下班时间" width="120" />
          <el-table-column prop="crossDay" label="跨天" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.crossDay ? 'warning' : 'info'" size="small">
                {{ row.crossDay ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openShiftEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleShiftDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ==================== 排班管理 ==================== -->
      <el-tab-pane label="排班管理" name="schedule">
        <div class="toolbar">
          <el-date-picker
            v-model="scheduleMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            style="width: 160px"
            @change="loadSchedules"
          />
          <el-button type="primary" style="margin-left: 12px" @click="openAssignDialog">新增排班</el-button>
          <el-button @click="loadSchedules">刷新</el-button>
        </div>

        <div class="calendar-wrapper" v-loading="scheduleLoading">
          <el-empty v-if="scheduleEmployeeIds.length === 0" description="本月暂无排班数据" />
          <el-scrollbar v-else max-height="520px">
            <table class="calendar-table">
              <thead>
                <tr>
                  <th class="sticky-col">员工</th>
                  <th v-for="d in monthDays" :key="d" :class="{ 'weekend-col': [0, 6].includes(new Date(d).getDay()) }">
                    {{ d.slice(8) }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="eid in scheduleEmployeeIds" :key="eid">
                  <td class="sticky-col emp-cell">{{ getEmployeeName(eid) }}</td>
                  <td v-for="d in monthDays" :key="d" :class="{ 'weekend-col': [0, 6].includes(new Date(d).getDay()) }">
                    <el-tag v-if="scheduleMap[eid]?.[d]" size="small" type="success">
                      {{ scheduleMap[eid][d] }}
                    </el-tag>
                    <span v-else class="no-shift">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </el-scrollbar>
        </div>
      </el-tab-pane>

      <!-- ==================== 打卡记录 ==================== -->
      <el-tab-pane label="打卡记录" name="punch">
        <div class="toolbar">
          <el-date-picker
            v-model="punchRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 280px"
            @change="loadPunches"
          />
          <el-button type="primary" style="margin-left: 12px" @click="loadPunches">查询</el-button>
        </div>

        <el-table :data="punches" v-loading="punchLoading" border stripe>
          <el-table-column prop="employeeId" label="员工ID" width="100" />
          <el-table-column prop="employeeName" label="姓名" width="120" />
          <el-table-column prop="punchTime" label="打卡时间" width="200">
            <template #default="{ row }">
              {{ row.punchTime?.replace('T', ' ') }}
            </template>
          </el-table-column>
          <el-table-column prop="punchType" label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.punchType === 'CLOCK_IN' ? 'success' : 'warning'" size="small">
                {{ row.punchType === 'CLOCK_IN' ? '上班' : '下班' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="120" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 班次 新增/编辑 弹窗 ==================== -->
    <el-dialog
      v-model="shiftDialogVisible"
      :title="shiftEditingId ? '编辑班次' : '新增班次'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="shiftFormRef" :model="shiftForm" :rules="shiftRules" label-width="90px">
        <el-form-item label="班次名称" prop="name">
          <el-input v-model="shiftForm.name" placeholder="如：早班" maxlength="30" />
        </el-form-item>
        <el-form-item label="上班时间" prop="startTime">
          <el-time-picker v-model="shiftForm.startTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下班时间" prop="endTime">
          <el-time-picker v-model="shiftForm.endTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="跨天">
          <el-switch v-model="shiftForm.crossDay" active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="shiftForm.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shiftDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitShift">确定</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 排班分配弹窗 ==================== -->
    <el-dialog v-model="assignDialogVisible" title="新增排班" width="600px" destroy-on-close>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="90px">
        <el-form-item label="选择员工" prop="employeeIds">
          <el-select
            v-model="assignForm.employeeIds"
            multiple
            filterable
            placeholder="搜索员工"
            style="width: 100%"
          >
            <el-option
              v-for="emp in employees"
              :key="emp.id"
              :label="`${emp.name}（${emp.empNo}）`"
              :value="emp.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择班次" prop="shiftId">
          <el-select v-model="assignForm.shiftId" placeholder="选择班次" style="width: 100%">
            <el-option
              v-for="s in shifts"
              :key="s.id"
              :label="`${s.name}（${s.startTime}-${s.endTime}）`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择日期" prop="dates">
          <el-date-picker
            v-model="assignForm.dates"
            type="dates"
            value-format="YYYY-MM-DD"
            placeholder="选择一个或多个日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.shift-schedule {
  padding: 24px;
}
.page-title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

/* 日历表格 */
.calendar-wrapper {
  overflow-x: auto;
}
.calendar-table {
  border-collapse: collapse;
  font-size: 13px;
  min-width: 100%;
}
.calendar-table th,
.calendar-table td {
  border: 1px solid #ebeef5;
  padding: 6px 4px;
  text-align: center;
  white-space: nowrap;
  min-width: 48px;
}
.calendar-table thead th {
  background: #f5f7fa;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 1;
}
.sticky-col {
  position: sticky;
  left: 0;
  background: #fff;
  z-index: 2;
  text-align: left;
  padding-left: 12px !important;
  min-width: 140px;
}
.emp-cell {
  font-weight: 500;
}
.weekend-col {
  background: #fdf6ec;
}
.no-shift {
  color: #c0c4cc;
}
</style>
