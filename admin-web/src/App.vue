<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  Box,
  ChatDotRound,
  DataAnalysis,
  Document,
  HomeFilled,
  Setting,
  Tickets
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const title = computed(() => route.meta.title || '工作台');
const isLoginPage = computed(() => route.path === '/login');
const adminUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('adminUser') || '{}');
  } catch {
    return {};
  }
});

const menus = computed(() => {
  const role = adminUser.value.role;
  return [
    { path: '/dashboard', label: '工作台', icon: HomeFilled },
    { path: '/products', label: '产品管理', icon: Box },
    { path: '/content', label: '方案案例', icon: Document },
    { path: '/sales', label: '询价报价', icon: Tickets },
    { path: '/ai', label: 'AI 知识库', icon: ChatDotRound },
    { path: '/settings', label: '系统设置', icon: Setting, roles: ['ADMIN'] }
  ].filter((item) => !item.roles || item.roles.includes(role));
});

function logout() {
  localStorage.removeItem('adminToken');
  localStorage.removeItem('adminUser');
  router.replace('/login');
}
</script>

<template>
  <router-view v-if="isLoginPage" />
  <el-container v-else class="shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <div class="brand-mark"><el-icon><DataAnalysis /></el-icon></div>
        <div>
          <strong>智能报价系统</strong>
          <span>Product Show Admin</span>
        </div>
      </div>
      <el-menu router :default-active="route.path" class="menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div>
          <h1>{{ title }}</h1>
          <p>工程监测产品展示、询价分配、智能报价与资料知识库运营后台</p>
        </div>
        <div class="topbar-actions">
          <span class="admin-name">{{ adminUser.displayName || '未登录' }} · {{ adminUser.role || '-' }}</span>
          <el-button type="primary" @click="router.push('/sales')">新建报价</el-button>
          <el-button @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
