const { request, toastApiOffline } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    copy: {
      intro: '\u5c55\u793a\u5df2\u843d\u5730\u7684\u76d1\u6d4b\u5de5\u7a0b\u9879\u76ee\u548c\u5178\u578b\u5e94\u7528\u573a\u666f\u3002'
    },
    cases: []
  },
  onLoad() {
    this.loadCases();
  },
  async loadCases() {
    try {
      const cases = await request('/cases');
      this.setData({ cases });
    } catch (error) {
      this.setData({ cases: app.globalData.mockData.cases });
      toastApiOffline();
    }
  }
});
