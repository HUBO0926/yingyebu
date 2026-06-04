<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import QRCode from 'qrcode';
import { getData, postData } from '../api';

interface QrSession {
  loginId: string;
  qrText: string;
  status: string;
  expiresAt: string;
}

interface AdminLoginResult {
  status: string;
  token?: string;
  role?: string;
  displayName?: string;
  openId?: string;
  permissions?: string[];
}

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const qrImage = ref('');
const qrText = ref('');
const statusText = ref('正在创建登录二维码');
const loginId = ref('');
let timer: number | undefined;

async function createQr() {
  loading.value = true;
  try {
    const session = await postData<QrSession>('/admin/auth/qr', {});
    loginId.value = session.loginId;
    qrText.value = session.qrText;
    qrImage.value = await QRCode.toDataURL(session.qrText, {
      width: 240,
      margin: 1,
      color: {
        dark: '#17233d',
        light: '#ffffff'
      }
    });
    statusText.value = '请使用本系统小程序扫码确认登录';
    startPolling();
  } catch (error) {
    statusText.value = '二维码创建失败，请确认后端 API 已启动';
    ElMessage.error(error instanceof Error ? error.message : '二维码创建失败');
  } finally {
    loading.value = false;
  }
}

function startPolling() {
  if (timer) window.clearInterval(timer);
  timer = window.setInterval(pollQr, 1800);
}

async function pollQr() {
  if (!loginId.value) return;
  try {
    const result = await getData<AdminLoginResult>(`/admin/auth/qr/${loginId.value}`);
    if (result.status === 'PENDING') {
      statusText.value = '等待小程序确认';
      return;
    }
    if (result.status === 'EXPIRED') {
      statusText.value = '二维码已过期，请刷新二维码';
      if (timer) window.clearInterval(timer);
      return;
    }
    if (result.status === 'DENIED') {
      statusText.value = '当前微信用户没有后台登录权限';
      if (timer) window.clearInterval(timer);
      return;
    }
    if (result.status === 'CONFIRMED' && result.token) {
      localStorage.setItem('adminToken', result.token);
      localStorage.setItem('adminUser', JSON.stringify({
        displayName: result.displayName,
        role: result.role,
        openId: result.openId,
        permissions: result.permissions || []
      }));
      ElMessage.success('登录成功');
      if (timer) window.clearInterval(timer);
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard';
      router.replace(redirect);
    }
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : '登录状态检查失败';
  }
}

function devCopyPath() {
  navigator.clipboard.writeText(qrText.value);
  ElMessage.success('小程序确认路径已复制');
}

onMounted(createQr);
onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer);
});
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <div class="brand-mark">恒</div>
        <div>
          <h1>产品展示与智能报价系统</h1>
          <p>管理后台微信扫码登录</p>
        </div>
      </div>

      <div class="qr-box" v-loading="loading">
        <img v-if="qrImage" :src="qrImage" alt="后台登录二维码" />
        <el-empty v-else description="等待二维码" />
      </div>

      <div class="login-status">{{ statusText }}</div>
      <p class="login-tip">只有后台授权为销售、技术工程师或管理员的微信用户可以登录。首次登录用户默认为普通客户，需要管理员在人员权限管理中调整。</p>

      <div class="login-actions">
        <el-button @click="createQr">刷新二维码</el-button>
        <el-button :disabled="!qrText" @click="devCopyPath">复制小程序确认路径</el-button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 32px;
  background: #eef3f8;
}

.login-panel {
  width: min(460px, 100%);
  padding: 32px;
  border: 1px solid #d9e1ee;
  border-radius: 8px;
  background: #ffffff;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 26px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  color: #ffffff;
  font-weight: 700;
  background: #1677ff;
}

.login-brand h1 {
  margin: 0;
  font-size: 20px;
}

.login-brand p,
.login-tip {
  margin: 6px 0 0;
  color: #6b778c;
  line-height: 1.6;
}

.qr-box {
  display: grid;
  place-items: center;
  min-height: 270px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  background: #f8fbff;
}

.qr-box img {
  width: 240px;
  height: 240px;
}

.login-status {
  margin-top: 18px;
  color: #1677ff;
  font-weight: 700;
  text-align: center;
}

.login-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}
</style>
