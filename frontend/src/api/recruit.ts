import { request } from './http';
import type { PageVo } from '@/types/portal';
import type {
  CandidateVo,
  CandidateCreateReq,
  OfferVo,
  OfferCreateReq,
  RequisitionVo,
  RequisitionCreateReq,
} from '@/types/talent';

// ==================== 候选人 ====================
export function listCandidates(current = 1, size = 10): Promise<PageVo<CandidateVo>> {
  return request<PageVo<CandidateVo>>({
    url: '/recruit/candidates',
    method: 'GET',
    params: { current, size },
  });
}

export function createCandidate(req: CandidateCreateReq): Promise<CandidateVo> {
  return request<CandidateVo>({ url: '/recruit/candidates', method: 'POST', data: req });
}

// ==================== Offer ====================
export function createOffer(req: OfferCreateReq): Promise<OfferVo> {
  return request<OfferVo>({ url: '/recruit/offers', method: 'POST', data: req });
}

export function submitOffer(id: number): Promise<void> {
  return request<void>({ url: `/recruit/offers/${id}/submit`, method: 'POST' });
}

export function acceptOffer(id: number): Promise<void> {
  return request<void>({ url: `/recruit/offers/${id}/accept`, method: 'POST' });
}

export function declineOffer(id: number): Promise<void> {
  return request<void>({ url: `/recruit/offers/${id}/decline`, method: 'POST' });
}

// ==================== 职位需求 ====================
export function listRequisitions(current = 1, size = 10): Promise<PageVo<RequisitionVo>> {
  return request<PageVo<RequisitionVo>>({
    url: '/recruit/requisitions',
    method: 'GET',
    params: { current, size },
  });
}

export function getRequisition(id: number): Promise<RequisitionVo> {
  return request<RequisitionVo>({ url: `/recruit/requisitions/${id}`, method: 'GET' });
}

export function createRequisition(req: RequisitionCreateReq): Promise<RequisitionVo> {
  return request<RequisitionVo>({ url: '/recruit/requisitions', method: 'POST', data: req });
}
