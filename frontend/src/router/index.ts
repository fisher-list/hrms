import {
  createRouter,
  createWebHistory,
  type RouteLocationNormalized,
  type RouteRecordRaw,
} from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { usePermissionStore } from '@/store/permission';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/change-password',
    name: 'change-password',
    component: () => import('@/views/auth/ChangePassword.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true },
  },
  // ==================== 组织管理 ====================
  {
    path: '/org/tree',
    name: 'org-tree',
    component: () => import('@/views/org/OrgTree.vue'),
    meta: { requiresAuth: true, permission: 'org:view' },
  },
  {
    path: '/org/departments',
    name: 'org-departments',
    component: () => import('@/views/org/DepartmentManage.vue'),
    meta: { requiresAuth: true, permission: 'org:view' },
  },
  {
    path: '/org/positions',
    name: 'org-positions',
    component: () => import('@/views/org/PositionManage.vue'),
    meta: { requiresAuth: true, permission: 'org:view' },
  },
  // ==================== 考勤管理 ====================
  {
    path: '/attendance/punches',
    name: 'attendance-punches',
    component: () => import('@/views/attendance/TimePunch.vue'),
    meta: { requiresAuth: true, permission: 'at:punch:list' },
  },
  {
    path: '/attendance/leaves',
    name: 'attendance-leaves',
    component: () => import('@/views/attendance/LeaveRequest.vue'),
    meta: { requiresAuth: true, permission: 'at:leave:list' },
  },
  {
    path: '/attendance/leave/new',
    name: 'attendance-leave-new',
    component: () => import('@/views/attendance/LeaveCreate.vue'),
    meta: { requiresAuth: true, permission: 'at:leave:submit' },
  },
  {
    path: '/attendance/overtime',
    name: 'attendance-overtime',
    component: () => import('@/views/attendance/OvertimeRequest.vue'),
    meta: { requiresAuth: true, permission: 'at:overtime:list' },
  },
  {
    path: '/attendance/shifts',
    name: 'attendance-shifts',
    component: () => import('@/views/attendance/ShiftSchedule.vue'),
    meta: { requiresAuth: true, permission: 'at:shift:list' },
  },
  {
    path: '/attendance/settlement',
    name: 'attendance-settlement',
    component: () => import('@/views/attendance/LeaveSettlement.vue'),
    meta: { requiresAuth: true, permission: 'at:settlement:list' },
  },
  // ==================== 招聘管理 ====================
  {
    path: '/recruit/requisitions',
    name: 'recruit-requisitions',
    component: () => import('@/views/recruit/RecruitRequisition.vue'),
    meta: { requiresAuth: true, permission: 'rc:requisition:list' },
  },
  {
    path: '/recruit/candidates',
    name: 'recruit-candidates',
    component: () => import('@/views/recruit/RecruitCandidate.vue'),
    meta: { requiresAuth: true, permission: 'rc:candidate:list' },
  },
  {
    path: '/recruit/offers',
    name: 'recruit-offers',
    component: () => import('@/views/recruit/RecruitOffer.vue'),
    meta: { requiresAuth: true, permission: 'rc:candidate:list' },
  },
  // ==================== 薪酬管理 ====================
  {
    path: '/payroll/runs',
    name: 'payroll-runs',
    component: () => import('@/views/payroll/PayrollRuns.vue'),
    meta: { requiresAuth: true, permission: 'py:run:list' },
  },
  {
    path: '/payroll/compensations',
    name: 'payroll-compensations',
    component: () => import('@/views/payroll/Compensation.vue'),
    meta: { requiresAuth: true, permission: 'py:compensation:list' },
  },
  {
    path: '/payroll/payslips',
    name: 'payroll-payslips',
    component: () => import('@/views/payroll/Payslip.vue'),
    meta: { requiresAuth: true, permission: 'py:payslip:list' },
  },
  // ==================== 审批 ====================
  {
    path: '/approval/todo',
    name: 'approval-todo',
    component: () => import('@/views/approval/ApprovalList.vue'),
    meta: { requiresAuth: true, permission: 'approval:view' },
  },
  // ==================== HR ====================
  {
    path: '/hr/employees',
    name: 'hr-employees',
    component: () => import('@/views/hr/EmployeeList.vue'),
    meta: { requiresAuth: true, permission: 'hr:employee:list' },
  },
  // ==================== 绩效 ====================
  {
    path: '/performance/reviews',
    name: 'performance-reviews',
    component: () => import('@/views/performance/PerformanceReview.vue'),
    meta: { requiresAuth: true, permission: 'pf:appraisal:list' },
  },
  // ==================== 门户 ====================
  {
    path: '/portal/ess',
    name: 'portal-ess',
    component: () => import('@/views/portal/Ess.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/portal/mss',
    name: 'portal-mss',
    component: () => import('@/views/portal/Mss.vue'),
    meta: { requiresAuth: true, permission: 'mss:todo:list' },
  },
  // ==================== 系统 ====================
  {
    path: '/system/roles',
    name: 'system-roles',
    component: () => import('@/views/system/Role.vue'),
    meta: { requiresAuth: true, permission: 'role:view' },
  },
  // ==================== 错误页 ====================
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// Global auth guard: redirect to /login with redirect query when not authed.
router.beforeEach(async (to: RouteLocationNormalized) => {
  const auth = useAuthStore();
  const requiresAuth = to.meta.requiresAuth !== false;

  // Not authenticated → try to restore via refreshToken cookie (page refresh scenario)
  if (requiresAuth && !auth.isAuthenticated) {
    const restored = await auth.tryRestore();
    if (!restored) {
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      };
    }
    // After restoring the token, fetch /me to sync forceChangePassword flag
    await auth.fetchMe();
  }

  // Already authenticated visiting /login → redirect to dashboard
  if (to.path === '/login' && auth.isAuthenticated) {
    return { path: '/dashboard' };
  }

  // Force password change: if the flag is set, only allow /change-password and /login
  if (auth.isAuthenticated && auth.forceChangePassword && to.path !== '/change-password') {
    return { path: '/change-password' };
  }

  // Permission check for routes declaring meta.permission
  if (auth.isAuthenticated && to.meta.permission) {
    const permStore = usePermissionStore();
    if (!permStore.loaded) {
      await permStore.fetchPermissions();
    }
    const required = to.meta.permission as string;
    if (!permStore.has(required)) {
      return { path: '/403' };
    }
  }

  return true;
});

export default router;
