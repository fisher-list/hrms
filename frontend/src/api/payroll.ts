import { request } from './http';
import type { PageVo } from '@/types/portal';
import type {
  PayrollPeriodVo,
  PayrollRunVo,
  PayrollDetailVo,
  CompensationVo,
  CompensationCreateReq,
} from '@/types/org-payroll';

// ==================== 薪酬周期 ====================
export function listPeriods(): Promise<PayrollPeriodVo[]> {
  return request<PayrollPeriodVo[]>({ url: '/payroll/periods', method: 'GET' });
}

export function createPeriod(periodMonth: string): Promise<PayrollPeriodVo> {
  return request<PayrollPeriodVo>({
    url: '/payroll/periods',
    method: 'POST',
    data: { periodMonth },
  });
}

// ==================== 薪酬批次 ====================
export function listRuns(periodId?: number): Promise<PayrollRunVo[]> {
  return request<PayrollRunVo[]>({
    url: '/payroll/runs',
    method: 'GET',
    params: periodId != null ? { periodId } : undefined,
  });
}

export function createRun(periodId: number): Promise<PayrollRunVo> {
  return request<PayrollRunVo>({
    url: '/payroll/runs',
    method: 'POST',
    data: { periodId },
  });
}

export function calculateRun(runId: number): Promise<number[]> {
  return request<number[]>({
    url: `/payroll/runs/${runId}/calculate`,
    method: 'POST',
  });
}

export function lockRun(runId: number): Promise<PayrollRunVo> {
  return request<PayrollRunVo>({
    url: `/payroll/runs/${runId}/lock`,
    method: 'POST',
  });
}

export function reverseRun(runId: number): Promise<PayrollRunVo> {
  return request<PayrollRunVo>({
    url: `/payroll/runs/${runId}/reverse`,
    method: 'POST',
  });
}

export function getRunDetails(runId: number): Promise<PayrollDetailVo[]> {
  return request<PayrollDetailVo[]>({
    url: `/payroll/runs/${runId}`,
    method: 'GET',
  });
}

// ==================== 薪资档案 ====================
export function listCompensations(current = 1, size = 10): Promise<PageVo<CompensationVo>> {
  return request<PageVo<CompensationVo>>({
    url: '/payroll/compensations',
    method: 'GET',
    params: { current, size },
  });
}

export function createCompensation(req: CompensationCreateReq): Promise<CompensationVo> {
  return request<CompensationVo>({
    url: '/payroll/compensations',
    method: 'POST',
    data: req,
  });
}

export function getCompensation(employeeId: number): Promise<CompensationVo> {
  return request<CompensationVo>({
    url: `/payroll/compensations/${employeeId}`,
    method: 'GET',
  });
}

// ==================== 工资条 ====================
export function listMyPayslips(): Promise<PayrollDetailVo[]> {
  return request<PayrollDetailVo[]>({ url: '/payroll/payslips', method: 'GET' });
}

export function getMyPayslip(runId: number): Promise<PayrollDetailVo> {
  return request<PayrollDetailVo>({ url: `/payroll/payslips/${runId}`, method: 'GET' });
}
