const { request, toastApiOffline } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    copy: {
      search: '\u641c\u7d22\u4ea7\u54c1\u3001\u578b\u53f7\u3001\u5206\u7c7b',
      empty: '\u6682\u65e0\u5339\u914d\u4ea7\u54c1',
      offline: '\u540e\u7aef\u672a\u542f\u52a8\uff0c\u5f53\u524d\u4f7f\u7528\u672c\u5730\u6f14\u793a\u6570\u636e'
    },
    keyword: '',
    activeCategoryId: 0,
    categories: [],
    products: [],
    filtered: [],
    usingMock: false
  },
  onLoad() {
    this.loadProducts();
  },
  async loadProducts() {
    try {
      const categories = await request('/categories');
      const products = await request('/products');
      this.setData({
        categories: [{ id: 0, name: '\u5168\u90e8' }, ...categories],
        products,
        filtered: products,
        usingMock: false
      });
    } catch (error) {
      const mock = app.globalData.mockData;
      this.setData({
        categories: mock.categories,
        products: mock.products,
        filtered: mock.products,
        usingMock: true
      });
      toastApiOffline();
    }
  },
  onSearch(event) {
    this.setData({ keyword: event.detail.value });
    this.applyFilters();
  },
  chooseCategory(event) {
    this.setData({ activeCategoryId: Number(event.currentTarget.dataset.id) });
    this.applyFilters();
  },
  applyFilters() {
    const keyword = this.data.keyword.trim();
    const categoryId = this.data.activeCategoryId;
    const filtered = this.data.products.filter((item) => {
      const text = `${item.name}${item.model}${item.categoryName}`;
      const matchesKeyword = !keyword || text.includes(keyword);
      const matchesCategory = !categoryId || Number(item.categoryId) === categoryId;
      return matchesKeyword && matchesCategory;
    });
    this.setData({ filtered });
  },
  goDetail(event) {
    wx.navigateTo({ url: `/pages/product-detail/product-detail?id=${event.currentTarget.dataset.id}` });
  }
});
