const { request, toastApiOffline } = require('../../utils/api');

const app = getApp();

const SALES_USERS = [
  { id: 'sales-1', name: '\u738b\u9500\u552e' },
  { id: 'sales-2', name: '\u674e\u9500\u552e' },
  { id: 'sales-3', name: '\u8d75\u9500\u552e' }
];

const ROLE_LABELS = {
  CUSTOMER: '\u666e\u901a\u5ba2\u6237',
  SALES: '\u9500\u552e',
  ENGINEER: '\u6280\u672f\u5de5\u7a0b\u5e08',
  ADMIN: '\u7ba1\u7406\u5458'
};

function canQuote(role) {
  return ['SALES', 'ENGINEER', 'ADMIN'].includes(role);
}

function canViewAllQuotes(role) {
  return ['ENGINEER', 'ADMIN'].includes(role);
}

function emptyDashboard() {
  return {
    favoriteCount: 0,
    quoteMessageCount: 0,
    aiQuestionCount: 0,
    quoteCount: 0,
    inquiryCount: 0
  };
}

function mockUser() {
  return {
    token: 'mock-customer-token',
    openId: 'mock-openid-offline',
    unionId: '',
    role: 'CUSTOMER',
    displayName: '\u666e\u901a\u5ba2\u6237',
    permissions: permissionsForRole('CUSTOMER')
  };
}

function permissionsForRole(role) {
  if (role === 'ADMIN') return ['QUOTE_CREATE', 'QUOTE_VIEW_ALL', 'INQUIRY_ASSIGN', 'AI_CHAT', 'FAVORITE'];
  if (role === 'ENGINEER') return ['QUOTE_CREATE', 'QUOTE_VIEW_ALL', 'AI_CHAT', 'FAVORITE'];
  if (role === 'SALES') return ['QUOTE_CREATE', 'QUOTE_VIEW_OWN', 'AI_CHAT', 'FAVORITE'];
  return ['FAVORITE', 'AI_CHAT', 'QUOTE_MESSAGE'];
}

function mockFavorites() {
  return [
    { id: 1, name: '\u667a\u80fd\u4f4d\u79fb\u4f20\u611f\u5668', model: 'PS-DIS-300', summary: '\u8fb9\u5761\u3001\u575d\u4f53\u4f4d\u79fb\u8fde\u7eed\u76d1\u6d4b\u8d44\u6599\u3002' },
    { id: 2, name: '\u591a\u901a\u9053\u6570\u636e\u91c7\u96c6\u4eea', model: 'PS-DAQ-16', summary: '\u591a\u6d4b\u70b9\u63a5\u5165\u4e0e\u8fdc\u7a0b\u914d\u7f6e\u8d44\u6599\u3002' }
  ];
}

function mockQuoteMessages() {
  return [
    { id: 1, projectName: '\u5317\u5c71\u8fb9\u5761\u81ea\u52a8\u5316\u76d1\u6d4b', amount: '69008.00', status: '\u5df2\u53d1\u9001', sentAt: '2026-06-03 10:30' },
    { id: 2, projectName: '\u5c3e\u77ff\u5e93\u96e8\u91cf\u901a\u4fe1\u76d1\u6d4b', amount: '14880.00', status: '\u5f85\u67e5\u770b', sentAt: '2026-06-02 16:12' }
  ];
}

function mockQuotes(role) {
  const quotes = [
    { id: 2001, quoteNo: 'Q20260603001', projectName: '\u5317\u5c71\u8fb9\u5761\u81ea\u52a8\u5316\u76d1\u6d4b', customerName: '\u534e\u5317\u77ff\u4e1a\u96c6\u56e2', ownerId: 'sales-1', ownerName: '\u738b\u9500\u552e', totalAmount: '69008.00', status: '\u8349\u7a3f', createdAt: '2026-06-03 10:20' },
    { id: 2002, quoteNo: 'Q20260602003', projectName: '\u897f\u5357\u6c34\u5e93\u5927\u575d\u76d1\u6d4b', customerName: '\u897f\u5357\u6c34\u52a1', ownerId: 'sales-2', ownerName: '\u674e\u9500\u552e', totalAmount: '128600.00', status: '\u5df2\u751f\u6210', createdAt: '2026-06-02 15:50' },
    { id: 2003, quoteNo: 'Q20260601002', projectName: '\u5730\u707e\u4f4d\u79fb\u89c2\u6d4b\u7ad9', customerName: '\u897f\u5317\u5730\u52d8\u9662', ownerId: 'sales-1', ownerName: '\u738b\u9500\u552e', totalAmount: '43200.00', status: '\u5df2\u53d1\u9001', createdAt: '2026-06-01 09:08' }
  ];
  return role === 'SALES' ? quotes.filter((quote) => quote.ownerId === 'sales-1') : quotes;
}

function mockInquiries() {
  return [
    { id: 1001, contactName: '\u5f20\u5de5', company: '\u534e\u5317\u77ff\u4e1a\u96c6\u56e2', projectName: '\u5317\u5c71\u8fb9\u5761\u81ea\u52a8\u5316\u76d1\u6d4b', status: '\u5f85\u5206\u914d', assignedSalesId: '', assignedSalesName: '\u672a\u5206\u914d', selectedSalesIndex: 0 },
    { id: 1002, contactName: '\u9648\u5de5', company: '\u897f\u5357\u6c34\u52a1', projectName: '\u6c34\u5e93\u5927\u575d\u76d1\u6d4b', status: '\u5df2\u5206\u914d', assignedSalesId: 'sales-2', assignedSalesName: '\u674e\u9500\u552e', selectedSalesIndex: 1 }
  ];
}

function statusCode(status) {
  if (status === '\u5df2\u53d1\u9001') return 'SENT';
  if (status === '\u8349\u7a3f') return 'DRAFT';
  return 'GENERATED';
}

function quotePreviewFromSummary(quote) {
  const finalAmount = Number(quote.totalAmount || 0).toFixed(2);
  const amount = (Number(quote.totalAmount || 0) / 0.95).toFixed(2);
  return {
    id: quote.id,
    quoteNo: quote.quoteNo,
    status: statusCode(quote.status),
    statusText: quote.status,
    createdAt: quote.createdAt,
    wordFileName: `${quote.quoteNo}.docx`,
    customer: {
      name: '\u5ba2\u6237\u8054\u7cfb\u4eba',
      company: quote.customerName,
      phone: '13800000000'
    },
    project: {
      name: quote.projectName,
      scenario: quote.projectName.includes('\u6c34\u5e93') ? '\u6c34\u5e93\u5927\u575d\u76d1\u6d4b' : '\u8fb9\u5761\u76d1\u6d4b'
    },
    template: '\u6807\u51c6\u8bbe\u5907\u62a5\u4ef7\u4e66',
    discountRate: 95,
    remark: '\u5386\u53f2\u62a5\u4ef7\u8be6\u60c5\u4e3a\u672c\u5730\u6f14\u793a\u9884\u89c8\uff0c\u540e\u7eed\u53ef\u63a5\u5165\u540e\u7aef\u62a5\u4ef7\u660e\u7ec6\u3002',
    totals: {
      testItemCount: 1,
      configuredCategories: 3,
      productCount: 3,
      amount,
      discountAmount: finalAmount,
      finalAmount
    },
    testItems: [
      {
        id: `${quote.id}-site`,
        name: '\u6807\u51c6\u76d1\u6d4b\u7ad9',
        siteCount: 1,
        subtotal: finalAmount,
        categories: [
          {
            categoryId: 'sensor',
            categoryName: '\u4f20\u611f\u5668',
            skipped: false,
            selectedProducts: [
              {
                productId: 'sensor-dis-300',
                name: '\u667a\u80fd\u4f4d\u79fb\u4f20\u611f\u5668',
                model: 'PS-DIS-300',
                unit: '\u53f0',
                standardPrice: 6800,
                imageUrl: 'https://dummyimage.com/240x180/e8f2ff/1677ff&text=PS-DIS-300',
                quantity: 2,
                parameters: [
                  { label: '\u91cf\u7a0b', value: '300mm' },
                  { label: '\u7cbe\u5ea6', value: '0.1mm' }
                ],
                summary: '\u7528\u4e8e\u8fb9\u5761\u3001\u575d\u4f53\u548c\u77ff\u5c71\u7ed3\u6784\u4f4d\u79fb\u8fde\u7eed\u76d1\u6d4b\u3002',
                subtotal: '13600.00'
              }
            ]
          },
          {
            categoryId: 'collector',
            categoryName: '\u91c7\u96c6\u4eea',
            skipped: false,
            selectedProducts: [
              {
                productId: 'collector-daq-16',
                name: '\u591a\u901a\u9053\u6570\u636e\u91c7\u96c6\u4eea',
                model: 'PS-DAQ-16',
                unit: '\u53f0',
                standardPrice: 12800,
                imageUrl: 'https://dummyimage.com/240x180/f0f5ff/315efb&text=PS-DAQ-16',
                quantity: 1,
                parameters: [
                  { label: '\u901a\u9053', value: '16' },
                  { label: '\u901a\u4fe1', value: '4G / Ethernet' }
                ],
                summary: '\u652f\u6301\u591a\u6d4b\u70b9\u63a5\u5165\u3001\u8fb9\u7f18\u7f13\u5b58\u548c\u8fdc\u7a0b\u914d\u7f6e\u3002',
                subtotal: '12800.00'
              }
            ]
          },
          {
            categoryId: 'communication',
            categoryName: '\u901a\u4fe1',
            skipped: false,
            selectedProducts: [
              {
                productId: 'comm-gw-4g',
                name: '\u5de5\u4e1a 4G \u901a\u4fe1\u7f51\u5173',
                model: 'PS-GW-4G',
                unit: '\u53f0',
                standardPrice: 3600,
                imageUrl: 'https://dummyimage.com/240x180/e6fffb/08979c&text=PS-GW-4G',
                quantity: 1,
                parameters: [
                  { label: '\u7f51\u7edc', value: '4G Cat.4' },
                  { label: '\u63a5\u53e3', value: 'RS485 / LAN' }
                ],
                summary: '\u9762\u5411\u91ce\u5916\u9879\u76ee\u7684\u6570\u636e\u56de\u4f20\u548c\u8bbe\u5907\u8fdc\u7a0b\u7ef4\u62a4\u3002',
                subtotal: '3600.00'
              }
            ]
          }
        ]
      }
    ],
    mockLocal: true
  };
}

Page({
  data: {
    copy: {
      title: '\u6211\u7684',
      intro: '\u5fae\u4fe1\u767b\u5f55\u540e\uff0c\u7cfb\u7edf\u4f1a\u6839\u636e\u540e\u53f0\u6388\u6743\u89d2\u8272\u5f00\u653e\u5bf9\u5e94\u529f\u80fd\u3002',
      login: '\u5fae\u4fe1\u6388\u6743\u767b\u5f55',
      logout: '\u9000\u51fa\u767b\u5f55',
      logged: '\u5df2\u767b\u5f55',
      authPending: '\u672a\u83b7\u53d6\u540e\u53f0\u6388\u6743',
      authTip: '\u9996\u6b21\u5fae\u4fe1\u767b\u5f55\u540e\u9ed8\u8ba4\u4e3a\u666e\u901a\u5ba2\u6237\uff1b\u9500\u552e\u3001\u6280\u672f\u5de5\u7a0b\u5e08\u548c\u7ba1\u7406\u5458\u6743\u9650\u9700\u7531\u540e\u53f0\u4eba\u5458\u6743\u9650\u7ba1\u7406\u8c03\u6574\u3002',
      favorites: '\u6211\u7684\u6536\u85cf',
      quoteMessages: '\u62a5\u4ef7\u5355\u6d88\u606f',
      ai: 'AI \u52a9\u624b',
      quoteCenter: '\u62a5\u4ef7\u4e2d\u5fc3',
      quoteCenterHint: '\u67e5\u770b\u5386\u53f2\u62a5\u4ef7\uff0c\u5e76\u4f7f\u7528\u62a5\u4ef7\u5de5\u5177\u751f\u6210\u65b0\u62a5\u4ef7\u3002',
      quoteDetail: '\u67e5\u770b\u8be6\u60c5',
      createQuote: '\u4f7f\u7528\u62a5\u4ef7\u5de5\u5177',
      allQuoteHint: '\u5f53\u524d\u89d2\u8272\u53ef\u67e5\u770b\u5168\u90e8\u9500\u552e\u62a5\u4ef7\u8bb0\u5f55\u3002',
      ownQuoteHint: '\u9500\u552e\u53ea\u80fd\u67e5\u770b\u81ea\u5df1\u7684\u62a5\u4ef7\u8bb0\u5f55\u3002',
      inquiryAssign: '\u8be2\u4ef7\u5206\u914d',
      assignHint: '\u7ba1\u7406\u5458\u53ef\u67e5\u770b\u5168\u90e8\u5ba2\u6237\u8be2\u4ef7\uff0c\u5e76\u5206\u914d\u7ed9\u4efb\u610f\u9500\u552e\u3002',
      assign: '\u5206\u914d',
      enter: '\u8fdb\u5165',
      emptyLogin: '\u8bf7\u5148\u767b\u5f55\u67e5\u770b\u4e2a\u4eba\u529f\u80fd\u3002',
      emptyData: '\u6682\u65e0\u6570\u636e'
    },
    token: '',
    user: null,
    roleLabel: '\u672a\u767b\u5f55',
    isLogged: false,
    isCustomer: false,
    canQuote: false,
    canViewAllQuotes: false,
    isAdmin: false,
    dashboard: emptyDashboard(),
    favorites: [],
    quoteMessages: [],
    quotes: [],
    inquiries: [],
    salesUsers: SALES_USERS,
    salesNames: SALES_USERS.map((sales) => sales.name)
  },
  onShow() {
    wx.setNavigationBarTitle({ title: this.data.copy.title });
    this.hydrateFromGlobal();
    if (this.data.token) this.loadMyData();
  },
  hydrateFromGlobal() {
    const user = app.globalData.currentUser;
    this.applyUser(user, app.globalData.token || '');
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
      this.applyUser(result, result.token);
      wx.showToast({ title: '\u767b\u5f55\u6210\u529f' });
      await this.loadMyData();
    } catch (error) {
      const user = mockUser();
      app.globalData.token = user.token;
      app.globalData.currentUser = user;
      this.applyUser(user, user.token);
      this.loadLocalData(user.role);
      toastApiOffline();
    }
  },
  logout() {
    app.globalData.token = '';
    app.globalData.currentUser = null;
    this.applyUser(null, '');
    this.setData({
      dashboard: emptyDashboard(),
      favorites: [],
      quoteMessages: [],
      quotes: [],
      inquiries: []
    });
  },
  applyUser(user, token) {
    const role = user ? user.role : '';
    this.setData({
      token,
      user,
      roleLabel: user ? ROLE_LABELS[role] || user.displayName : '\u672a\u767b\u5f55',
      isLogged: Boolean(token && user),
      isCustomer: role === 'CUSTOMER',
      canQuote: canQuote(role),
      canViewAllQuotes: canViewAllQuotes(role),
      isAdmin: role === 'ADMIN'
    });
  },
  async loadMyData() {
    const role = this.data.user ? this.data.user.role : 'CUSTOMER';
    try {
      const [dashboard, favorites, quoteMessages, quotes] = await Promise.all([
        request('/me/dashboard'),
        request('/me/favorites'),
        request('/me/quote-messages'),
        request('/me/quotes')
      ]);
      const nextData = { dashboard, favorites, quoteMessages, quotes };
      if (role === 'ADMIN') {
        const [inquiries, salesUsers] = await Promise.all([
          request('/admin/inquiries'),
          request('/admin/sales-users')
        ]);
        nextData.inquiries = this.withSelectedSales(inquiries, salesUsers);
        nextData.salesUsers = salesUsers;
        nextData.salesNames = salesUsers.map((sales) => sales.name);
      }
      this.setData(nextData);
    } catch (error) {
      this.loadLocalData(role);
    }
  },
  loadLocalData(role) {
    const favorites = mockFavorites();
    const quoteMessages = mockQuoteMessages();
    const quotes = canQuote(role) ? mockQuotes(role) : [];
    const inquiries = role === 'ADMIN' ? mockInquiries() : [];
    this.setData({
      dashboard: {
        favoriteCount: favorites.length,
        quoteMessageCount: quoteMessages.length,
        aiQuestionCount: 4,
        quoteCount: quotes.length,
        inquiryCount: inquiries.length
      },
      favorites,
      quoteMessages,
      quotes,
      inquiries,
      salesUsers: SALES_USERS,
      salesNames: SALES_USERS.map((sales) => sales.name)
    });
  },
  withSelectedSales(inquiries, salesUsers) {
    return inquiries.map((inquiry) => {
      const selectedSalesIndex = Math.max(0, salesUsers.findIndex((sales) => sales.id === inquiry.assignedSalesId));
      return { ...inquiry, selectedSalesIndex };
    });
  },
  goQuoteCreate() {
    if (!this.data.canQuote) {
      wx.showToast({ title: '\u9700\u8981\u5185\u90e8\u89d2\u8272\u6388\u6743', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/quote-create/quote-create' });
  },
  goAi() {
    wx.navigateTo({ url: '/pages/ai/ai' });
  },
  openQuoteDetail(event) {
    const quoteIndex = Number(event.currentTarget.dataset.quoteIndex);
    const quote = this.data.quotes[quoteIndex];
    if (!quote) return;
    app.globalData.pendingQuotePreview = quotePreviewFromSummary(quote);
    wx.navigateTo({ url: '/pages/quote-preview/quote-preview' });
  },
  selectSales(event) {
    const inquiryIndex = Number(event.currentTarget.dataset.inquiryIndex);
    const selectedSalesIndex = Number(event.detail.value);
    this.setData({ [`inquiries[${inquiryIndex}].selectedSalesIndex`]: selectedSalesIndex });
  },
  async assignInquiry(event) {
    const inquiryIndex = Number(event.currentTarget.dataset.inquiryIndex);
    const inquiry = this.data.inquiries[inquiryIndex];
    const sales = this.data.salesUsers[inquiry.selectedSalesIndex || 0];
    if (!sales) {
      wx.showToast({ title: '\u8bf7\u9009\u62e9\u9500\u552e', icon: 'none' });
      return;
    }
    try {
      const updated = await request(`/admin/inquiries/${inquiry.id}/assign`, {
        method: 'POST',
        data: { salesId: sales.id }
      });
      this.setData({ [`inquiries[${inquiryIndex}]`]: { ...updated, selectedSalesIndex: inquiry.selectedSalesIndex || 0 } });
      wx.showToast({ title: '\u5df2\u5206\u914d' });
    } catch (error) {
      const updated = {
        ...inquiry,
        assignedSalesId: sales.id,
        assignedSalesName: sales.name,
        status: '\u5df2\u5206\u914d'
      };
      this.setData({ [`inquiries[${inquiryIndex}]`]: updated });
      toastApiOffline();
    }
  }
});
