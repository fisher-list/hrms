import { request } from './http';
import type {
  PerformanceCycleVo,
  AppraisalVo,
  GoalItemReq,
  ReviewItemReq,
} from '@/types/talent';

export function listPerformanceCycles(): Promise<PerformanceCycleVo[]> {
  return request<PerformanceCycleVo[]>({ url: '/performance/cycles', method: 'GET' });
}

export function createPerformanceCycle(data: {
  name: string;
  startDate: string;
  endDate: string;
  scopeType?: string;
  scopeDepts?: string;
}): Promise<PerformanceCycleVo> {
  return request<PerformanceCycleVo>({ url: '/performance/cycles', method: 'POST', data });
}

export function activatePerformanceCycle(id: number): Promise<PerformanceCycleVo> {
  return request<PerformanceCycleVo>({ url: `/performance/cycles/${id}/activate`, method: 'POST' });
}

export function closePerformanceCycle(id: number): Promise<PerformanceCycleVo> {
  return request<PerformanceCycleVo>({ url: `/performance/cycles/${id}/close`, method: 'POST' });
}

export function listAppraisals(cycleId?: number, employeeId?: number): Promise<AppraisalVo[]> {
  return request<AppraisalVo[]>({
    url: '/performance/appraisals',
    method: 'GET',
    params: { cycleId, employeeId },
  });
}

export function setAppraisalGoals(id: number, goals: GoalItemReq[]): Promise<unknown[]> {
  return request<unknown[]>({
    url: `/performance/appraisals/${id}/goals`,
    method: 'POST',
    data: { goals },
  });
}

export function confirmAppraisalGoals(id: number): Promise<void> {
  return request<void>({ url: `/performance/appraisals/${id}/goals/confirm`, method: 'POST' });
}

export function submitSelfReview(id: number, reviews: ReviewItemReq[]): Promise<void> {
  return request<void>({
    url: `/performance/appraisals/${id}/self-review`,
    method: 'POST',
    data: { reviews },
  });
}

export function submitManagerReview(id: number, reviews: ReviewItemReq[]): Promise<void> {
  return request<void>({
    url: `/performance/appraisals/${id}/manager-review`,
    method: 'POST',
    data: { reviews },
  });
}

export function finalizeAppraisal(id: number): Promise<AppraisalVo> {
  return request<AppraisalVo>({ url: `/performance/appraisals/${id}/finalize`, method: 'POST' });
}
