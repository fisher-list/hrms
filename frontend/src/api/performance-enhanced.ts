/** 绩效目标分解 + 薪酬调整联动 API */

import { request } from './http';

// ==================== 类型定义 ====================

/** 绩效目标计划 */
export interface GoalPlanVo {
  id: number;
  parentId?: number;
  cycleId: number;
  goalLevel: string;
  ownerType: string;
  ownerId: number;
  title: string;
  description?: string;
  weight: number;
  targetValue?: number;
  actualValue?: number;
  status: string;
  children?: GoalPlanVo[];
}

/** 薪酬调整建议 */
export interface SalaryAdjustmentVo {
  id: number;
  employeeId: number;
  employeeName?: string;
  empNo?: string;
  appraisalId: number;
  cycleId: number;
  grade: string;
  finalScore: number;
  currentBaseSalary: number;
  adjustmentPct: number;
  suggestedSalary: number;
  effectiveDate?: string;
  status: string;
  remark?: string;
}

/** 创建目标请求 */
export interface GoalPlanCreateReq {
  cycleId: number;
  goalLevel: string;
  ownerType: string;
  ownerId: number;
  goals: { title: string; description?: string; weight: number; targetValue?: number }[];
}

/** 目标分解请求 */
export interface GoalDecomposeReq {
  parentGoalId: number;
  subGoals: {
    title: string;
    description?: string;
    weight: number;
    targetValue?: number;
    ownerType: string;
    ownerId: number;
  }[];
}

// ==================== 目标分解 API ====================

/** 创建顶层目标 */
export function createGoalPlans(data: GoalPlanCreateReq): Promise<GoalPlanVo[]> {
  return request<GoalPlanVo[]>({ url: '/performance/goal-plans', method: 'POST', data });
}

/** 分解目标（上级→下级） */
export function decomposeGoal(data: GoalDecomposeReq): Promise<GoalPlanVo[]> {
  return request<GoalPlanVo[]>({ url: '/performance/goal-plans/decompose', method: 'POST', data });
}

/** 确认目标 */
export function confirmGoalPlan(id: number): Promise<void> {
  return request<void>({ url: `/performance/goal-plans/${id}/confirm`, method: 'POST' });
}

/** 获取目标树 */
export function getGoalTree(cycleId: number): Promise<GoalPlanVo[]> {
  return request<GoalPlanVo[]>({ url: '/performance/goal-plans/tree', method: 'GET', params: { cycleId } });
}

/** 获取下级子目标 */
export function getGoalDescendants(id: number): Promise<GoalPlanVo[]> {
  return request<GoalPlanVo[]>({ url: `/performance/goal-plans/${id}/descendants`, method: 'GET' });
}

/** 更新实际完成值 */
export function updateGoalActualValue(id: number, actualValue: number): Promise<void> {
  return request<void>({
    url: `/performance/goal-plans/${id}/actual-value`,
    method: 'PUT',
    params: { actualValue },
  });
}

// ==================== 薪酬调整 API ====================

/** 为单个考核生成调薪建议 */
export function generateSalaryAdjustment(appraisalId: number): Promise<SalaryAdjustmentVo> {
  return request<SalaryAdjustmentVo>({
    url: `/performance/salary-adjustments/generate/${appraisalId}`,
    method: 'POST',
  });
}

/** 批量生成调薪建议 */
export function batchGenerateSalaryAdjustments(cycleId: number): Promise<SalaryAdjustmentVo[]> {
  return request<SalaryAdjustmentVo[]>({
    url: '/performance/salary-adjustments/batch-generate',
    method: 'POST',
    params: { cycleId },
  });
}

/** 查询调薪建议列表 */
export function listSalaryAdjustments(params?: {
  employeeId?: number;
  cycleId?: number;
  status?: string;
}): Promise<SalaryAdjustmentVo[]> {
  return request<SalaryAdjustmentVo[]>({
    url: '/performance/salary-adjustments',
    method: 'GET',
    params,
  });
}

/** 审批通过调薪建议 */
export function approveSalaryAdjustment(id: number): Promise<void> {
  return request<void>({ url: `/performance/salary-adjustments/${id}/approve`, method: 'POST' });
}

/** 拒绝调薪建议 */
export function rejectSalaryAdjustment(id: number, reason?: string): Promise<void> {
  return request<void>({
    url: `/performance/salary-adjustments/${id}/reject`,
    method: 'POST',
    params: { reason },
  });
}

/** 执行调薪 */
export function executeSalaryAdjustment(id: number): Promise<void> {
  return request<void>({ url: `/performance/salary-adjustments/${id}/execute`, method: 'POST' });
}
