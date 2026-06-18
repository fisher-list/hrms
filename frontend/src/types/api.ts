// Shared API contract types aligned with backend R<T>.

export interface ApiResult<T> {
  code: number;
  msg: string;
  data: T;
}

export interface UserVo {
  id: number | string;
  username: string;
  nickname: string;
  forceChangePassword?: boolean;
}

export interface LoginReq {
  username: string;
  password: string;
}

export interface ChangePasswordReq {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface TokenVo {
  accessToken: string;
  refreshToken?: string; // 后端通过 HttpOnly cookie 返回，body 中为 null
  expiresIn: number;
  tokenType: string;
  uid: number;
  username: string;
  forceChangePassword?: boolean;
}

export interface RefreshVo {
  accessToken: string;
  refreshToken?: string; // 后端通过 HttpOnly cookie 返回，body 中为 null
  forceChangePassword?: boolean;
}

// --- S02: Role & Permission types ---

export interface RoleVo {
  id: number;
  code: string;
  name: string;
  description: string;
  builtin: boolean;
  enabled: boolean;
}

export interface RoleReq {
  code: string;
  name: string;
  description?: string;
  enabled?: boolean;
}

export interface PermissionVo {
  id: number;
  code: string;
  name: string;
  children?: PermissionVo[];
}

export interface SimpleUserVo {
  id: number;
  username: string;
  nickname: string;
}
