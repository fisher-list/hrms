import { createApp } from 'vue';
import { createPinia } from 'pinia';
import './styles.css';

import App from './App.vue';
import router from './router';
import vPermission from './directives/permission';

const app = createApp(App);
// Pinia must be installed before router so the guard can access stores.
app.use(createPinia());
app.use(router);
// Element Plus 已通过 unplugin-vue-components 和 unplugin-auto-import 按需引入
app.directive('permission', vPermission);
app.mount('#app');
