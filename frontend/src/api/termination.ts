import { request } from './http';
import type { PageVo } from '@/types/portal';

// ==================== 离职管理 ====================

/** 离职类型枚举 */
export type TerminationType = 'RESIGNATION' | 'DISMISSAL' | 'MUTUAL' | 'CONTRACT_EXPIRY' | 'RETIREMENT';

/** 离职申请创建请求 */
export interface TerminationFormCreateReq {
  employeeId: number;
  terminationType: TerminationType;
  reason?: string;
  lastWorkingDay: string;
}

/** 离职申请VO */
export interface TerminationFormVo {
  id: number;
  formNo: string;
  employeeId: number;
  terminationType: string;
  reason?: string;
  lastWorkingDay?: string;
  status: string;
  approvalInstanceId?: number;
  createdAt?: string;
}

/** 离职交接项 */
export interface HandoverItemVo {
  id: number;
  formId: number;
  category: string;
  description: string;
  handoverTo?: string;
  status: string;
  remark?: string;
  createdAt?: string;
}

/** 离职交接项创建请求 */
export interface HandoverItemCreateReq {
  formId: number;
  category: string;
  description: string;
  handoverTo?: string;
  remark?: string;
}

/** 创建离职申请 */
export function createTerminationForm(data: TerminationFormCreateReq): Promise<TerminationFormVo> {
  return request<TerminationFormVo>({
    url: '/hr/termination-forms',
    method: 'POST',
    data,
  });
}

/** 提交离职申请审批 */
export function submitTerminationForm(id: number): Promise<void> {
  return request<void>({
    url: `/hr/termination-forms/${id}/submit`,
    method: 'POST',
  });
}

/** 查询离职申请列表 */
export function listTerminationForms(
  status?: string,
  current = 1,
  size = 10,
): Promise<PageVo<TerminationFormVo>> {
  return request<PageVo<TerminationFormVo>>({
    url: '/hr/termination-forms',
    method: 'GET',
    params: { status, current, size },
  });
}

/** 获取离职申请详情 */
export function getTerminationForm(id: number): Promise<TerminationFormVo> {
  return request<TerminationFormVo>({
    url: `/hr/termination-forms/${id}`,
    method: 'GET',
  });
}

// ==================== 工作交接 ====================

/** 创建交接项 */
export function createHandoverItem(data: HandoverItemCreateReq): Promise<HandoverItemVo> {
  return request<HandoverItemVo>({
    url: '/hr/handover-items',
    method: 'POST',
    data,
  });
}

/** 查询离职交接清单 */
export function listHandoverItems(formId: number): Promise<HandoverItemVo[]> {
  return request<HandoverItemVo[]>({
    url: `/hr/handover-items`,
    method: 'GET',
    params: { formId },
  });
}

/** 更新交接项状态 */
export function updateHandoverItemStatus(id: number, status: string): Promise<void> {
  return request<void>({
    url: `/hr/handover-items/${id}/status`,
    method: 'PUT',
    params: { status },
  });
}
