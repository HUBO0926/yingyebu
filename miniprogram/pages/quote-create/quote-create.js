const { request, toastApiOffline } = require('../../utils/api');
const app = getApp();

const CATEGORY_DEFS = [
  { id: 'sensor', name: '\u4f20\u611f\u5668' },
  { id: 'collector', name: '\u91c7\u96c6\u4eea' },
  { id: 'communication', name: '\u901a\u4fe1' },
  { id: 'accessory', name: '\u5b89\u88c5\u9644\u4ef6' },
  { id: 'power', name: '\u4f9b\u7535\u7cfb\u7edf' },
  { id: 'material', name: '\u5b89\u88c5\u8017\u6750' }
];

const IMAGE_BASE = 'https://dummyimage.com/240x180';

const PRODUCT_CATALOG = [
  {
    id: 'sensor-dis-300',
    categoryId: 'sensor',
    name: '\u667a\u80fd\u4f4d\u79fb\u4f20\u611f\u5668',
    model: 'PS-DIS-300',
    imageUrl: `${IMAGE_BASE}/e8f2ff/1677ff&text=PS-DIS-300`,
    unit: '\u53f0',
    standardPrice: 6800,
    parameters: [
      { label: '\u91cf\u7a0b', value: '300mm' },
      { label: '\u7cbe\u5ea6', value: '0.1mm' },
      { label: '\u9632\u62a4', value: 'IP67' }
    ],
    summary: '\u7528\u4e8e\u8fb9\u5761\u3001\u575d\u4f53\u548c\u77ff\u5c71\u7ed3\u6784\u4f4d\u79fb\u8fde\u7eed\u76d1\u6d4b\u3002'
  },
  {
    id: 'sensor-rain-01',
    categoryId: 'sensor',
    name: '\u7ffb\u6597\u5f0f\u96e8\u91cf\u4f20\u611f\u5668',
    model: 'PS-RAIN-01',
    imageUrl: `${IMAGE_BASE}/edf7f1/16835b&text=PS-RAIN-01`,
    unit: '\u53f0',
    standardPrice: 1800,
    parameters: [
      { label: '\u5206\u8fa8\u7387', value: '0.2mm' },
      { label: '\u8f93\u51fa', value: 'RS485' },
      { label: '\u9632\u62a4', value: 'IP65' }
    ],
    summary: '\u91c7\u96c6\u73b0\u573a\u964d\u96e8\u6570\u636e\uff0c\u7528\u4e8e\u9884\u8b66\u9608\u503c\u5224\u65ad\u548c\u65e5\u62a5\u5206\u6790\u3002'
  },
  {
    id: 'collector-daq-16',
    categoryId: 'collector',
    name: '\u591a\u901a\u9053\u6570\u636e\u91c7\u96c6\u4eea',
    model: 'PS-DAQ-16',
    imageUrl: `${IMAGE_BASE}/f0f5ff/315efb&text=PS-DAQ-16`,
    unit: '\u53f0',
    standardPrice: 12800,
    parameters: [
      { label: '\u901a\u9053', value: '16' },
      { label: '\u901a\u4fe1', value: '4G / Ethernet' },
      { label: '\u4f9b\u7535', value: 'DC12V' }
    ],
    summary: '\u652f\u6301\u591a\u6d4b\u70b9\u63a5\u5165\u3001\u8fb9\u7f18\u7f13\u5b58\u548c\u8fdc\u7a0b\u914d\u7f6e\u3002'
  },
  {
    id: 'collector-edge-8',
    categoryId: 'collector',
    name: '\u8fb9\u7f18\u8ba1\u7b97\u91c7\u96c6\u7ec8\u7aef',
    model: 'PS-EDGE-8',
    imageUrl: `${IMAGE_BASE}/f7f0ff/722ed1&text=PS-EDGE-8`,
    unit: '\u53f0',
    standardPrice: 9800,
    parameters: [
      { label: '\u901a\u9053', value: '8' },
      { label: '\u5b58\u50a8', value: '32GB' },
      { label: '\u534f\u8bae', value: 'MQTT / HTTP' }
    ],
    summary: '\u9002\u5408\u5c0f\u578b\u76d1\u6d4b\u7ad9\u7684\u6570\u636e\u91c7\u96c6\u3001\u672c\u5730\u7f13\u5b58\u548c\u7b80\u5355\u8fb9\u7f18\u8ba1\u7b97\u3002'
  },
  {
    id: 'comm-gw-4g',
    categoryId: 'communication',
    name: '\u5de5\u4e1a 4G \u901a\u4fe1\u7f51\u5173',
    model: 'PS-GW-4G',
    imageUrl: `${IMAGE_BASE}/e6fffb/08979c&text=PS-GW-4G`,
    unit: '\u53f0',
    standardPrice: 3600,
    parameters: [
      { label: '\u7f51\u7edc', value: '4G Cat.4' },
      { label: '\u63a5\u53e3', value: 'RS485 / LAN' },
      { label: '\u6e29\u5ea6', value: '-20~70C' }
    ],
    summary: '\u9762\u5411\u91ce\u5916\u9879\u76ee\u7684\u6570\u636e\u56de\u4f20\u548c\u8bbe\u5907\u8fdc\u7a0b\u7ef4\u62a4\u3002'
  },
  {
    id: 'comm-lora-bridge',
    categoryId: 'communication',
    name: 'LoRa \u65e0\u7ebf\u7f51\u6865',
    model: 'PS-LORA-BR',
    imageUrl: `${IMAGE_BASE}/f6ffed/389e0d&text=PS-LORA`,
    unit: '\u5bf9',
    standardPrice: 2600,
    parameters: [
      { label: '\u9891\u6bb5', value: '470MHz' },
      { label: '\u8ddd\u79bb', value: '3km' },
      { label: '\u9632\u62a4', value: 'IP66' }
    ],
    summary: '\u7528\u4e8e\u5c71\u5730\u548c\u575d\u533a\u77ed\u8ddd\u79bb\u65e0\u7ebf\u4f20\u8f93\uff0c\u964d\u4f4e\u5e03\u7ebf\u6210\u672c\u3002'
  },
  {
    id: 'acc-bracket',
    categoryId: 'accessory',
    name: '\u4e0d\u9508\u94a2\u5b89\u88c5\u652f\u67b6',
    model: 'PS-BRACKET-S',
    imageUrl: `${IMAGE_BASE}/fff7e6/d46b08&text=BRACKET`,
    unit: '\u5957',
    standardPrice: 520,
    parameters: [
      { label: '\u6750\u8d28', value: '304' },
      { label: '\u9ad8\u5ea6', value: '1.2m' },
      { label: '\u5b89\u88c5', value: '\u5730\u57fa / \u5899\u9762' }
    ],
    summary: '\u7528\u4e8e\u4f20\u611f\u5668\u3001\u91c7\u96c6\u7bb1\u548c\u5929\u7ebf\u7684\u73b0\u573a\u56fa\u5b9a\u3002'
  },
  {
    id: 'acc-protect-box',
    categoryId: 'accessory',
    name: '\u5ba4\u5916\u9632\u62a4\u8bbe\u5907\u7bb1',
    model: 'PS-BOX-500',
    imageUrl: `${IMAGE_BASE}/fff1f0/c41d7f&text=PS-BOX`,
    unit: '\u4e2a',
    standardPrice: 880,
    parameters: [
      { label: '\u5c3a\u5bf8', value: '500x400x220' },
      { label: '\u9632\u62a4', value: 'IP65' },
      { label: '\u6750\u8d28', value: '\u51b7\u8f67\u94a2' }
    ],
    summary: '\u4fdd\u62a4\u91c7\u96c6\u4eea\u3001\u7535\u6e90\u548c\u901a\u4fe1\u8bbe\u5907\uff0c\u9002\u5408\u91ce\u5916\u957f\u671f\u90e8\u7f72\u3002'
  },
  {
    id: 'power-solar-120',
    categoryId: 'power',
    name: '\u592a\u9633\u80fd\u4f9b\u7535\u7bb1',
    model: 'PS-SOLAR-120',
    imageUrl: `${IMAGE_BASE}/fffbe6/d48806&text=SOLAR`,
    unit: '\u5957',
    standardPrice: 4200,
    parameters: [
      { label: '\u529f\u7387', value: '120W' },
      { label: '\u7535\u6c60', value: '80Ah' },
      { label: '\u9632\u62a4', value: 'IP65' }
    ],
    summary: '\u9002\u5408\u65e0\u5e02\u7535\u533a\u57df\u7684\u76d1\u6d4b\u7ad9\u6301\u7eed\u4f9b\u7535\u3002'
  },
  {
    id: 'power-battery-50',
    categoryId: 'power',
    name: '\u5907\u7528\u9502\u7535\u7535\u6e90',
    model: 'PS-BAT-50',
    imageUrl: `${IMAGE_BASE}/f9f0ff/531dab&text=BATTERY`,
    unit: '\u5957',
    standardPrice: 2300,
    parameters: [
      { label: '\u5bb9\u91cf', value: '50Ah' },
      { label: '\u8f93\u51fa', value: '12V' },
      { label: '\u5bff\u547d', value: '2000 cycles' }
    ],
    summary: '\u7528\u4e8e\u9634\u96e8\u5929\u6216\u5e02\u7535\u4e2d\u65ad\u65f6\u7684\u76d1\u6d4b\u7ad9\u5907\u7528\u4f9b\u7535\u3002'
  },
  {
    id: 'mat-cable-kit',
    categoryId: 'material',
    name: '\u9632\u6c34\u63a5\u5934\u4e0e\u7ebf\u7f06\u5305',
    model: 'PS-CABLE-KIT',
    imageUrl: `${IMAGE_BASE}/f0f5ff/1d39c4&text=CABLE`,
    unit: '\u6279',
    standardPrice: 960,
    parameters: [
      { label: '\u7ebf\u7f06', value: '100m' },
      { label: '\u63a5\u5934', value: '20pcs' },
      { label: '\u9632\u62a4', value: 'IP67' }
    ],
    summary: '\u7528\u4e8e\u4f20\u611f\u5668\u5230\u91c7\u96c6\u4eea\u7684\u73b0\u573a\u63a5\u7ebf\u548c\u9632\u6c34\u5904\u7406\u3002'
  },
  {
    id: 'mat-install-pack',
    categoryId: 'material',
    name: '\u57fa\u7840\u5b89\u88c5\u8017\u6750\u5305',
    model: 'PS-INSTALL-PACK',
    imageUrl: `${IMAGE_BASE}/f5f5f5/595959&text=INSTALL`,
    unit: '\u6279',
    standardPrice: 680,
    parameters: [
      { label: '\u542b\u91cf', value: '\u87ba\u6813 / \u80f6\u5e26 / \u6807\u7b7e' },
      { label: '\u9002\u7528', value: '1 station' },
      { label: '\u7c7b\u578b', value: '\u901a\u7528' }
    ],
    summary: '\u8986\u76d6\u5355\u4e2a\u76d1\u6d4b\u7ad9\u7684\u5e38\u7528\u5b89\u88c5\u8017\u6750\u548c\u6807\u8bc6\u6750\u6599\u3002'
  }
];

function productsForCategory(categoryId, selected = {}) {
  return PRODUCT_CATALOG.filter((product) => product.categoryId === categoryId).map((product) => ({
    ...product,
    productId: product.id,
    categoryName: CATEGORY_DEFS.find((category) => category.id === categoryId).name,
    selected: Boolean(selected[product.id]),
    quantity: selected[product.id] || 1,
    subtotal: 0
  }));
}

function createCategories(selected = {}) {
  return CATEGORY_DEFS.map((category, index) => ({
    ...category,
    active: index === 0,
    skipped: false,
    selectedCount: 0,
    statusText: '\u5f85\u914d\u7f6e',
    categorySubtotal: '0.00',
    products: productsForCategory(category.id, selected[category.id] || {})
  }));
}

function createTestItem(id, overrides = {}) {
  return {
    id,
    name: overrides.name || `\u6d4b\u9879 ${id}`,
    siteCount: Math.max(1, Number(overrides.siteCount || 1)),
    activeCategoryId: 'sensor',
    subtotal: '0.00',
    categories: createCategories(overrides.selected || {})
  };
}

function pad(number) {
  return String(number).padStart(2, '0');
}

function createLocalQuoteNo() {
  const now = new Date();
  const stamp = [
    now.getFullYear(),
    pad(now.getMonth() + 1),
    pad(now.getDate()),
    pad(now.getHours()),
    pad(now.getMinutes()),
    pad(now.getSeconds())
  ].join('');
  return `Q${stamp}`;
}

function formatDateTime(date = new Date()) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

Page({
  data: {
    copy: {
      currency: '\uffe5',
      separator: '\u00b7',
      heroKicker: '\u9500\u552e\u62a5\u4ef7\u5de5\u4f5c\u53f0',
      heroTitle: '\u521b\u5efa\u62a5\u4ef7\u8349\u7a3f',
      heroSubtitle: '\u5148\u786e\u8ba4\u6d4b\u9879\u6570\u91cf\uff0c\u518d\u4e3a\u6bcf\u4e2a\u6d4b\u9879\u914d\u7f6e\u516d\u7c7b\u8bbe\u5907\u660e\u7ec6\u3002',
      amountDue: '\u5e94\u62a5\u91d1\u989d',
      customer: '\u5ba2\u6237\u4e0e\u9879\u76ee',
      contactName: '\u8054\u7cfb\u4eba',
      company: '\u516c\u53f8\u540d\u79f0',
      projectName: '\u9879\u76ee\u540d\u79f0',
      contactPhone: '\u8054\u7cfb\u7535\u8bdd',
      scenario: '\u76d1\u6d4b\u573a\u666f',
      testItemSetup: '\u6d4b\u9879\u6570\u91cf',
      testItemSetupHint: '\u6d4b\u9879\u76f8\u5f53\u4e8e\u7ad9\u70b9\u7c7b\u578b\uff0c\u6bcf\u4e2a\u6d4b\u9879\u5185\u72ec\u7acb\u914d\u7f6e\u8bbe\u5907\u3002',
      testItems: '\u6d4b\u9879\u4e0e\u516d\u7c7b\u8bbe\u5907',
      testItemName: '\u6d4b\u9879\u540d\u79f0',
      siteCount: '\u6d4b\u9879\u6570\u91cf',
      categoryProgress: '\u7c7b\u76ee\u8fdb\u5ea6',
      addTestItem: '\u65b0\u589e\u6d4b\u9879',
      skipCategory: '\u8df3\u8fc7\u6b64\u7c7b',
      restoreCategory: '\u6062\u590d\u914d\u7f6e',
      openProductLibrary: '\u4ece\u4ea7\u54c1\u5e93\u9009\u62e9',
      productLibrary: '\u4ea7\u54c1\u5e93',
      pickerHint: '\u4e0b\u6ed1\u67e5\u770b\u66f4\u591a\u4ea7\u54c1\uff0c\u70b9\u51fb\u5373\u53ef\u591a\u9009\u6216\u53d6\u6d88\u3002',
      selectedProducts: '\u5df2\u9009\u8bbe\u5907\u660e\u7ec6',
      close: '\u5b8c\u6210',
      select: '\u9009\u62e9',
      selected: '\u5df2\u9009',
      productInfo: '\u4ea7\u54c1\u4fe1\u606f',
      model: '\u578b\u53f7',
      unit: '\u5355\u4f4d',
      standardPrice: '\u6807\u51c6\u5355\u4ef7',
      parameters: '\u6280\u672f\u53c2\u6570',
      summaryText: '\u4ea7\u54c1\u7b80\u4ecb',
      quantity: '\u6570\u91cf',
      subtotal: '\u5c0f\u8ba1',
      skipped: '\u5df2\u8df3\u8fc7',
      emptyCategory: '\u672c\u7c7b\u672a\u9009\u62e9\u4ea7\u54c1\uff0c\u53ef\u4ece\u4ea7\u54c1\u5e93\u9009\u62e9\u6216\u8df3\u8fc7\u6b64\u7c7b\u3002',
      template: '\u62a5\u4ef7\u6a21\u677f',
      choose: '\u9009\u62e9',
      discountRate: '\u6298\u6263\u7387',
      remark: '\u62a5\u4ef7\u8bf4\u660e',
      summary: '\u62a5\u4ef7\u6c47\u603b',
      configuredCategories: '\u5df2\u914d\u7f6e\u7c7b\u76ee',
      productCount: '\u4ea7\u54c1\u660e\u7ec6',
      originalAmount: '\u62a5\u4ef7\u539f\u4ef7',
      finalAmount: '\u6298\u540e\u91d1\u989d',
      saveDraft: '\u4fdd\u5b58\u8349\u7a3f',
      generate: '\u751f\u6210\u62a5\u4ef7'
    },
    scenarios: [
      '\u8fb9\u5761\u76d1\u6d4b',
      '\u5c3e\u77ff\u5e93\u76d1\u6d4b',
      '\u6c34\u5e93\u5927\u575d\u76d1\u6d4b',
      '\u5730\u8d28\u707e\u5bb3\u76d1\u6d4b'
    ],
    templates: ['\u6807\u51c6\u8bbe\u5907\u62a5\u4ef7\u4e66', '\u65b9\u6848\u578b\u62a5\u4ef7\u4e66', '\u7b80\u7248\u9884\u7b97\u5355'],
    form: {
      customerName: '\u5f20\u5de5',
      company: '\u534e\u5317\u77ff\u4e1a\u96c6\u56e2',
      projectName: '\u5317\u5c71\u8fb9\u5761\u81ea\u52a8\u5316\u76d1\u6d4b',
      contactPhone: '13800000000',
      scenarioIndex: 0,
      templateIndex: 0,
      discountRate: 95,
      testItemCount: 2,
      remark: '\u542b\u8bbe\u5907\u3001\u57fa\u7840\u8f85\u6750\u4e0e\u73b0\u573a\u8054\u8c03\u670d\u52a1\u3002'
    },
    testItems: [
      createTestItem(1, {
        name: '\u8868\u9762\u4f4d\u79fb\u76d1\u6d4b\u7ad9',
        siteCount: 2,
        selected: {
          sensor: { 'sensor-dis-300': 1 },
          collector: { 'collector-daq-16': 1 },
          communication: { 'comm-gw-4g': 1 },
          accessory: { 'acc-bracket': 1 },
          power: { 'power-solar-120': 1 },
          material: { 'mat-cable-kit': 1 }
        }
      }),
      createTestItem(2, {
        name: '\u96e8\u91cf\u901a\u4fe1\u76d1\u6d4b\u7ad9',
        siteCount: 1,
        selected: {
          sensor: { 'sensor-rain-01': 1 },
          collector: { 'collector-edge-8': 1 },
          communication: { 'comm-lora-bridge': 1 },
          material: { 'mat-install-pack': 1 }
        }
      })
    ],
    totals: {
      testItemCount: 0,
      configuredCategories: 0,
      productCount: 0,
      amount: '0.00',
      discountAmount: '0.00',
      finalAmount: '0.00'
    },
    picker: {
      visible: false,
      testIndex: -1,
      categoryIndex: -1,
      categoryName: '',
      products: []
    },
    submitting: false
  },
  onLoad() {
    this.recalculate();
  },
  updateForm(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: event.detail.value });
  },
  selectScenario(event) {
    this.setData({ 'form.scenarioIndex': Number(event.currentTarget.dataset.index) });
  },
  selectTemplate(event) {
    this.setData({ 'form.templateIndex': Number(event.detail.value) });
  },
  changeDiscount(event) {
    this.setData({ 'form.discountRate': Number(event.detail.value) });
    this.recalculate();
  },
  changeTestItemCount(event) {
    const nextCount = Math.max(1, Math.min(12, Number(event.detail.value || 1)));
    const items = [...this.data.testItems];
    while (items.length < nextCount) {
      items.push(createTestItem(Date.now() + items.length, { name: `\u6d4b\u9879 ${items.length + 1}`, siteCount: 1 }));
    }
    this.setData({
      'form.testItemCount': nextCount,
      testItems: items.slice(0, nextCount)
    });
    this.recalculate();
  },
  addTestItem() {
    const items = [...this.data.testItems];
    const nextIndex = items.length + 1;
    items.push(createTestItem(Date.now(), { name: `\u6d4b\u9879 ${nextIndex}`, siteCount: 1 }));
    this.setData({
      'form.testItemCount': items.length,
      testItems: items
    });
    this.recalculate();
  },
  updateTestItem(event) {
    const testIndex = Number(event.currentTarget.dataset.testIndex);
    const field = event.currentTarget.dataset.field;
    const testItems = this.data.testItems.map((item, index) => {
      if (index !== testIndex) return item;
      const value = field === 'name' ? event.detail.value : Math.max(1, Number(event.detail.value || 1));
      return { ...item, [field]: value };
    });
    this.setData({ testItems });
    this.recalculate();
  },
  activateCategory(event) {
    const testIndex = Number(event.currentTarget.dataset.testIndex);
    const categoryId = event.currentTarget.dataset.categoryId;
    const testItems = this.data.testItems.map((item, index) => {
      if (index !== testIndex) return item;
      return {
        ...item,
        activeCategoryId: categoryId,
        categories: item.categories.map((category) => ({ ...category, active: category.id === categoryId }))
      };
    });
    this.setData({ testItems });
  },
  openProductPicker(event) {
    const testIndex = Number(event.currentTarget.dataset.testIndex);
    const categoryIndex = Number(event.currentTarget.dataset.categoryIndex);
    const category = this.data.testItems[testIndex].categories[categoryIndex];
    this.setData({
      picker: {
        visible: true,
        testIndex,
        categoryIndex,
        categoryName: category.name,
        products: category.products
      }
    });
  },
  closeProductPicker() {
    this.setData({
      picker: {
        visible: false,
        testIndex: -1,
        categoryIndex: -1,
        categoryName: '',
        products: []
      }
    });
  },
  noop() {},
  toggleSkipCategory(event) {
    const testIndex = Number(event.currentTarget.dataset.testIndex);
    const categoryIndex = Number(event.currentTarget.dataset.categoryIndex);
    const testItems = this.data.testItems.map((item, itemIndex) => {
      if (itemIndex !== testIndex) return item;
      const categories = item.categories.map((category, index) => {
        if (index !== categoryIndex) return category;
        const skipped = !category.skipped;
        return {
          ...category,
          skipped,
          products: skipped ? category.products.map((product) => ({ ...product, selected: false })) : category.products
        };
      });
      return { ...item, categories };
    });
    this.setData({ testItems });
    this.recalculate();
  },
  toggleProduct(event) {
    const testIndex = Number(event.currentTarget.dataset.testIndex);
    const categoryIndex = Number(event.currentTarget.dataset.categoryIndex);
    const productIndex = Number(event.currentTarget.dataset.productIndex);
    const testItems = this.data.testItems.map((item, itemIndex) => {
      if (itemIndex !== testIndex) return item;
      const categories = item.categories.map((category, catIndex) => {
        if (catIndex !== categoryIndex) return category;
        return {
          ...category,
          skipped: false,
          products: category.products.map((product, prodIndex) => {
            if (prodIndex !== productIndex) return product;
            return { ...product, selected: !product.selected };
          })
        };
      });
      return { ...item, categories };
    });
    const nextData = { testItems };
    if (this.data.picker.visible && this.data.picker.testIndex === testIndex && this.data.picker.categoryIndex === categoryIndex) {
      nextData['picker.products'] = testItems[testIndex].categories[categoryIndex].products;
    }
    this.setData(nextData);
    this.recalculate();
  },
  updateProductQuantity(event) {
    const testIndex = Number(event.currentTarget.dataset.testIndex);
    const categoryIndex = Number(event.currentTarget.dataset.categoryIndex);
    const productIndex = Number(event.currentTarget.dataset.productIndex);
    const quantity = Math.max(0, Number(event.detail.value || 0));
    const testItems = this.data.testItems.map((item, itemIndex) => {
      if (itemIndex !== testIndex) return item;
      const categories = item.categories.map((category, catIndex) => {
        if (catIndex !== categoryIndex) return category;
        return {
          ...category,
          products: category.products.map((product, prodIndex) => {
            if (prodIndex !== productIndex) return product;
            return { ...product, quantity, selected: quantity > 0 ? product.selected : false };
          })
        };
      });
      return { ...item, categories };
    });
    const nextData = { testItems };
    if (this.data.picker.visible && this.data.picker.testIndex === testIndex && this.data.picker.categoryIndex === categoryIndex) {
      nextData['picker.products'] = testItems[testIndex].categories[categoryIndex].products;
    }
    this.setData(nextData);
    this.recalculate();
  },
  recalculate() {
    let configuredCategories = 0;
    let productCount = 0;
    let amount = 0;
    const testItems = this.data.testItems.map((item) => {
      let itemSubtotal = 0;
      const siteCount = Math.max(1, Number(item.siteCount || 1));
      const categories = item.categories.map((category) => {
        let selectedCount = 0;
        let categorySubtotal = 0;
        const products = category.products.map((product) => {
          const quantity = Number(product.quantity || 0);
          const selected = Boolean(product.selected) && quantity > 0 && !category.skipped;
          const subtotal = selected ? Number(product.standardPrice || 0) * quantity * siteCount : 0;
          if (selected) {
            selectedCount += 1;
            productCount += 1;
            categorySubtotal += subtotal;
          }
          return { ...product, selected, subtotal: subtotal.toFixed(2) };
        });
        if (selectedCount > 0 || category.skipped) configuredCategories += 1;
        itemSubtotal += categorySubtotal;
        return {
          ...category,
          selectedCount,
          statusText: category.skipped ? '\u5df2\u8df3\u8fc7' : selectedCount > 0 ? `\u5df2\u9009${selectedCount}\u9879` : '\u5f85\u914d\u7f6e',
          categorySubtotal: categorySubtotal.toFixed(2),
          products
        };
      });
      amount += itemSubtotal;
      return { ...item, siteCount, subtotal: itemSubtotal.toFixed(2), categories };
    });
    const finalAmount = amount * (Number(this.data.form.discountRate || 0) / 100);
    this.setData({
      testItems,
      totals: {
        testItemCount: testItems.length,
        configuredCategories,
        productCount,
        amount: amount.toFixed(2),
        discountAmount: finalAmount.toFixed(2),
        finalAmount: finalAmount.toFixed(2)
      }
    });
  },
  validate() {
    const required = [
      ['customerName', '\u5ba2\u6237\u8054\u7cfb\u4eba'],
      ['company', '\u516c\u53f8\u540d\u79f0'],
      ['projectName', '\u9879\u76ee\u540d\u79f0']
    ];
    for (const [field, label] of required) {
      if (!this.data.form[field].trim()) {
        wx.showToast({ title: `\u8bf7\u586b\u5199${label}`, icon: 'none' });
        return false;
      }
    }
    return true;
  },
  buildPayload(status) {
    return {
      status,
      customer: {
        name: this.data.form.customerName,
        company: this.data.form.company,
        phone: this.data.form.contactPhone
      },
      project: {
        name: this.data.form.projectName,
        scenario: this.data.scenarios[this.data.form.scenarioIndex]
      },
      template: this.data.templates[this.data.form.templateIndex],
      discountRate: this.data.form.discountRate,
      remark: this.data.form.remark,
      testItems: this.data.testItems.map((item) => ({
        id: item.id,
        name: item.name,
        siteCount: item.siteCount,
        subtotal: item.subtotal,
        categories: item.categories.map((category) => ({
          categoryId: category.id,
          categoryName: category.name,
          skipped: category.skipped,
          selectedProducts: category.products
            .filter((product) => product.selected)
            .map((product) => ({
              productId: product.productId,
              name: product.name,
              model: product.model,
              unit: product.unit,
              standardPrice: product.standardPrice,
              imageUrl: product.imageUrl,
              quantity: product.quantity,
              parameters: product.parameters,
              summary: product.summary,
              subtotal: product.subtotal
            }))
        }))
      })),
      totals: this.data.totals
    };
  },
  buildPreview(payload, serverQuote = null, options = {}) {
    const quoteNo = serverQuote && serverQuote.quoteNo ? serverQuote.quoteNo : createLocalQuoteNo();
    const id = serverQuote && serverQuote.id ? serverQuote.id : `local-${Date.now()}`;
    return {
      ...payload,
      id,
      quoteNo,
      status: 'GENERATED',
      statusText: '\u5df2\u751f\u6210',
      createdAt: serverQuote && serverQuote.createdAt ? serverQuote.createdAt : formatDateTime(),
      mockLocal: Boolean(options.mockLocal),
      totals: {
        ...payload.totals,
        finalAmount: serverQuote && serverQuote.totalAmount ? Number(serverQuote.totalAmount).toFixed(2) : payload.totals.finalAmount
      }
    };
  },
  openPreview(preview) {
    app.globalData.pendingQuotePreview = preview;
    wx.navigateTo({ url: '/pages/quote-preview/quote-preview' });
  },
  async submit(event) {
    const status = event.currentTarget.dataset.status;
    if (!this.validate()) return;
    const payload = this.buildPayload(status);
    this.setData({ submitting: true });
    try {
      const serverQuote = await request('/quotes', { method: 'POST', data: payload });
      if (status === 'draft') {
        wx.showToast({ title: '\u8349\u7a3f\u5df2\u4fdd\u5b58' });
        return;
      }
      this.openPreview(this.buildPreview(payload, serverQuote));
    } catch (error) {
      toastApiOffline();
      if (status === 'draft') {
        wx.showModal({
          title: '\u672c\u5730\u6f14\u793a\u62a5\u4ef7',
          content: `\u8349\u7a3f\u5df2\u4fdd\u7559\u5728\u5f53\u524d\u9875\u9762\uff1a${this.data.form.projectName}\uff0c\u62a5\u4ef7\u91d1\u989d ${this.data.copy.currency}${this.data.totals.finalAmount}\u3002`,
          showCancel: false
        });
        return;
      }
      this.openPreview(this.buildPreview(payload, null, { mockLocal: true }));
    } finally {
      this.setData({ submitting: false });
    }
  }
});
