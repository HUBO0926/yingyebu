<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getData,
  postData,
  type Category,
  type Inquiry,
  type Product,
  type QuoteDetail,
  type QuoteSummary,
  type SalesUser
} from '../api';

interface QuoteProduct {
  productId: number;
  categoryId: number;
  categoryName: string;
  name: string;
  model: string;
  unit: string;
  standardPrice: number;
  imageUrl: string;
  quantity: number;
  selected: boolean;
  parameters: { label: string; value: string }[];
  summary: string;
}

interface QuoteCategory {
  categoryId: number;
  categoryName: string;
  skipped: boolean;
  products: QuoteProduct[];
}

interface QuoteTestItem {
  id: number;
  name: string;
  siteCount: number;
  categories: QuoteCategory[];
}

const REQUIRED_CATEGORY_NAMES = ['传感器', '采集仪', '通信', '安装附件', '供电系统', '安装耗材'];
const activeTab = ref('inquiries');
const loading = ref(false);
const quoteDrawer = ref(false);
const detailVisible = ref(false);
const detail = ref<QuoteDetail | null>(null);
const inquiries = ref<Inquiry[]>([]);
const quotes = ref<QuoteSummary[]>([]);
const salesUsers = ref<SalesUser[]>([]);
const products = ref<Product[]>([]);
const categories = ref<Category[]>([]);
const assignSelections = reactive<Record<number, string>>({});
const quoteForm = reactive({
  customerName: '张工',
  company: '华北矿业集团',
  projectName: '北山边坡自动化监测',
  contactPhone: '13800000000',
  scenario: '边坡监测',
  template: '标准设备报价书',
  discountRate: 95,
  remark: '含设备、基础辅材与现场联调服务。',
  testItemCount: 1
});
const testItems = ref<QuoteTestItem[]>([]);

const totals = computed(() => {
  let configuredCategories = 0;
  let productCount = 0;
  let amount = 0;
  for (const item of testItems.value) {
    const siteCount = Math.max(1, Number(item.siteCount || 1));
    for (const category of item.categories) {
      const selected = category.products.filter((product) => product.selected && product.quantity > 0);
      if (selected.length || category.skipped) configuredCategories += 1;
      for (const product of selected) {
        productCount += 1;
        amount += product.standardPrice * product.quantity * siteCount;
      }
    }
  }
  const finalAmount = amount * (Number(quoteForm.discountRate || 0) / 100);
  return {
    testItemCount: testItems.value.length,
    configuredCategories,
    productCount,
    amount: amount.toFixed(2),
    finalAmount: finalAmount.toFixed(2)
  };
});

async function refresh() {
  loading.value = true;
  try {
    const [nextInquiries, nextQuotes, nextSalesUsers, nextProducts, nextCategories] = await Promise.all([
      getData<Inquiry[]>('/admin/inquiries'),
      getData<QuoteSummary[]>('/admin/quotes'),
      getData<SalesUser[]>('/admin/sales-users'),
      getData<Product[]>('/admin/products'),
      getData<Category[]>('/admin/categories')
    ]);
    inquiries.value = nextInquiries;
    quotes.value = nextQuotes;
    salesUsers.value = nextSalesUsers;
    products.value = nextProducts;
    categories.value = nextCategories;
    nextInquiries.forEach((item) => {
      assignSelections[item.id] = item.assignedSalesId || nextSalesUsers[0]?.id || '';
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '询价报价数据加载失败，请确认后端 API 已启动');
  } finally {
    loading.value = false;
  }
}

function categoryByName(name: string, index: number) {
  return categories.value.find((item) => item.name === name) || { id: -(index + 1), name, sortOrder: index + 1 };
}

function unitForCategory(categoryName: string) {
  if (categoryName === '安装耗材') return '批';
  if (categoryName === '安装附件' || categoryName === '供电系统') return '套';
  return '台';
}

function productToQuoteProduct(product: Product, categoryName: string): QuoteProduct {
  return {
    productId: product.id,
    categoryId: product.categoryId,
    categoryName,
    name: product.name,
    model: product.model,
    unit: unitForCategory(categoryName),
    standardPrice: Number(product.price || 0),
    imageUrl: product.carouselImages?.[0] || product.imageUrl,
    quantity: 1,
    selected: false,
    parameters: Object.entries(product.parameters || {}).map(([label, value]) => ({ label, value })),
    summary: product.summary
  };
}

function productsForCategory(category: Category) {
  return products.value
    .filter((product) => product.categoryId === category.id || product.categoryName === category.name)
    .map((product) => productToQuoteProduct(product, category.name));
}

function createQuoteCategories(): QuoteCategory[] {
  return REQUIRED_CATEGORY_NAMES.map((name, index) => {
    const category = categoryByName(name, index);
    return {
      categoryId: category.id,
      categoryName: category.name,
      skipped: false,
      products: productsForCategory(category)
    };
  });
}

function createTestItem(index: number): QuoteTestItem {
  return {
    id: Date.now() + index,
    name: `测项 ${index}`,
    siteCount: 1,
    categories: createQuoteCategories()
  };
}

function resetQuoteForm(row?: Inquiry) {
  quoteForm.customerName = row?.contactName || '张工';
  quoteForm.company = row?.company || '华北矿业集团';
  quoteForm.projectName = row?.projectName || '北山边坡自动化监测';
  quoteForm.contactPhone = '13800000000';
  quoteForm.scenario = row?.projectName?.includes('水库') ? '水库大坝监测' : '边坡监测';
  quoteForm.template = '标准设备报价书';
  quoteForm.discountRate = 95;
  quoteForm.remark = '含设备、基础辅材与现场联调服务。';
  quoteForm.testItemCount = 1;
  testItems.value = [createTestItem(1)];
}

function openQuoteCreate(row?: Inquiry) {
  if (!products.value.length || !categories.value.length) {
    ElMessage.warning('产品库或分类为空，请先确认产品管理数据可用');
    return;
  }
  resetQuoteForm(row);
  quoteDrawer.value = true;
}

function changeTestItemCount(value: number | undefined) {
  const nextCount = Math.max(1, Math.min(12, Number(value || 1)));
  quoteForm.testItemCount = nextCount;
  while (testItems.value.length < nextCount) testItems.value.push(createTestItem(testItems.value.length + 1));
  testItems.value = testItems.value.slice(0, nextCount);
}

function addTestItem() {
  testItems.value.push(createTestItem(testItems.value.length + 1));
  quoteForm.testItemCount = testItems.value.length;
}

function selectedProducts(category: QuoteCategory) {
  return category.products.filter((product) => product.selected);
}

function productSubtotal(item: QuoteTestItem, product: QuoteProduct) {
  return (Number(item.siteCount || 1) * Number(product.quantity || 0) * Number(product.standardPrice || 0)).toFixed(2);
}

function categorySubtotal(item: QuoteTestItem, category: QuoteCategory) {
  return selectedProducts(category)
    .reduce((sum, product) => sum + Number(productSubtotal(item, product)), 0)
    .toFixed(2);
}

function itemSubtotal(item: QuoteTestItem) {
  return item.categories
    .reduce((sum, category) => sum + Number(categorySubtotal(item, category)), 0)
    .toFixed(2);
}

function toggleProduct(category: QuoteCategory, product: QuoteProduct, value: boolean) {
  category.skipped = false;
  product.selected = value;
  if (value && product.quantity < 1) product.quantity = 1;
}

function toggleSkip(category: QuoteCategory) {
  category.skipped = !category.skipped;
  if (category.skipped) {
    category.products.forEach((product) => {
      product.selected = false;
    });
  }
}

function buildPayload(status: 'draft' | 'generated') {
  return {
    status,
    customer: {
      name: quoteForm.customerName,
      company: quoteForm.company,
      phone: quoteForm.contactPhone
    },
    project: {
      name: quoteForm.projectName,
      scenario: quoteForm.scenario
    },
    template: quoteForm.template,
    discountRate: quoteForm.discountRate,
    remark: quoteForm.remark,
    testItems: testItems.value.map((item) => ({
      id: item.id,
      name: item.name,
      siteCount: item.siteCount,
      subtotal: itemSubtotal(item),
      categories: item.categories.map((category) => ({
        categoryId: category.categoryId,
        categoryName: category.categoryName,
        skipped: category.skipped,
        selectedProducts: selectedProducts(category).map((product) => ({
          productId: product.productId,
          name: product.name,
          model: product.model,
          unit: product.unit,
          standardPrice: product.standardPrice,
          imageUrl: product.imageUrl,
          quantity: product.quantity,
          parameters: product.parameters,
          summary: product.summary,
          subtotal: productSubtotal(item, product)
        }))
      }))
    })),
    totals: totals.value
  };
}

async function submitQuote(status: 'draft' | 'generated') {
  if (!quoteForm.customerName.trim() || !quoteForm.company.trim() || !quoteForm.projectName.trim()) {
    ElMessage.warning('请完整填写客户与项目信息');
    return;
  }
  if (totals.value.productCount === 0) {
    ElMessage.warning('请至少选择一个产品明细');
    return;
  }
  try {
    const serverQuote = await postData<{ id: number }>('/quotes', buildPayload(status));
    ElMessage.success(status === 'draft' ? '草稿已保存' : '报价已生成');
    await refresh();
    if (status === 'generated' && serverQuote.id) {
      await openDetail({ id: serverQuote.id } as QuoteSummary);
      quoteDrawer.value = false;
      activeTab.value = 'quotes';
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '报价提交失败');
  }
}

async function assign(row: Inquiry) {
  const salesId = assignSelections[row.id];
  if (!salesId) {
    ElMessage.warning('请选择销售人员');
    return;
  }
  await postData(`/admin/inquiries/${row.id}/assign`, { salesId });
  ElMessage.success('询价已分配');
  await refresh();
}

async function openDetail(row: QuoteSummary) {
  detail.value = await getData<QuoteDetail>(`/admin/quotes/${row.id}`);
  detailVisible.value = true;
}

async function sendQuote() {
  if (!detail.value) return;
  await postData(`/quotes/${detail.value.summary.id}/send`, {});
  ElMessage.success('报价已发送');
  await openDetail(detail.value.summary);
  await refresh();
}

async function exportQuote() {
  if (!detail.value) return;
  const result = await postData<{ fileName: string; url: string; mock: boolean }>(`/quotes/${detail.value.summary.id}/export`, {});
  ElMessage.success(`已生成 ${result.fileName}`);
}

onMounted(refresh);
</script>

<template>
  <section>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="询价分配" name="inquiries">
        <div class="toolbar">
          <span class="muted">管理员可将客户询价分配给任意销售，并可从询价快速创建报价。</span>
          <div class="toolbar-left">
            <el-button @click="refresh">刷新</el-button>
            <el-button type="primary" @click="openQuoteCreate()">新建报价</el-button>
          </div>
        </div>
        <el-table v-loading="loading" :data="inquiries" border empty-text="暂无询价">
          <el-table-column prop="projectName" label="项目名称" min-width="190" />
          <el-table-column prop="company" label="客户公司" min-width="160" />
          <el-table-column prop="contactName" label="联系人" width="110" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="assignedSalesName" label="当前销售" width="120" />
          <el-table-column label="分配销售" width="220">
            <template #default="{ row }">
              <el-select v-model="assignSelections[row.id]" placeholder="选择销售">
                <el-option v-for="item in salesUsers" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="assign(row)">分配</el-button>
              <el-button text type="success" @click="openQuoteCreate(row)">新建报价</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="报价记录" name="quotes">
        <div class="toolbar">
          <span class="muted">报价记录支持查看详情、Word 预览、发送与下载。</span>
          <el-button type="primary" @click="openQuoteCreate()">新建报价</el-button>
        </div>
        <el-table v-loading="loading" :data="quotes" border empty-text="暂无报价">
          <el-table-column prop="quoteNo" label="报价编号" min-width="170" />
          <el-table-column prop="projectName" label="项目名称" min-width="180" />
          <el-table-column prop="customerName" label="客户" min-width="140" />
          <el-table-column prop="ownerName" label="销售" width="100" />
          <el-table-column prop="totalAmount" label="金额" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="quoteDrawer" title="新建报价" size="86%">
      <el-form :model="quoteForm" label-width="94px" class="quote-form">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="联系人"><el-input v-model="quoteForm.customerName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="客户公司"><el-input v-model="quoteForm.company" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="联系电话"><el-input v-model="quoteForm.contactPhone" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="项目名称"><el-input v-model="quoteForm.projectName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="监测场景"><el-input v-model="quoteForm.scenario" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="报价模板"><el-input v-model="quoteForm.template" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="测项数量">
              <el-input-number v-model="quoteForm.testItemCount" :min="1" :max="12" style="width: 100%" @change="changeTestItemCount" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="折扣率">
              <el-input-number v-model="quoteForm.discountRate" :min="70" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-button class="full-action" @click="addTestItem">新增测项</el-button>
          </el-col>
        </el-row>
        <el-form-item label="报价说明"><el-input v-model="quoteForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>

      <div class="quote-summary-bar">
        <span>测项 {{ totals.testItemCount }}</span>
        <span>已配置类目 {{ totals.configuredCategories }}</span>
        <span>产品明细 {{ totals.productCount }}</span>
        <span>原价 ￥{{ totals.amount }}</span>
        <strong>折后 ￥{{ totals.finalAmount }}</strong>
      </div>

      <div v-for="(item, itemIndex) in testItems" :key="item.id" class="quote-test-card">
        <div class="test-card-head">
          <strong>{{ itemIndex + 1 }}. {{ item.name }}</strong>
          <span>小计 ￥{{ itemSubtotal(item) }}</span>
        </div>
        <el-row :gutter="16">
          <el-col :span="12"><el-input v-model="item.name" placeholder="测项名称" /></el-col>
          <el-col :span="12"><el-input-number v-model="item.siteCount" :min="1" style="width: 100%" /></el-col>
        </el-row>

        <el-collapse class="category-collapse">
          <el-collapse-item v-for="category in item.categories" :key="category.categoryName" :name="`${item.id}-${category.categoryName}`">
            <template #title>
              <div class="category-head">
                <span>{{ category.categoryName }}</span>
                <el-tag v-if="category.skipped" type="info">已跳过</el-tag>
                <el-tag v-else type="success">已选 {{ selectedProducts(category).length }} 项</el-tag>
                <strong>￥{{ categorySubtotal(item, category) }}</strong>
              </div>
            </template>
            <div class="category-actions">
              <el-button size="small" :type="category.skipped ? 'primary' : 'info'" @click="toggleSkip(category)">
                {{ category.skipped ? '恢复配置' : '跳过此类' }}
              </el-button>
            </div>
            <div v-if="!category.skipped" class="product-picker-grid">
              <div v-for="product in category.products" :key="product.productId" class="quote-product-card" :class="{ selected: product.selected }">
                <el-image :src="product.imageUrl" fit="cover" class="quote-product-image" />
                <div class="quote-product-body">
                  <div class="quote-product-top">
                    <strong>{{ product.name }}</strong>
                    <el-checkbox :model-value="product.selected" @change="(value: boolean) => toggleProduct(category, product, value)">
                      {{ product.selected ? '已选' : '选择' }}
                    </el-checkbox>
                  </div>
                  <p class="muted">{{ product.model }} · {{ product.unit }} · ￥{{ product.standardPrice }}</p>
                  <p>{{ product.summary }}</p>
                  <div class="parameter-tags">
                    <el-tag v-for="param in product.parameters" :key="param.label" size="small">{{ param.label }}：{{ param.value }}</el-tag>
                  </div>
                  <div v-if="product.selected" class="quantity-line">
                    <span>数量</span>
                    <el-input-number v-model="product.quantity" :min="1" :max="999" size="small" />
                    <strong>小计 ￥{{ productSubtotal(item, product) }}</strong>
                  </div>
                </div>
              </div>
              <el-empty v-if="category.products.length === 0" description="该分类暂无产品，请先到产品管理中维护" />
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <template #footer>
        <el-button @click="quoteDrawer = false">取消</el-button>
        <el-button @click="submitQuote('draft')">保存草稿</el-button>
        <el-button type="primary" @click="submitQuote('generated')">生成报价</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailVisible" title="报价详情" size="720px">
      <template v-if="detail">
        <el-descriptions border :column="2" class="section">
          <el-descriptions-item label="报价编号">{{ detail.summary.quoteNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.summary.status }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ detail.summary.projectName }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detail.summary.customerName }}</el-descriptions-item>
          <el-descriptions-item label="销售">{{ detail.summary.ownerName }}</el-descriptions-item>
          <el-descriptions-item label="金额">￥{{ detail.summary.totalAmount }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="detail.items" border class="section">
          <el-table-column prop="measurement" label="测项/类别" min-width="180" />
          <el-table-column prop="productName" label="产品" min-width="160" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="unitPrice" label="单价" width="110" />
          <el-table-column prop="amount" label="小计" width="110" />
        </el-table>

        <div class="word-preview">
          <div class="word-head">
            <strong>{{ detail.wordPreview.title }}</strong>
            <el-tag>{{ detail.wordPreview.fileName }}</el-tag>
          </div>
          <p class="muted">{{ detail.wordPreview.templateName }}</p>
          <table>
            <thead>
              <tr><th>测项</th><th>产品</th><th>数量</th><th>金额</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in detail.wordPreview.rows" :key="`${row.measurement}-${row.productName}`">
                <td>{{ row.measurement }}</td>
                <td>{{ row.productName }}</td>
                <td>{{ row.quantity }}</td>
                <td>￥{{ row.amount }}</td>
              </tr>
            </tbody>
          </table>
          <div class="word-total">合计：￥{{ detail.wordPreview.totalAmount }}</div>
          <p class="muted">{{ detail.wordPreview.footer }}</p>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button @click="exportQuote">下载 Word</el-button>
        <el-button type="primary" @click="sendQuote">发送报价</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.quote-form {
  padding-right: 18px;
}

.full-action {
  width: 100%;
}

.quote-summary-bar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 16px;
  border: 1px solid #d9e1ee;
  border-radius: 8px;
  background: #f8fbff;
}

.quote-summary-bar strong {
  color: #1677ff;
}

.quote-test-card {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  background: #ffffff;
}

.test-card-head,
.category-head,
.quote-product-top,
.quantity-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.test-card-head {
  margin-bottom: 14px;
}

.category-collapse {
  margin-top: 14px;
}

.category-head {
  width: 100%;
  padding-right: 12px;
}

.category-actions {
  margin-bottom: 12px;
}

.product-picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.quote-product-card {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  background: #ffffff;
}

.quote-product-card.selected {
  border-color: #1677ff;
  background: #f8fbff;
}

.quote-product-image {
  width: 108px;
  height: 86px;
  border-radius: 6px;
  background: #edf1f7;
}

.quote-product-body p {
  margin: 6px 0;
  line-height: 1.45;
}

.parameter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quantity-line {
  margin-top: 10px;
}
</style>
