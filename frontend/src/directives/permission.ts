import type { Directive } from 'vue';
import { usePermissionStore } from '@/store/permission';

/**
 * v-permission="'employee:edit'"
 * v-permission="['employee:edit', 'employee:view']"
 * Removes the element from DOM if user lacks ALL listed permissions.
 * When an array is given, all codes must be present (AND logic).
 *
 * 挂载时和每次 updated 时均重新检查权限，确保权限变更后 UI 同步更新。
 */
function checkPermission(el: HTMLElement, binding: { value: string | string[] }): void {
  const store = usePermissionStore();
  const required = Array.isArray(binding.value) ? binding.value : [binding.value];
  const allowed = required.every((code) => store.has(code));
  if (!allowed) {
    el.remove();
  }
}

const vPermission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    checkPermission(el, binding);
  },
  updated(el, binding) {
    checkPermission(el, binding);
  },
};

export default vPermission;
