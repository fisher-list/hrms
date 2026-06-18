import { request } from './http';
import type { EmployeeListVo } from '@/types/hr';
import type { PageVo } from '@/types/portal';

/** 员工列表查询 */
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

/** 获取员工详情 */
export function getEmployee(id: number): Promise<Record<string, unknown>> {
  return request<Record<string, unknown>>({
    url: `/hr/employees/${id}`,
    method: 'GET',
  });
}

// ==================== 合同续签 ====================

/** 即将到期合同VO */
export interface ExpiringContractVo {
  employeeId: number;
  empNo: string;
  name: string;
  deptId?: number;
  deptName?: string;
  contractId: number;
  contractNo?: string;
  contractType?: string;
  contractStartDate?: string;
  contractEndDate?: string;
  daysUntilExpiry: number;
  alertLevel: 'URGENT' | 'WARNING';
}

/** 续签申请VO */
export interface ContractRenewalVo {
  id: number;
  employeeId: number;
  originalContractId: number;
  renewalNo: string;
  newContractType: string;
  newStartDate: string;
  newEndDate?: string;
  remark?: string;
  status: string;
  approvalInstanceId?: number;
  createdAt?: string;
}

/** 续签申请创建请求 */
export interface ContractRenewalCreateReq {
  employeeId: number;
  originalContractId: number;
  newContractType: string;
  newStartDate: string;
  newEndDate?: string;
  remark?: string;
}

/** 获取即将到期的合同预警列表 */
export function listExpiringContracts(daysAhead = 30): Promise<ExpiringContractVo[]> {
  return request<ExpiringContractVo[]>({
    url: '/hr/contract-renewals/expiring',
    method: 'GET',
    params: { daysAhead },
  });
}

/** 创建合同续签申请 */
export function createContractRenewal(data: ContractRenewalCreateReq): Promise<ContractRenewalVo> {
  return request<ContractRenewalVo>({
    url: '/hr/contract-renewals',
    method: 'POST',
    data,
  });
}

/** 提交续签申请审批 */
export function submitContractRenewal(id: number): Promise<void> {
  return request<void>({
    url: `/hr/contract-renewals/${id}/submit`,
    method: 'POST',
  });
}

/** 查询续签申请列表 */
export function listContractRenewals(
  status?: string,
  current = 1,
  size = 10,
): Promise<PageVo<ContractRenewalVo>> {
  return request<PageVo<ContractRenewalVo>>({
    url: '/hr/contract-renewals',
    method: 'GET',
    params: { status, current, size },
  });
}

// ==================== 试用期转正 ====================

/** 即将到期试用期员工VO */
export interface ProbationEmployeeVo {
  employeeId: number;
  empNo: string;
  name: string;
  deptId?: number;
  deptName?: string;
  positionId?: number;
  hireDate?: string;
  probationEndDate?: string;
  daysUntilExpiry: number;
  alertLevel: 'URGENT' | 'WARNING';
  hasPendingConversion: boolean;
}

/** 转正申请VO */
export interface ProbationConversionVo {
  id: number;
  employeeId: number;
  conversionNo: string;
  probationStartDate?: string;
  probationEndDate?: string;
  plannedConversionDate?: string;
  evaluationRemark?: string;
  managerComment?: string;
  evaluationScore?: number;
  status: string;
  approvalInstanceId?: number;
  createdAt?: string;
}

/** 转正申请创建请求 */
export interface ProbationConversionCreateReq {
  employeeId: number;
  evaluationRemark?: string;
  plannedConversionDate?: string;
  evaluationScore?: number;
}

/** 获取即将到期试用期员工列表 */
export function listExpiringProbations(daysAhead = 30): Promise<ProbationEmployeeVo[]> {
  return request<ProbationEmployeeVo[]>({
    url: '/hr/probation-conversions/expiring',
    method: 'GET',
    params: { daysAhead },
  });
}

/** 创建转正申请 */
export function createProbationConversion(data: ProbationConversionCreateReq): Promise<ProbationConversionVo> {
  return request<ProbationConversionVo>({
    url: '/hr/probation-conversions',
    method: 'POST',
    data,
  });
}

/** 提交转正申请审批 */
export function submitProbationConversion(id: number): Promise<void> {
  return request<void>({
    url: `/hr/probation-conversions/${id}/submit`,
    method: 'POST',
  });
}

/** 查询转正申请列表 */
export function listProbationConversions(
  status?: string,
  current = 1,
  size = 10,
): Promise<PageVo<ProbationConversionVo>> {
  return request<PageVo<ProbationConversionVo>>({
    url: '/hr/probation-conversions',
    method: 'GET',
    params: { status, current, size },
  });
}

// ==================== 批量导入导出 ====================

/** 批量导入结果 */
export interface BatchImportResultVo {
  totalRows: number;
  successCount: number;
  failCount: number;
  allSuccess: boolean;
  errors?: { rowNum: number; message: string }[];
}

/** 批量导入员工 */
export function batchImportEmployees(file: File): Promise<BatchImportResultVo> {
  const formData = new FormData();
  formData.append('file', file);
  return request<BatchImportResultVo>({
    url: '/hr/employees/batch/import',
    method: 'POST',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/** 导出员工花名册（返回blob URL供下载） */
export async function exportEmployees(status?: string): Promise<void> {
  const http = (await import('./http')).default;
  const resp = await http.get('/hr/employees/batch/export', {
    params: { status },
    responseType: 'blob',
  });
  // 创建下载链接
  const blob = new Blob([resp.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = '员工花名册.xlsx';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
