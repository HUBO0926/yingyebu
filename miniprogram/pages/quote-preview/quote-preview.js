const { request, toastApiOffline } = require('../../utils/api');

const app = getApp();

function pad(number) {
  return String(number).padStart(2, '0');
}

function formatDateTime(date = new Date()) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function statusText(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'SENT') return '已发送';
  if (normalized === 'DRAFT') return '草稿';
  return '已生成';
}

function backendBase() {
  return app.globalData.apiBase.replace(/\/api\/?$/, '');
}

function resolveFileUrl(url) {
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith('/')) return `${backendBase()}${url}`;
  return `${backendBase()}/${url}`;
}

function selectedDocumentItems(quote) {
  const items = [];
  for (const testItem of quote.testItems || []) {
    for (const category of testItem.categories || []) {
      for (const product of category.selectedProducts || []) {
        items.push({
          measurement: testItem.name,
          categoryName: category.categoryName,
          name: product.name,
          model: product.model,
          unit: product.unit,
          quantity: product.quantity,
          standardPrice: product.standardPrice,
          subtotal: product.subtotal
        });
      }
    }
  }
  return items;
}

function enrichQuote(quote) {
  if (!quote) return null;
  const quoteNo = quote.quoteNo || `Q${Date.now()}`;
  return {
    ...quote,
    quoteNo,
    statusText: quote.statusText || statusText(quote.status),
    createdAt: quote.createdAt || formatDateTime(),
    wordFileName: quote.wordFileName || `${quoteNo}.docx`,
    documentDate: quote.documentDate || formatDateTime(),
    documentItems: quote.documentItems || selectedDocumentItems(quote)
  };
}

Page({
  data: {
    copy: {
      currency: '￥',
      separator: '·',
      emptyTitle: '暂无报价预览',
      emptyDesc: '请先从报价工具生成一份报价。',
      backToCreate: '返回报价工具',
      previewTitle: '报价预览',
      previewSubtitle: '确认测项、设备明细和报价金额后，可发送给客户或下载报价书。',
      quoteInfo: '报价信息',
      wordPreview: '报价模板 Word 文档',
      wordPreviewHint: '这里展示按当前报价模板生成的 Word 报价书预览，下载时会生成同名 docx 文件。',
      wordPreviewBadge: 'Word 预览',
      docProjectOverview: '一、项目概况',
      docQuoteItems: '二、设备报价明细',
      docTotal: '三、报价汇总',
      docFooter: '本报价为系统自动生成版本，正式报价以双方确认文件为准。',
      customerInfo: '客户项目',
      testItems: '测项与设备明细',
      totalSummary: '报价汇总',
      contactName: '联系人',
      company: '公司',
      phone: '电话',
      projectName: '项目',
      scenario: '场景',
      template: '模板',
      remark: '说明',
      siteCount: '测项数量',
      categorySkipped: '已跳过',
      categoryEmpty: '未选择产品',
      model: '型号',
      unit: '单位',
      standardPrice: '标准单价',
      quantity: '数量',
      parameters: '技术参数',
      summaryText: '产品简介',
      subtotal: '小计',
      originalAmount: '报价原价',
      discountRate: '折扣率',
      finalAmount: '折后金额',
      edit: '返回修改',
      send: '发送报价',
      download: '下载报价'
    },
    quote: null,
    sending: false,
    downloading: false
  },
  onLoad() {
    const quote = app.globalData.pendingQuotePreview;
    if (quote) {
      this.setData({ quote: enrichQuote(quote) });
    }
  },
  backToCreate() {
    wx.navigateBack({ delta: 1 });
  },
  markSent(extra = {}) {
    const nextQuote = {
      ...this.data.quote,
      ...extra,
      status: 'SENT',
      statusText: '已发送',
      sentAt: extra.sentAt || formatDateTime()
    };
    const enriched = enrichQuote(nextQuote);
    app.globalData.pendingQuotePreview = enriched;
    this.setData({ quote: enriched });
  },
  async sendQuote() {
    if (!this.data.quote || this.data.sending) return;
    this.setData({ sending: true });
    try {
      if (!this.data.quote.mockLocal && String(this.data.quote.id).indexOf('local-') !== 0) {
        const result = await request(`/quotes/${this.data.quote.id}/send`, { method: 'POST' });
        this.markSent(result);
      } else {
        this.markSent();
      }
      wx.showToast({ title: '报价已发送', icon: 'success' });
    } catch (error) {
      toastApiOffline();
      this.markSent({ mockLocal: true });
      wx.showModal({
        title: '本地演示发送',
        content: '后端暂未连接，已在小程序内模拟发送成功，正式上线后会接入报价消息通知。',
        showCancel: false
      });
    } finally {
      this.setData({ sending: false });
    }
  },
  async downloadQuote() {
    if (!this.data.quote || this.data.downloading) return;
    this.setData({ downloading: true });
    try {
      if (this.data.quote.mockLocal || String(this.data.quote.id).indexOf('local-') === 0) {
        this.showMockDownload();
        return;
      }
      const result = await request(`/quotes/${this.data.quote.id}/export`, { method: 'POST' });
      if (result.mock) {
        this.showMockDownload(result);
        return;
      }
      const fileUrl = resolveFileUrl(result.url);
      if (!fileUrl) {
        this.showMockDownload(result);
        return;
      }
      wx.downloadFile({
        url: fileUrl,
        success: (downloadResult) => {
          if (downloadResult.statusCode >= 400 || !downloadResult.tempFilePath) {
            this.showMockDownload(result);
            return;
          }
          wx.openDocument({
            filePath: downloadResult.tempFilePath,
            showMenu: true,
            fail: () => this.showMockDownload(result)
          });
        },
        fail: () => this.showMockDownload(result)
      });
    } catch (error) {
      toastApiOffline();
      this.showMockDownload();
    } finally {
      this.setData({ downloading: false });
    }
  },
  showMockDownload(result = {}) {
    const fileName = result.fileName || `${this.data.quote.quoteNo || 'quote'}.docx`;
    wx.showModal({
      title: '本地演示下载',
      content: `报价书 ${fileName} 已生成下载入口。后端文件服务接入后，将自动打开 Word 报价书。`,
      showCancel: false
    });
  }
});
