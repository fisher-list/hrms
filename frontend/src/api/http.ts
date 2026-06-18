import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { ElMessage } from 'element-plus';
import type { ApiResult } from '@/types/api';

// Extend axios config with a private retry flag to prevent infinite refresh loops.
type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true, // 允许跨域携带 cookie（HttpOnly refreshToken）
});

// Lazy router import avoids circular dep with router/index.ts.
async function redirectToLogin(): Promise<void> {
  const { default: router } = await import('@/router');
  const current = router.currentRoute.value;
  if (current.path !== '/login') {
    await router.replace({
      path: '/login',
      query: { redirect: current.fullPath },
    });
  }
}

// Single-flight refresh: many parallel 401s share one refresh call.
let refreshing: Promise<string | null> | null = null;

async function doRefresh(): Promise<string | null> {
  try {
    // refreshToken 通过 HttpOnly cookie 自动携带，无需在 body 中发送
    const resp = await axios.post<{ code: number; data?: { accessToken: string } }>(
      '/api/auth/refresh',
      {},
      { timeout: 15000, withCredentials: true },
    );
    if (resp.data.code === 0 && resp.data.data) {
      const { useAuthStore } = await import('@/store/auth');
      const authStore = useAuthStore();
      authStore.setAccessToken(resp.data.data.accessToken);
      return resp.data.data.accessToken;
    }
    return null;
  } catch {
    return null;
  }
}

http.interceptors.request.use(async (config) => {
  // 从 Pinia store 读取 accessToken（内存存储，防止 XSS 窃取）
  // Pinia 已在 app 创建时安装，此处 store 已可用
  try {
    const { useAuthStore } = await import('@/store/auth');
    const token = useAuthStore().accessToken;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  } catch {
    // ignore — 首次请求时 store 可能尚未就绪
  }
  return config;
});

http.interceptors.response.use(
  (resp: AxiosResponse<ApiResult<unknown>>) => {
    const body = resp.data;
    // R.code === 0 means business success.
    if (body && typeof body === 'object' && body.code === 0) {
      return resp;
    }
    // Business error: surface message and reject so callers' catch can handle.
    const msg = body?.msg ?? '请求失败';
    ElMessage.error(msg);
    return Promise.reject(
      Object.assign(new Error(msg), { code: body?.code, data: body?.data }),
    );
  },
  async (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status;
    const original = error.config as RetriableConfig | undefined;

    // 401: try one refresh, then replay; on second 401 redirect to login.
    if (status === 401 && original && !original._retried) {
      const url = original.url ?? '';
      // Never refresh-on-refresh; never refresh-on-login itself.
      if (url.includes('/auth/refresh') || url.includes('/auth/login')) {
        await redirectToLogin();
        return Promise.reject(error);
      }
      original._retried = true;
      refreshing = refreshing ?? doRefresh();
      const newToken = await refreshing;
      refreshing = null;
      if (newToken) {
        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${newToken}`;
        return http.request(original);
      }
      // Refresh failed → clear and bounce to login.
      const { useAuthStore } = await import('@/store/auth');
      await useAuthStore().logout({ remote: false });
      ElMessage.error('登录已过期,请重新登录');
      await redirectToLogin();
      return Promise.reject(error);
    }

    // 403: redirect to forbidden page.
    if (status === 403) {
      const { default: router } = await import('@/router');
      const current = router.currentRoute.value;
      if (current.path !== '/403') {
        await router.push('/403');
      }
      return Promise.reject(error);
    }

    const msg = error.response?.data?.msg ?? error.message ?? '网络错误';
    ElMessage.error(msg);
    return Promise.reject(error);
  },
);

// Typed helper: unwraps R<T> and returns data only.
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const resp = await http.request<ApiResult<T>>(config);
  return resp.data.data;
}

export default http;
