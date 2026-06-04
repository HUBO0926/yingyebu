const { request, toastApiOffline } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    copy: {
      eyebrow: '\u5de5\u7a0b\u76d1\u6d4b\u9500\u552e\u5e73\u53f0',
      title: '\u4ea7\u54c1\u5c55\u793a\u4e0e\u667a\u80fd\u62a5\u4ef7\u7cfb\u7edf',
      subtitle: '\u805a\u5408\u4ea7\u54c1\u3001\u65b9\u6848\u3001\u6848\u4f8b\u3001\u8be2\u4ef7\u4e0e AI \u77e5\u8bc6\u5e93\u5360\u4f4d\u80fd\u529b\u3002',
      inquiry: '\u8bbe\u5907\u8be2\u4ef7',
      ai: 'AI \u52a9\u624b',
      solutionCenter: '\u89e3\u51b3\u65b9\u6848\u4e2d\u5fc3',
      productCenter: '\u4ea7\u54c1\u4e2d\u5fc3',
      viewAll: '\u67e5\u770b\u5168\u90e8',
      cases: '\u5178\u578b\u6848\u4f8b',
      viewCases: '\u67e5\u770b\u6848\u4f8b',
      offline: '\u5df2\u4f7f\u7528\u672c\u5730\u6f14\u793a\u6570\u636e'
    },
    home: {
      industries: [],
      products: [],
      solutions: [],
      cases: []
    },
    usingMock: false
  },
  onLoad() {
    this.loadHome();
  },
  async loadHome() {
    try {
      const home = await request('/catalog/home');
      this.setData({ home, usingMock: false });
    } catch (error) {
      const mock = app.globalData.mockData;
      this.setData({
        usingMock: true,
        home: {
          industries: mock.industries,
          products: mock.products,
          solutions: mock.solutions,
          cases: mock.cases
        }
      });
      toastApiOffline();
    }
  },
  goProducts() {
    wx.switchTab({ url: '/pages/products/products' });
  },
  goCases() {
    wx.navigateTo({ url: '/pages/cases/cases' });
  },
  goInquiry() {
    wx.navigateTo({ url: '/pages/inquiry/inquiry' });
  },
  goAi() {
    wx.navigateTo({ url: '/pages/ai/ai' });
  }
});
