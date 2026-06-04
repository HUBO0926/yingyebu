<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  deleteData,
  getData,
  postData,
  putData,
  type Category,
  type FileRecord,
  type Product
} from '../api';
import MultiImageUpload from '../components/MultiImageUpload.vue';
import RichTextEditor from '../components/RichTextEditor.vue';

interface ProductForm {
  id?: number;
  categoryId: number | null;
  name: string;
  model: string;
  summary: string;
  price: number;
  parametersText: string;
  carouselImages: string[];
  detailHtml: string;
  attachments: FileRecord[];
  uploadedBy: string;
}

interface CategoryForm {
  id?: number;
  name: string;
  sortOrder: number;
}

const keyword = ref('');
const categoryId = ref<number | ''>('');
const loading = ref(false);
const drawerVisible = ref(false);
const categoryDialogVisible = ref(false);
const products = ref<Product[]>([]);
const categories = ref<Category[]>([]);
const form = reactive<ProductForm>(emptyProductForm());
const categoryForm = reactive<CategoryForm>(emptyCategoryForm());

const filtered = computed(() =>
  products.value.filter((item) => {
    const text = [item.name, item.model, item.categoryName, item.summary].join(' ');
    const matchKeyword = !keyword.value || text.includes(keyword.value);
    const matchCategory = categoryId.value === '' || item.categoryId === categoryId.value;
    return matchKeyword && matchCategory;
  })
);

function defaultCategoryId() {
  return categories.value[0]?.id || null;
}

function emptyProductForm(): ProductForm {
  return {
    categoryId: defaultCategoryId(),
    name: '',
    model: '',
    summary: '',
    price: 0,
    parametersText: '量程：300mm\n精度：0.1mm\n防护：IP67',
    carouselImages: [],
    detailHtml: '<h2>产品图文详情</h2><p>请填写产品应用场景、核心能力、技术亮点和安装说明。</p>',
    attachments: [],
    uploadedBy: '管理员'
  };
}

function emptyCategoryForm(): CategoryForm {
  return { name: '', sortOrder: 99 };
}

function resetProductForm(next: ProductForm = emptyProductForm()) {
  Object.assign(form, next, {
    carouselImages: [...next.carouselImages],
    attachments: [...next.attachments]
  });
}

function resetCategoryForm(next: CategoryForm = emptyCategoryForm()) {
  Object.assign(categoryForm, next);
}

function parametersText(parameters: Record<string, string>) {
  return Object.entries(parameters || {}).map(([key, value]) => `${key}：${value}`).join('\n');
}

function parseParameters(text: string) {
  return text.split('\n').map((line) => line.trim()).filter(Boolean).reduce<Record<string, string>>((result, line) => {
    const [key, ...rest] = line.split(/[：:]/);
    if (key) result[key.trim()] = rest.join('：').trim() || '待补充';
    return result;
  }, {});
}

async function refresh() {
  loading.value = true;
  try {
    const [nextProducts, nextCategories] = await Promise.all([
      getData<Product[]>('/admin/products'),
      getData<Category[]>('/admin/categories')
    ]);
    products.value = nextProducts;
    categories.value = nextCategories;
    if (!form.categoryId && nextCategories.length) form.categoryId = nextCategories[0].id;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '产品资料加载失败，请确认后端 API 已启动');
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  if (!categories.value.length) {
    ElMessage.warning('请先新增产品分类，或确认后端分类接口可用');
    return;
  }
  resetProductForm({
    ...emptyProductForm(),
    categoryId: defaultCategoryId()
  });
  drawerVisible.value = true;
}

function openEdit(row: Product) {
  resetProductForm({
    id: row.id,
    categoryId: row.categoryId,
    name: row.name,
    model: row.model,
    summary: row.summary,
    price: Number(row.price),
    parametersText: parametersText(row.parameters),
    carouselImages: row.carouselImages?.length ? row.carouselImages : [row.imageUrl].filter(Boolean),
    detailHtml: row.detailHtml || '',
    attachments: row.attachments || [],
    uploadedBy: row.uploadedBy || '管理员'
  });
  drawerVisible.value = true;
}

function trackUpload(record: FileRecord) {
  form.attachments = [record, ...form.attachments];
}

async function submit() {
  if (!form.categoryId) {
    ElMessage.warning('请选择产品分类');
    return;
  }
  if (!form.name.trim()) {
    ElMessage.warning('请填写产品名称');
    return;
  }
  const payload = {
    categoryId: form.categoryId,
    name: form.name.trim(),
    model: form.model.trim(),
    summary: form.summary.trim(),
    imageUrl: form.carouselImages[0] || '',
    price: form.price,
    parameters: parseParameters(form.parametersText),
    carouselImages: form.carouselImages,
    detailHtml: form.detailHtml,
    attachments: form.attachments,
    uploadedBy: form.uploadedBy || '管理员'
  };
  try {
    if (form.id) {
      await putData(`/admin/products/${form.id}`, payload);
      ElMessage.success('产品已更新');
    } else {
      await postData('/admin/products', payload);
      ElMessage.success('产品已新增');
    }
    drawerVisible.value = false;
    await refresh();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '产品保存失败');
  }
}

async function remove(row: Product) {
  await ElMessageBox.confirm(`确认删除产品“${row.name}”？`, '删除确认', { type: 'warning' });
  await deleteData(`/admin/products/${row.id}`);
  ElMessage.success('产品已删除');
  await refresh();
}

function openCategoryCreate() {
  resetCategoryForm();
  categoryDialogVisible.value = true;
}

function openCategoryEdit(row: Category) {
  resetCategoryForm({ id: row.id, name: row.name, sortOrder: row.sortOrder });
  categoryDialogVisible.value = true;
}

async function submitCategory() {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请填写分类名称');
    return;
  }
  const payload = { name: categoryForm.name.trim(), sortOrder: categoryForm.sortOrder };
  try {
    const saved = categoryForm.id
      ? await putData<Category>(`/admin/categories/${categoryForm.id}`, payload)
      : await postData<Category>('/admin/categories', payload);
    ElMessage.success(categoryForm.id ? '分类已更新' : '分类已新增');
    resetCategoryForm();
    await refresh();
    form.categoryId = saved.id;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分类保存失败');
  }
}

async function removeCategory(row: Category) {
  await ElMessageBox.confirm(`确认删除分类“${row.name}”？已有产品的分类不能删除。`, '删除确认', { type: 'warning' });
  try {
    await deleteData(`/admin/categories/${row.id}`);
    ElMessage.success('分类已删除');
    await refresh();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分类删除失败');
  }
}

onMounted(refresh);
</script>

<template>
  <section>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索产品、型号、分类" clearable style="width: 280px" />
        <el-select v-model="categoryId" placeholder="全部分类" clearable style="width: 180px">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </div>
      <div class="toolbar-left">
        <el-button @click="openCategoryCreate">分类管理</el-button>
        <el-button type="primary" @click="openCreate">新增产品</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="filtered" border empty-text="暂无产品">
      <el-table-column label="轮播图" width="118">
        <template #default="{ row }">
          <el-image :src="row.carouselImages?.[0] || row.imageUrl" fit="cover" class="table-image" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="产品名称" min-width="170" />
      <el-table-column prop="model" label="型号" width="150" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="summary" label="产品简介" min-width="260" show-overflow-tooltip />
      <el-table-column prop="price" label="标准单价" width="130" />
      <el-table-column prop="uploadedBy" label="上传人" width="110" />
      <el-table-column prop="updatedAt" label="更新时间" width="190" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawerVisible" :title="form.id ? '编辑产品资料' : '新增产品资料'" size="76%">
      <el-form :model="form" label-width="104px" class="editor-form">
        <el-form-item label="展示轮播图">
          <MultiImageUpload v-model="form.carouselImages" module="product-carousel" :uploaded-by="form.uploadedBy" @uploaded="trackUpload" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上传人"><el-input v-model="form.uploadedBy" /></el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="产品名称"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="标准单价"><el-input-number v-model="form.price" :min="0" :precision="2" style="width: 220px" /></el-form-item>
        <el-form-item label="产品简介"><el-input v-model="form.summary" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="技术参数">
          <el-input v-model="form.parametersText" type="textarea" :rows="5" placeholder="每行一个参数，例如：量程：300mm" />
        </el-form-item>
        <el-form-item label="图文详情">
          <RichTextEditor v-model="form.detailHtml" module="product-editor" :uploaded-by="form.uploadedBy" @uploaded="trackUpload" />
        </el-form-item>
        <el-form-item label="上传记录">
          <el-table :data="form.attachments" size="small" border class="nested-table" empty-text="暂无上传记录">
            <el-table-column prop="originalName" label="文件名" min-width="180" />
            <el-table-column prop="module" label="模块" width="140" />
            <el-table-column prop="uploadedBy" label="上传人" width="100" />
            <el-table-column prop="uploadedAt" label="上传时间" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="categoryDialogVisible" :title="categoryForm.id ? '编辑分类' : '分类管理'" width="720px">
      <div class="category-layout">
        <el-form :model="categoryForm" label-width="72px" class="category-form">
          <el-form-item label="名称"><el-input v-model="categoryForm.name" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="categoryForm.sortOrder" :min="1" style="width: 100%" /></el-form-item>
          <el-button type="primary" @click="submitCategory">保存分类</el-button>
        </el-form>
        <el-table :data="categories" border height="320" empty-text="暂无分类">
          <el-table-column prop="name" label="分类名称" />
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button text type="primary" @click="openCategoryEdit(row)">编辑</el-button>
              <el-button text type="danger" @click="removeCategory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </section>
</template>
