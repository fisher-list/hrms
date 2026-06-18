import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import axios from 'axios';
import { loginApi, meApi } from '@/api/auth';
import type { LoginReq, UserVo } from '@/types/api';

const USER_KEY = 'hrms_user';

function readUser(): UserVo | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserVo;
  } catch {
    return null;
  }
}

export const useAuthStore = defineStore('auth', () => {
  // accessToken 仅存于 Pinia 内存，不持久化到 localStorage，防止 XSS 窃取
  // refreshToken 已改为 HttpOnly cookie，前端不再持有
  const accessToken = ref<string>('');
  const user = ref<UserVo | null>(readUser());
  const forceChangePassword = ref<boolean>(false);

  const isAuthenticated = computed<boolean>(() => accessToken.value.length > 0);

  function persist(): void {
    if (user.value) {
      localStorage.setItem(USER_KEY, JSON.stringify(user.value));
    } else {
      localStorage.removeItem(USER_KEY);
    }
  }

  async function login(payload: LoginReq): Promise<void> {
    const token = await loginApi(payload);
    accessToken.value = token.accessToken;
    user.value = { id: token.uid, username: token.username, nickname: token.username, forceChangePassword: token.forceChangePassword };
    forceChangePassword.value = token.forceChangePassword === true;
    persist();
  }

  async function logout(options: { remote?: boolean } = {}): Promise<void> {
    if (options.remote !== false) {
      try {
        await axios.post('/api/auth/logout', {}, { timeout: 15000, withCredentials: true });
      } catch {
        // Local logout must still complete when the session is already invalid.
      }
    }
    accessToken.value = '';
    user.value = null;
    forceChangePassword.value = false;
    persist();
    // Clear permission cache on logout.
    import('@/store/permission').then(({ usePermissionStore }) => {
      usePermissionStore().clear();
    });
  }

  /** 仅更新 accessToken（用于 axios 拦截器刷新后同步） */
  function setAccessToken(access: string): void {
    accessToken.value = access;
  }

  /** Set forceChangePassword flag (called from /api/me response or login) */
  function setForceChangePassword(val: boolean): void {
    forceChangePassword.value = val;
  }

  /** 刷新页面时尝试通过 refreshToken cookie 恢复 accessToken */
  async function tryRestore(): Promise<boolean> {
    try {
      const { default: http } = await import('@/api/http');
      const resp = await http.post('/auth/refresh');
      const data = resp.data?.data;
      if (data?.accessToken) {
        accessToken.value = data.accessToken;
        forceChangePassword.value = data.forceChangePassword === true;
        return true;
      }
      return false;
    } catch {
      return false;
    }
  }

  /**
   * Fetch the current user info from /api/me and sync the forceChangePassword flag.
   * Used by the router guard after a page refresh to restore this state.
   */
  async function fetchMe(): Promise<void> {
    try {
      const me = await meApi();
      user.value = me;
      forceChangePassword.value = me.forceChangePassword === true;
      persist();
    } catch {
      // ignore — caller will handle auth failure
    }
  }

  return {
    accessToken,
    user,
    forceChangePassword,
    isAuthenticated,
    login,
    logout,
    setAccessToken,
    setForceChangePassword,
    tryRestore,
    fetchMe,
  };
});
