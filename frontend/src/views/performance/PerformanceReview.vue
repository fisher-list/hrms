<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  listPerformanceCycles,
  createPerformanceCycle,
  activatePerformanceCycle,
  closePerformanceCycle,
  listAppraisals,
  setAppraisalGoals,
  confirmAppraisalGoals,
  submitSelfReview,
  submitManagerReview,
  finalizeAppraisal,
} from '@/api/performance';
import type { PerformanceCycleVo, AppraisalVo, GoalItemReq, ReviewItemReq } from '@/types/talent';

const cycles = ref<PerformanceCycleVo[]>([]);
const appraisals = ref<AppraisalVo[]>([]);
const selectedCycleId = ref<number | undefined>();
const loadingCycles = ref(false);
const loadingAppraisals = ref(false);

const cycleDialog = ref(false);
const cycleFormRef = ref<FormInstance>();
const cycleForm = reactive({
  name: '',
  range: [] as string[],
  scopeType: 'ALL',
  scopeDepts: '',
});

const cycleRules: FormRules = {
  name: [{ required: true, message: '请输入周期名称', trigger: 'blur' }],
  range: [{ type: 'array', required: true, message: '请选择日期范围', trigger: 'change' }],
};

const goalDialog = ref(false);
const reviewDialog = ref(false);
const reviewMode = ref<'self' | 'manager'>('self');
const activeAppraisal = ref<AppraisalVo | null>(null);

const goals = ref<GoalItemReq[]>([
  { description: '工作质量', weight: 40 },
  { description: '团队协作', weight: 30 },
  { description: '创新能力', weight: 30 },
]);

const reviews = ref<ReviewItemReq[]>([
  { scoringItemId: 1, score: 4, comment: '' },
  { scoringItemId: 2, score: 4, comment: '' },
  { scoringItemId: 3, score: 4, comment: '' },
]);

const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  DRAFT: 'info',
  ACTIVE: 'success',
  CLOSED: 'danger',
  GOAL_SET: 'warning',
  GOAL_CONFIRMED: 'warning',
  SELF_REVIEWED: 'warning',
  MANAGER_REVIEWED: 'success',
  COMPLETED: 'success',
};

async function loadCycles(): Promise<void> {
  loadingCycles.value = true;
  try {
    cycles.value = await listPerformanceCycles();
  } catch {
    ElMessage.error('加载绩效周期失败');
  } finally {
    loadingCycles.value = false;
  }
}

async function loadAppraisals(): Promise<void> {
  loadingAppraisals.value = true;
  try {
    appraisals.value = await listAppraisals(selectedCycleId.value);
  } catch {
    ElMessage.error('加载绩效单失败');
  } finally {
    loadingAppraisals.value = false;
  }
}

async function onCreateCycle(): Promise<void> {
  const valid = await cycleFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  try {
    await createPerformanceCycle({
      name: cycleForm.name,
      startDate: cycleForm.range[0],
      endDate: cycleForm.range[1],
      scopeType: cycleForm.scopeType,
      scopeDepts: cycleForm.scopeDepts,
    });
    ElMessage.success('周期已创建');
    cycleDialog.value = false;
    void loadCycles();
  } catch {
    // ignore
  }
}

async function onActivate(cycle: PerformanceCycleVo): Promise<void> {
  try {
    await activatePerformanceCycle(cycle.id);
    ElMessage.success('周期已激活并生成绩效单');
    await loadCycles();
    selectedCycleId.value = cycle.id;
    await loadAppraisals();
  } catch {
    // ignore
  }
}

async function onClose(cycle: PerformanceCycleVo): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认关闭周期 ${cycle.name}？`, '关闭确认', { type: 'warning' });
    await closePerformanceCycle(cycle.id);
    ElMessage.success('周期已关闭');
    void loadCycles();
  } catch {
    // ignore
  }
}

function openGoals(row: AppraisalVo): void {
  activeAppraisal.value = row;
  goals.value = [
    { description: '工作质量', weight: 40 },
    { description: '团队协作', weight: 30 },
    { description: '创新能力', weight: 30 },
  ];
  goalDialog.value = true;
}

async function onSubmitGoals(): Promise<void> {
  if (!activeAppraisal.value) return;
  const total = goals.value.reduce((sum, item) => sum + Number(item.weight || 0), 0);
  if (total !== 100) {
    ElMessage.warning('目标权重合计必须等于 100');
    return;
  }
  try {
    await setAppraisalGoals(activeAppraisal.value.id, goals.value);
    await confirmAppraisalGoals(activeAppraisal.value.id);
    ElMessage.success('目标已提交并确认');
    goalDialog.value = false;
    void loadAppraisals();
  } catch {
    // ignore
  }
}

function openReview(row: AppraisalVo, mode: 'self' | 'manager'): void {
  activeAppraisal.value = row;
  reviewMode.value = mode;
  reviews.value = [
    { scoringItemId: 1, score: 4, comment: '' },
    { scoringItemId: 2, score: 4, comment: '' },
    { scoringItemId: 3, score: 4, comment: '' },
  ];
  reviewDialog.value = true;
}

async function onSubmitReview(): Promise<void> {
  if (!activeAppraisal.value) return;
  try {
    if (reviewMode.value === 'self') {
      await submitSelfReview(activeAppraisal.value.id, reviews.value);
      ElMessage.success('自评已提交');
    } else {
      await submitManagerReview(activeAppraisal.value.id, reviews.value);
      ElMessage.success('上级评分已提交');
    }
    reviewDialog.value = false;
    void loadAppraisals();
  } catch {
    // ignore
  }
}

async function onFinalize(row: AppraisalVo): Promise<void> {
  try {
    await finalizeAppraisal(row.id);
    ElMessage.success('绩效单已完成');
    void loadAppraisals();
  } catch {
    // ignore
  }
}

function addGoal(): void {
  goals.value.push({ description: '', weight: 0 });
}

function removeGoal(index: number): void {
  goals.value.splice(index, 1);
}

onMounted(async () => {
  await loadCycles();
  await loadAppraisals();
});
</script>

<template>
  <div class="performance-page">
    <div class="header">
      <h2 class="title">绩效评审</h2>
      <el-button type="primary" @click="cycleDialog = true">新建周期</el-button>
    </div>

    <el-card shadow="never" class="section">
      <template #header>绩效周期</template>
      <el-table v-loading="loadingCycles" :data="cycles" border stripe>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="startDate" label="开始" width="140" />
        <el-table-column prop="endDate" label="结束" width="140" />
        <el-table-column prop="scopeType" label="范围" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="selectedCycleId = row.id; loadAppraisals()">
              查看绩效单
            </el-button>
            <el-button v-if="row.status === 'DRAFT'" type="primary" size="small" @click="onActivate(row)">
              激活
            </el-button>
            <el-button v-if="row.status === 'ACTIVE'" type="danger" size="small" @click="onClose(row)">
              关闭
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="section">
      <template #header>绩效单</template>
      <el-table v-loading="loadingAppraisals" :data="appraisals" border stripe>
        <el-table-column prop="id" label="绩效单ID" width="120" />
        <el-table-column prop="cycleId" label="周期ID" width="100" />
        <el-table-column prop="employeeId" label="员工ID" width="120" />
        <el-table-column prop="status" label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="selfScore" label="自评分" width="100" />
        <el-table-column prop="managerScore" label="上级分" width="100" />
        <el-table-column prop="finalScore" label="最终分" width="100" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openGoals(row)">目标</el-button>
            <el-button size="small" @click="openReview(row, 'self')">自评</el-button>
            <el-button size="small" @click="openReview(row, 'manager')">上级评分</el-button>
            <el-button size="small" type="success" @click="onFinalize(row)">完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="cycleDialog" title="新建绩效周期" width="560px">
      <el-form ref="cycleFormRef" :model="cycleForm" :rules="cycleRules" label-width="100px">
        <el-form-item label="周期名称" prop="name"><el-input v-model="cycleForm.name" /></el-form-item>
        <el-form-item label="日期范围" prop="range">
          <el-date-picker v-model="cycleForm.range" type="daterange" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="适用范围">
          <el-radio-group v-model="cycleForm.scopeType">
            <el-radio-button label="ALL">全部员工</el-radio-button>
            <el-radio-button label="DEPT">指定部门</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="cycleForm.scopeType === 'DEPT'" label="部门ID">
          <el-input v-model="cycleForm.scopeDepts" placeholder="逗号分隔，如 1,2,3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cycleDialog = false">取消</el-button>
        <el-button type="primary" @click="onCreateCycle">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="goalDialog" title="设置目标" width="720px">
      <div v-for="(goal, index) in goals" :key="index" class="line-item">
        <el-input v-model="goal.description" placeholder="目标描述" />
        <el-input-number v-model="goal.weight" :min="0" :max="100" />
        <el-button type="danger" link @click="removeGoal(index)">删除</el-button>
      </div>
      <el-button @click="addGoal">新增目标</el-button>
      <template #footer>
        <el-button @click="goalDialog = false">取消</el-button>
        <el-button type="primary" @click="onSubmitGoals">提交并确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialog" :title="reviewMode === 'self' ? '提交自评' : '提交上级评分'" width="720px">
      <div v-for="item in reviews" :key="item.scoringItemId" class="line-item">
        <span class="score-id">评分项 {{ item.scoringItemId }}</span>
        <el-rate v-model="item.score" :max="5" />
        <el-input v-model="item.comment" placeholder="评价说明" />
      </div>
      <template #footer>
        <el-button @click="reviewDialog = false">取消</el-button>
        <el-button type="primary" @click="onSubmitReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.performance-page {
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
.section {
  margin-bottom: 16px;
}
.line-item {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
.score-id {
  width: 90px;
  color: #606266;
}
</style>
