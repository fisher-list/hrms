import { request } from './http';
import type { ApprovalTaskVo, ApprovalHistoryVo } from '@/types/portal';

// ==================== 审批待办 ====================
export function getApprovalTodo(): Promise<ApprovalTaskVo[]> {
  return request<ApprovalTaskVo[]>({ url: '/approval/todo', method: 'GET' });
}

export function approveTask(taskId: number, comment?: string): Promise<void> {
  return request<void>({
    url: `/approval/tasks/${taskId}/approve`,
    method: 'POST',
    data: { comment },
  });
}

export function rejectTask(taskId: number, comment?: string): Promise<void> {
  return request<void>({
    url: `/approval/tasks/${taskId}/reject`,
    method: 'POST',
    data: { comment },
  });
}

export function revokeTask(taskId: number): Promise<void> {
  return request<void>({
    url: `/approval/tasks/${taskId}/revoke`,
    method: 'POST',
  });
}

export function getApprovalHistory(instanceId: number): Promise<ApprovalHistoryVo[]> {
  return request<ApprovalHistoryVo[]>({
    url: `/approval/instances/${instanceId}/history`,
    method: 'GET',
  });
}
