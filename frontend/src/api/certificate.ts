/** 证明开具 API */

import { request } from './http';

/** 证明申请 */
export interface CertificateVo {
  id: number;
  employeeId: number;
  employeeName?: string;
  empNo?: string;
  type: string;
  purpose: string;
  copies: number;
  status: string;
  approvalInstanceId?: number;
  issuedAt?: string;
  incomeStartDate?: string;
  incomeEndDate?: string;
  rejectReason?: string;
}

/** 创建证明申请 */
export interface CertificateCreateReq {
  type: string;
  purpose: string;
  copies: number;
  incomeStartDate?: string;
  incomeEndDate?: string;
}

/** 员工自助申请证明 */
export function applyCertificate(data: CertificateCreateReq): Promise<CertificateVo> {
  return request<CertificateVo>({ url: '/certificates/apply', method: 'POST', data });
}

/** 查询我的证明申请 */
export function myCertificates(status?: string): Promise<CertificateVo[]> {
  return request<CertificateVo[]>({ url: '/certificates/my', method: 'GET', params: { status } });
}

/** 查询证明列表（管理员） */
export function listCertificates(params?: {
  employeeId?: number;
  status?: string;
}): Promise<CertificateVo[]> {
  return request<CertificateVo[]>({ url: '/certificates', method: 'GET', params });
}

/** 获取证明详情 */
export function getCertificate(id: number): Promise<CertificateVo> {
  return request<CertificateVo>({ url: `/certificates/${id}`, method: 'GET' });
}

/** 签发证明 */
export function issueCertificate(id: number): Promise<void> {
  return request<void>({ url: `/certificates/${id}/issue`, method: 'POST' });
}
