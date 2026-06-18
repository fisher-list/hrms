import { request } from './http';
import type { LoginReq, TokenVo, UserVo, ChangePasswordReq } from '@/types/api';

// Auth API: matches backend contract documented in EP01-S01.
export function loginApi(payload: LoginReq): Promise<TokenVo> {
  return request<TokenVo>({
    url: '/auth/login',
    method: 'POST',
    data: payload,
  });
}

export function meApi(): Promise<UserVo> {
  return request<UserVo>({
    url: '/me',
    method: 'GET',
  });
}

export function changePasswordApi(payload: ChangePasswordReq): Promise<void> {
  return request<void>({
    url: '/auth/change-password',
    method: 'POST',
    data: payload,
  });
}
