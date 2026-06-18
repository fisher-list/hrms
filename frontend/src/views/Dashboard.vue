<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';

const router = useRouter();
const auth = useAuthStore();

function onLogout(): void {
  auth.logout();
  ElMessage.success('已退出登录');
  void router.replace('/login');
}
</script>

<template>
  <div class="dashboard">
    <header class="dashboard-header">
      <span class="title">HRMS Dashboard</span>
      <span class="user">
        <!-- Use v-text / interpolation only; never v-html for user data. -->
        <span v-if="auth.user">欢迎,{{ auth.user.nickname || auth.user.username }}</span>
        <el-button type="primary" link @click="onLogout">退出登录</el-button>
      </span>
    </header>
    <main class="dashboard-body">
      <p>选择一个自助门户进入：</p>
      <div class="quick-links">
        <el-button type="primary" @click="$router.push('/portal/ess')">员工自助 (ESS)</el-button>
        <el-button type="primary" @click="$router.push('/portal/mss')">经理自助 (MSS)</el-button>
        <el-button @click="$router.push('/org/tree')">组织树</el-button>
        <el-button @click="$router.push('/hr/employees')">员工档案</el-button>
        <el-button @click="$router.push('/attendance/leave/new')">请假申请</el-button>
        <el-button @click="$router.push('/payroll/runs')">薪酬批次</el-button>
        <el-button @click="$router.push('/recruit/offers')">招聘 Offer</el-button>
        <el-button @click="$router.push('/performance/reviews')">绩效评审</el-button>
        <el-button @click="$router.push('/system/roles')">角色管理</el-button>
      </div>
    </main>
  </div>
</template>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: #fff;
}
.dashboard-header {
  height: 56px;
  padding: 0 24px;
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
  color: #606266;
}
.quick-links {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
