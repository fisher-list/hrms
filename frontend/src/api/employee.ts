import { request } from './http';
import type { EmployeeListVo } from '@/types/hr';
import type { PageVo } from '@/types/portal';

export function listEmployees(
  keyword?: string,
  status?: string,
  current = 1,
  size = 10,
): Promise<PageVo<EmployeeListVo>> {
  return request<PageVo<EmployeeListVo>>({
    url: '/hr/employees',
    method: 'GET',
    params: { keyword, status, current, size },
  });
}

export function getEmployee(id: number): Promise<Record<string, unknown>> {
  return request<Record<string, unknown>>({
    url: `/hr/employees/${id}`,
    method: 'GET',
  });
}
