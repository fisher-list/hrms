<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getTeam,
  getTeamLeaveRequests,
  getMssTodo,
  approveTask,
  rejectTask,
} from '@/api/portal';
import type { ApprovalTaskVo, LeaveRequestVo } from '@/types/portal';

const activeTab = ref('todo');
const loading = ref(false);

const todoTasks = ref<ApprovalTaskVo[]>([]);
const teamMembers = ref<Record<string, unknown>[]>([]);
const teamLeaves = ref<LeaveRequestVo[]>([]);

async function loadTodo(): Promise<void> {
  loading.value = true;
  try {
    todoTasks.value = await getMssTodo();
  } catch {
    ElMessage.error('加载待办失败');
  } finally {
    loading.value = false;
  }
}

async function loadTeam(): Promise<void> {
  loading.value = true;
  try {
    const page = await getTeam();
    teamMembers.value = page.records;
  } catch {
    ElMessage.error('加载团队失败');
  } finally {
    loading.value = false;
  }
}

async function loadTeamLeaves(): Promise<void> {
  loading.value = true;
  try {
    const page = await getTeamLeaveRequests();
    teamLeaves.value = page.records;
  } catch {
    ElMessage.error('加载团队请假失败');
  } finally {
    loading.value = false;
  }
}

async function onApprove(task: ApprovalTaskVo): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入审批意见', '通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      inputValidator: (v) => (v && v.trim().length > 0 ? true : '审批意见不能为空'),
    });
    await approveTask(task.id, value);
    ElMessage.success('审批已通过');
    void loadTodo();
  } catch {
    // user cancelled or backend error already toasted
  }
}

async function onReject(task: ApprovalTaskVo): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputValidator: (v) => (v && v.trim().length > 0 ? true : '驳回原因不能为空'),
    });
    await rejectTask(task.id, value);
    ElMessage.success('已驳回');
    void loadTodo();
  } catch {
    // ignore
  }
}

function onTabChange(name: string | number): void {
  const key = String(name);
  if (key === 'todo') void loadTodo();
  else if (key === 'team') void loadTeam();
  else if (key === 'leaves') void loadTeamLeaves();
}

onMounted(() => {
  void loadTodo();
});
</script>

<template>
  <div class="mss-portal">
    <h2 class="title">经理自助 (MSS)</h2>
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="待审批" name="todo">
        <el-table v-loading="loading" :data="todoTasks" border stripe>
          <el-table-column prop="id" label="任务ID" width="160" />
          <el-table-column prop="instanceId" label="审批单ID" width="160" />
          <el-table-column prop="nodeName" label="节点" min-width="140" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="createdAt" label="创建时间" width="200" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="onApprove(row)">通过</el-button>
              <el-button type="danger" size="small" @click="onReject(row)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="团队成员" name="team">
        <el-table v-loading="loading" :data="teamMembers" border stripe>
          <el-table-column prop="id" label="ID" width="120" />
          <el-table-column prop="empNo" label="工号" width="140" />
          <el-table-column prop="name" label="姓名" width="140" />
          <el-table-column prop="deptName" label="部门" min-width="160" />
          <el-table-column prop="positionName" label="岗位" min-width="160" />
          <el-table-column prop="status" label="状态" width="140" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="团队请假" name="leaves">
        <el-table v-loading="loading" :data="teamLeaves" border stripe>
          <el-table-column prop="id" label="ID" width="120" />
          <el-table-column prop="employeeId" label="员工ID" width="120" />
          <el-table-column prop="startDate" label="开始" width="140" />
          <el-table-column prop="endDate" label="结束" width="140" />
          <el-table-column prop="totalDays" label="天数" width="100" />
          <el-table-column prop="reason" label="原因" min-width="160" />
          <el-table-column prop="status" label="状态" width="120" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.mss-portal {
  padding: 24px;
}
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
