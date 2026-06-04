const { request, toastApiOffline } = require('../../utils/api');

const app = getApp();

const ROLE_LABELS = {
  CUSTOMER: '普通客户',
  SALES: '销售',
  ENGINEER: '技术工程师',
  ADMIN: '管理员'
};

Page({
  data: {
    loginId: '',
    user: null,
    roleLabel: '未登录',
    statusText: '请先完成微信登录',
    confirming: false
  },
  onLoad(options) {
    this.setData({ loginId: options.loginId || '' });
    this.hydrateUser();
  },
  onShow() {
    this.hydrateUser();
  },
  hydrateUser() {
    const user = app.globalData.currentUser;
    this.setData({
      user,
      roleLabel: user ? ROLE_LABELS[user.role] || user.role : '未登录',
      statusText: user ? '确认后将在电脑端登录管理后台' : '请先完成微信登录'
    });
  },
  login() {
    wx.login({
      success: async (res) => {
        await this.loginWithCode(res.code || 'dev-code');
      },
      fail: async () => {
        await this.loginWithCode('dev-code');
      }
    });
  },
  async loginWithCode(code) {
    try {
      const result = await request('/auth/wechat-login', {
        method: 'POST',
        data: { code }
      });
      app.globalData.token = result.token;
      app.globalData.currentUser = result;
      this.hydrateUser();
      wx.showToast({ title: '登录成功' });
    } catch (error) {
      toastApiOffline();
    }
  },
  async confirm() {
    if (!this.data.loginId) {
      wx.showToast({ title: '登录二维码无效', icon: 'none' });
      return;
    }
    if (!app.globalData.token) {
      wx.showToast({ title: '请先微信登录', icon: 'none' });
      return;
    }
    this.setData({ confirming: true });
    try {
      const result = await request(`/admin/auth/qr/${this.data.loginId}/confirm`, { method: 'POST' });
      this.setData({ statusText: `${result.displayName} 已确认后台登录` });
      wx.showModal({
        title: '确认成功',
        content: '电脑端管理后台将自动进入系统。',
        showCancel: false
      });
    } catch (error) {
      wx.showModal({
        title: '无法登录后台',
        content: error && error.message ? error.message : '当前微信用户没有后台登录权限。',
        showCancel: false
      });
    } finally {
      this.setData({ confirming: false });
    }
  }
});
