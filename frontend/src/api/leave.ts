import { request } from './http';
import type { LeaveTypeVo, LeaveRequestCreateReq } from '@/types/hr';
import type { LeaveRequestVo } from '@/types/portal';

export function listLeaveTypes(): Promise<LeaveTypeVo[]> {
  return request<LeaveTypeVo[]>({ url: '/attendance/leave-types', method: 'GET' });
}

export function createLeaveRequest(req: LeaveRequestCreateReq): Promise<LeaveRequestVo> {
  return request<LeaveRequestVo>({
    url: '/attendance/leave-requests',
    method: 'POST',
    data: req,
  });
}

export function submitLeaveRequest(id: number): Promise<void> {
  return request<void>({
    url: `/attendance/leave-requests/${id}/submit`,
    method: 'POST',
  });
}
