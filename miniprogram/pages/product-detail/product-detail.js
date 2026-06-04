const { request, toastApiOffline } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    copy: {
      parameters: '\u6280\u672f\u53c2\u6570',
      attachment: '\u4ea7\u54c1\u9644\u4ef6',
      attachmentHint: '\u7b2c\u4e00\u9636\u6bb5\u9644\u4ef6\u9884\u7559\uff0c\u751f\u4ea7\u73af\u5883\u7edf\u4e00\u8d70 MinIO \u5b58\u50a8\u62bd\u8c61\u3002',
      inquiry: '\u8bbe\u5907\u8be2\u4ef7',
      share: '\u5206\u4eab',
      empty: '\u4ea7\u54c1\u4e0d\u5b58\u5728'
    },
    product: null,
    params: []
  },
  onLoad(options) {
    this.loadProduct(options.id || 1);
  },
  async loadProduct(id) {
    try {
      const product = await request(`/products/${id}`);
      this.setProduct(product);
    } catch (error) {
      const product = app.globalData.mockData.products.find((item) => Number(item.id) === Number(id)) || app.globalData.mockData.products[0];
      this.setProduct(product);
      toastApiOffline();
    }
  },
  setProduct(product) {
    const params = Object.keys(product.parameters || {}).map((key) => ({ key, value: product.parameters[key] }));
    this.setData({ product, params });
  },
  goInquiry() {
    wx.navigateTo({ url: '/pages/inquiry/inquiry' });
  }
});
