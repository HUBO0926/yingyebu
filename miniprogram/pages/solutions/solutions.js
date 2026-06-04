const { request, toastApiOffline } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    copy: {
      intro: '\u9762\u5411\u5de5\u7a0b\u76d1\u6d4b\u573a\u666f\u7684\u884c\u4e1a\u5316\u914d\u7f6e\u65b9\u6848\u3002',
      recommended: '\u63a8\u8350\u914d\u7f6e'
    },
    solutions: []
  },
  onLoad() {
    this.loadSolutions();
  },
  async loadSolutions() {
    try {
      const solutions = await request('/solutions');
      this.setData({ solutions });
    } catch (error) {
      this.setData({ solutions: app.globalData.mockData.solutions });
      toastApiOffline();
    }
  }
});
