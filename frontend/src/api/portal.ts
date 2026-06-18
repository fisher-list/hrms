import { request } from './http';
import type {
  LeaveBalanceVo,
  LeaveRequestVo,
  ApprovalTaskVo,
  PayslipVo,
  PageVo,
} from '@/types/portal';

// ESS — Employee Self-Service
export function getMyProfile(): Promise<Record<string, unknown>> {
  return request<Record<string, unknown>>({ url: '/portal/ess/me', method: 'GET' });
}

export function getMyLeaveBalances(year?: number): Promise<LeaveBalanceVo[]> {
  return request<LeaveBalanceVo[]>({
    url: '/portal/ess/leave-balances',
    method: 'GET',
    params: year != null ? { year } : undefined,
  });
}

export function getMyLeaveRequests(
  status?: string,
  current = 1,
  size = 10,
): Promise<PageVo<LeaveRequestVo>> {
  return request<PageVo<LeaveRequestVo>>({
    url: '/portal/ess/leave-requests',
    method: 'GET',
    params: { status, current, size },
  });
}

export function getMyTodo(): Promise<ApprovalTaskVo[]> {
  return request<ApprovalTaskVo[]>({ url: '/portal/ess/todo', method: 'GET' });
}

export function getMyPayslips(): Promise<PayslipVo[]> {
  return request<PayslipVo[]>({ url: '/portal/ess/payslips', method: 'GET' });
}

export function getMyPayslip(runId: number): Promise<PayslipVo> {
  return request<PayslipVo>({ url: `/portal/ess/payslips/${runId}`, method: 'GET' });
}

// MSS — Manager Self-Service
export function getTeam(
  keyword?: string,
  status?: string,
  current = 1,
  size = 10,
): Promise<PageVo<Record<string, unknown>>> {
  return request<PageVo<Record<string, unknown>>>({
    url: '/portal/mss/team',
    method: 'GET',
    params: { keyword, status, current, size },
  });
}

export function getTeamLeaveRequests(
  employeeId?: number,
  status?: string,
  current = 1,
  size = 10,
): Promise<PageVo<LeaveRequestVo>> {
  return request<PageVo<LeaveRequestVo>>({
    url: '/portal/mss/team/leave-requests',
    method: 'GET',
    params: { employeeId, status, current, size },
  });
}

export function getMssTodo(): Promise<ApprovalTaskVo[]> {
  return request<ApprovalTaskVo[]>({ url: '/portal/mss/todo', method: 'GET' });
}

export function approveTask(taskId: number, comment: string): Promise<void> {
  return request<void>({
    url: `/portal/mss/tasks/${taskId}/approve`,
    method: 'POST',
    data: { comment },
  });
}

export function rejectTask(taskId: number, comment: string): Promise<void> {
  return request<void>({
    url: `/portal/mss/tasks/${taskId}/reject`,
    method: 'POST',
    data: { comment },
  });
}
