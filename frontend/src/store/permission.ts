import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getMyPermissions } from '@/api/role';

export const usePermissionStore = defineStore('permission', () => {
  const codes = ref<Set<string>>(new Set());
  const loaded = ref(false);

  async function fetchPermissions(): Promise<void> {
    try {
      const list = await getMyPermissions();
      codes.value = new Set(list);
    } finally {
      loaded.value = true;
    }
  }

  /**
   * 通配符权限匹配，与后端 PermissionService 语义一致：
   *   1. 精确匹配 → 'employee:edit'
   *   2. 全局通配符 → '*'
   *   3. 模块级通配符 → 'employee:*' 匹配 'employee:edit' 等
   */
  function has(code: string): boolean {
    const set = codes.value;
    // 精确匹配
    if (set.has(code)) return true;
    // 全局通配符
    if (set.has('*')) return true;
    // 模块级通配符：取冒号前的模块前缀
    const colonIdx = code.indexOf(':');
    if (colonIdx > 0) {
      const moduleWildcard = code.slice(0, colonIdx) + ':*';
      if (set.has(moduleWildcard)) return true;
    }
    return false;
  }

  function clear(): void {
    codes.value.clear();
    loaded.value = false;
  }

  return { codes, loaded, fetchPermissions, has, clear };
});
