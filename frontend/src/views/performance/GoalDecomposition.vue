<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  listPerformanceCycles,
} from '@/api/performance';
import {
  createGoalPlans,
  decomposeGoal,
  confirmGoalPlan,
  getGoalTree,
  type GoalPlanVo,
  type GoalPlanCreateReq,
} from '@/api/performance-enhanced';
import type { PerformanceCycleVo } from '@/types/talent';

const cycles = ref<PerformanceCycleVo[]>([]);
const selectedCycleId = ref<number>();
const goalTree = ref<GoalPlanVo[]>([]);
const loading = ref(false);

// 创建目标对话框
const createDialog = ref(false);
const createForm = ref<GoalPlanCreateReq>({
  cycleId: 0,
  goalLevel: 'ORG',
  ownerType: 'ORG',
  ownerId: 1,
  goals: [{ title: '', description: '', weight: 100, targetValue: undefined }],
});

// 分解目标对话框
const decomposeDialog = ref(false);
const decomposeParent = ref<GoalPlanVo | null>(null);
const decomposeSubGoals = ref([
  { title: '', description: '', weight: 0, targetValue: undefined as number | undefined, ownerType: 'DEPT', ownerId: 0 },
]);

const LEVEL_LABEL: Record<string, string> = { ORG: '组织级', DEPT: '部门级', PERSON: '个人级' };
const STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  DRAFT: 'info', CONFIRMED: 'success', IN_PROGRESS: 'warning', COMPLETED: 'success',
};

async function loadCycles(): Promise<void> {
  try {
    cycles.value = await listPerformanceCycles();
  } catch { /* ignore */ }
}

async function loadGoalTree(): Promise<void> {
  if (!selectedCycleId.value) return;
  loading.value = true;
  try {
    goalTree.value = await getGoalTree(selectedCycleId.value);
  } catch {
    ElMessage.error('加载目标树失败');
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  createForm.value = {
    cycleId: selectedCycleId.value || 0,
    goalLevel: 'ORG',
    ownerType: 'ORG',
    ownerId: 1,
    goals: [{ title: '', description: '', weight: 100, targetValue: undefined }],
  };
  createDialog.value = true;
}

async function onCreateGoals(): Promise<void> {
  try {
    await createGoalPlans(createForm.value);
    ElMessage.success('目标创建成功');
    createDialog.value = false;
    await loadGoalTree();
  } catch { /* ignore */ }
}

function addCreateGoal(): void {
  createForm.value.goals.push({ title: '', description: '', weight: 0, targetValue: undefined });
}

function openDecompose(node: GoalPlanVo): void {
  decomposeParent.value = node;
  decomposeSubGoals.value = [
    { title: '', description: '', weight: 0, targetValue: undefined, ownerType: node.goalLevel === 'ORG' ? 'DEPT' : 'PERSON', ownerId: 0 },
  ];
  decomposeDialog.value = true;
}

async function onDecompose(): Promise<void> {
  if (!decomposeParent.value) return;
  const totalWeight = decomposeSubGoals.value.reduce((s, g) => s + g.weight, 0);
  if (totalWeight !== decomposeParent.value.weight) {
    ElMessage.warning(`子目标权重之和(${totalWeight})必须等于父目标权重(${decomposeParent.value.weight})`);
    return;
  }
  try {
    await decomposeGoal({
      parentGoalId: decomposeParent.value.id,
      subGoals: decomposeSubGoals.value,
    });
    ElMessage.success('目标分解成功');
    decomposeDialog.value = false;
    await loadGoalTree();
  } catch { /* ignore */ }
}

function addSubGoal(): void {
  const level = decomposeParent.value?.goalLevel === 'ORG' ? 'DEPT' : 'PERSON';
  decomposeSubGoals.value.push({ title: '', description: '', weight: 0, targetValue: undefined, ownerType: level, ownerId: 0 });
}

async function onConfirm(node: GoalPlanVo): Promise<void> {
  try {
    await confirmGoalPlan(node.id);
    ElMessage.success('目标已确认');
    await loadGoalTree();
  } catch { /* ignore */ }
}

onMounted(async () => {
  await loadCycles();
});
</script>

<template>
  <div class="page-container">
    <div class="header">
      <h2 class="title">绩效目标分解</h2>
      <div>
        <el-select v-model="selectedCycleId" placeholder="选择绩效周期" style="width: 200px; margin-right: 12px;" @change="loadGoalTree">
          <el-option v-for="c in cycles" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" :disabled="!selectedCycleId" @click="openCreate">创建目标</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>目标树</template>
      <el-table v-loading="loading" :data="goalTree" border stripe row-key="id" default-expand-all>
        <el-table-column prop="title" label="目标名称" min-width="200" />
        <el-table-column prop="goalLevel" label="层级" width="100">
          <template #default="{ row }">
            <el-tag>{{ LEVEL_LABEL[row.goalLevel] || row.goalLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="权重" width="80" />
        <el-table-column prop="targetValue" label="目标值" width="100" />
        <el-table-column prop="actualValue" label="实际值" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.goalLevel !== 'PERSON'" size="small" type="primary" @click="openDecompose(row)">分解</el-button>
            <el-button v-if="row.status === 'DRAFT'" size="small" type="success" @click="onConfirm(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建目标对话框 -->
    <el-dialog v-model="createDialog" title="创建顶层目标" width="700px">
      <el-form label-width="100px">
        <el-form-item label="目标层级">
          <el-radio-group v-model="createForm.goalLevel">
            <el-radio-button label="ORG">组织级</el-radio-button>
            <el-radio-button label="DEPT">部门级</el-radio-button>
            <el-radio-button label="PERSON">个人级</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所有者ID">
          <el-input-number v-model="createForm.ownerId" :min="1" />
        </el-form-item>
        <el-divider>目标列表</el-divider>
        <div v-for="(g, idx) in createForm.goals" :key="idx" style="display: flex; gap: 12px; margin-bottom: 12px;">
          <el-input v-model="g.title" placeholder="目标名称" style="flex: 2;" />
          <el-input v-model="g.description" placeholder="描述" style="flex: 2;" />
          <el-input-number v-model="g.weight" :min="0" :max="100" placeholder="权重" style="flex: 1;" />
          <el-input-number v-model="g.targetValue" :min="0" placeholder="目标值" style="flex: 1;" />
        </div>
        <el-button @click="addCreateGoal">新增目标</el-button>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="onCreateGoals">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分解目标对话框 -->
    <el-dialog v-model="decomposeDialog" title="分解目标" width="750px">
      <p>父目标：<strong>{{ decomposeParent?.title }}</strong>（权重: {{ decomposeParent?.weight }}）</p>
      <el-divider>子目标列表</el-divider>
      <div v-for="(g, idx) in decomposeSubGoals" :key="idx" style="display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap;">
        <el-input v-model="g.title" placeholder="子目标名称" style="flex: 2; min-width: 120px;" />
        <el-input-number v-model="g.weight" :min="0" :max="100" placeholder="权重" style="flex: 1; min-width: 100px;" />
        <el-input-number v-model="g.ownerId" :min="1" placeholder="所有者ID" style="flex: 1; min-width: 100px;" />
      </div>
      <el-button @click="addSubGoal">新增子目标</el-button>
      <template #footer>
        <el-button @click="decomposeDialog = false">取消</el-button>
        <el-button type="primary" @click="onDecompose">确认分解</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; }
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.title { margin: 0; font-size: 18px; font-weight: 600; }
</style>
