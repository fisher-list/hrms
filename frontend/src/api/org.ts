import { request } from './http';
import type { CompanyVo, DepartmentTreeVo, DepartmentReq, PositionVo, PositionReq } from '@/types/org-payroll';

// ==================== 公司 ====================
export function getCompany(): Promise<CompanyVo> {
  return request<CompanyVo>({ url: '/company', method: 'GET' });
}

// ==================== 部门 ====================
export function getDeptTree(companyId: number): Promise<DepartmentTreeVo[]> {
  return request<DepartmentTreeVo[]>({
    url: '/departments/tree',
    method: 'GET',
    params: { companyId },
  });
}

export function createDept(req: DepartmentReq): Promise<DepartmentTreeVo> {
  return request<DepartmentTreeVo>({ url: '/departments', method: 'POST', data: req });
}

export function updateDept(id: number, req: DepartmentReq): Promise<DepartmentTreeVo> {
  return request<DepartmentTreeVo>({ url: `/departments/${id}`, method: 'PUT', data: req });
}

export function deleteDept(id: number): Promise<void> {
  return request<void>({ url: `/departments/${id}`, method: 'DELETE' });
}

// ==================== 岗位 ====================
export function listPositions(deptId?: number): Promise<PositionVo[]> {
  return request<PositionVo[]>({
    url: '/positions',
    method: 'GET',
    params: deptId != null ? { deptId } : undefined,
  });
}

export function getPosition(id: number): Promise<PositionVo> {
  return request<PositionVo>({ url: `/positions/${id}`, method: 'GET' });
}

export function createPosition(req: PositionReq): Promise<PositionVo> {
  return request<PositionVo>({ url: '/positions', method: 'POST', data: req });
}

export function updatePosition(id: number, req: PositionReq): Promise<PositionVo> {
  return request<PositionVo>({ url: `/positions/${id}`, method: 'PUT', data: req });
}

export function deletePosition(id: number): Promise<void> {
  return request<void>({ url: `/positions/${id}`, method: 'DELETE' });
}
