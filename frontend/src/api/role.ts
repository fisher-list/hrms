import { request } from './http';
import type { RoleVo, RoleReq, PermissionVo, SimpleUserVo } from '@/types/api';

type MyPermissionsResponse = string[] | { permissions?: string[] };

export function getRoles(): Promise<RoleVo[]> {
  return request<RoleVo[]>({ url: '/roles', method: 'GET' });
}

export function createRole(data: RoleReq): Promise<RoleVo> {
  return request<RoleVo>({ url: '/roles', method: 'POST', data });
}

export function updateRole(id: number, data: RoleReq): Promise<RoleVo> {
  return request<RoleVo>({ url: `/roles/${id}`, method: 'PUT', data });
}

export function deleteRole(id: number): Promise<void> {
  return request<void>({ url: `/roles/${id}`, method: 'DELETE' });
}

export function getRolePermissions(roleId: number): Promise<PermissionVo[]> {
  return request<PermissionVo[]>({ url: `/roles/${roleId}/permissions`, method: 'GET' });
}

export function updateRolePermissions(roleId: number, permissionIds: number[]): Promise<void> {
  return request<void>({
    url: `/roles/${roleId}/permissions`,
    method: 'PUT',
    data: { permissionIds },
  });
}

export function getRoleUsers(roleId: number): Promise<SimpleUserVo[]> {
  return request<SimpleUserVo[]>({ url: `/roles/${roleId}/users`, method: 'GET' });
}

export function assignRoleUsers(roleId: number, userIds: number[]): Promise<void> {
  return request<void>({
    url: `/roles/${roleId}/users`,
    method: 'POST',
    data: { userIds },
  });
}

export function getAllPermissions(): Promise<PermissionVo[]> {
  return request<PermissionVo[]>({ url: '/permissions', method: 'GET' });
}

export async function getMyPermissions(): Promise<string[]> {
  const data = await request<MyPermissionsResponse>({ url: '/me/permissions', method: 'GET' });
  if (Array.isArray(data)) {
    return data;
  }
  return Array.isArray(data.permissions) ? data.permissions : [];
}

export function getAllUsers(): Promise<SimpleUserVo[]> {
  return request<SimpleUserVo[]>({ url: '/users', method: 'GET' });
}
