import { createRouter, createWebHistory } from 'vue-router';
import LoginView from './views/LoginView.vue';
import DashboardView from './views/DashboardView.vue';
import ProductsView from './views/ProductsView.vue';
import ContentView from './views/ContentView.vue';
import SalesView from './views/SalesView.vue';
import AiView from './views/AiView.vue';
import SettingsView from './views/SettingsView.vue';

function adminRole() {
  try {
    return JSON.parse(localStorage.getItem('adminUser') || '{}').role || '';
  } catch {
    return '';
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { title: '扫码登录', public: true } },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', component: DashboardView, meta: { title: '工作台' } },
    { path: '/products', component: ProductsView, meta: { title: '产品管理' } },
    { path: '/content', component: ContentView, meta: { title: '方案案例' } },
    { path: '/sales', component: SalesView, meta: { title: '询价报价' } },
    { path: '/ai', component: AiView, meta: { title: 'AI 知识库' } },
    { path: '/settings', component: SettingsView, meta: { title: '系统设置', roles: ['ADMIN'] } }
  ]
});

router.beforeEach((to) => {
  if (to.meta.public) return true;
  const token = localStorage.getItem('adminToken');
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  const allowedRoles = to.meta.roles as string[] | undefined;
  if (allowedRoles && !allowedRoles.includes(adminRole())) {
    return '/dashboard';
  }
  return true;
});
