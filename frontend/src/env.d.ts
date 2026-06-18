/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

export {};

declare module 'vue-router' {
  interface RouteMeta {
    /** Whether the route requires authentication. Defaults to true. */
    requiresAuth?: boolean;
    /** Permission code required to access this route. */
    permission?: string;
  }
}
