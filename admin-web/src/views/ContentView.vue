<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  deleteData,
  getData,
  postData,
  putData,
  type FileRecord,
  type Product,
  type ProjectCase,
  type Solution
} from '../api';
import MultiImageUpload from '../components/MultiImageUpload.vue';
import RichTextEditor from '../components/RichTextEditor.vue';

type ContentMode = 'solution' | 'case';

interface ContentForm {
  id?: number;
  title: string;
  name: string;
  industry: string;
  location: string;
  summary: string;
  architecture: string;
  recommendedProductIds: number[];
  carouselImages: string[];
  detailHtml: string;
  attachments: FileRecord[];
  uploadedBy: string;
}

const activeTab = ref<ContentMode>('solution');
const keyword = ref('');
const loading = ref(false);
const drawerVisible = ref(false);
const products = ref<Product[]>([]);
const solutions = ref<Solution[]>([]);
const cases = ref<ProjectCase[]>([]);
const form = reactive<ContentForm>(emptyForm());

const filteredSolutions = computed(() =>
  solutions.value.filter((item) => !keyword.value || [item.title, item.industry, item.summary].join(' ').includes(keyword.value))
);

const filteredCases = computed(() =>
  cases.value.filter((item) => !keyword.value || [item.name, item.industry, item.location, item.summary].join(' ').includes(keyword.value))
);

function emptyForm(): ContentForm {
  return {
    title: '',
    name: '',
    industry: activeTab.value === 'solution' ? '边坡监测' : '智慧矿山',
    location: '',
    summary: '',
    architecture: '',
    recommendedProductIds: [],
    carouselImages: [],
    detailHtml: '<h2>图文详情</h2><p>请编辑小程序详情页展示的正文、图片、表格和项目说明。</p>',
    attachments: [],
    uploadedBy: '管理员'
  };
}

function resetForm(next: ContentForm = emptyForm()) {
  Object.assign(form, next, {
    recommendedProductIds: [...next.recommendedProductIds],
    carouselImages: [...next.carouselImages],
    attachments: [...next.attachments]
  });
}

async function refresh() {
  loading.value = true;
  try {
    const [nextSolutions, nextCases, nextProducts] = await Promise.all([
      getData<Solution[]>('/admin/solutions'),
      getData<ProjectCase[]>('/admin/cases'),
      getData<Product[]>('/admin/products')
    ]);
    solutions.value = nextSolutions;
    cases.value = nextCases;
    products.value = nextProducts;
  } finally {
    loading.value = false;
  }
}

function openCreate(mode: ContentMode) {
  activeTab.value = mode;
  resetForm();
  drawerVisible.value = true;
}

function openSolution(row: Solution) {
  activeTab.value = 'solution';
  resetForm({
    id: row.id,
    title: row.title,
    name: '',
    industry: row.industry,
    location: '',
    summary: row.summary,
    architecture: row.architecture,
    recommendedProductIds: row.recommendedProductIds || [],
    carouselImages: row.carouselImages?.length ? row.carouselImages : [row.imageUrl].filter(Boolean),
    detailHtml: row.detailHtml || '',
    attachments: row.attachments || [],
    uploadedBy: row.uploadedBy || '管理员'
  });
  drawerVisible.value = true;
}

function openCase(row: ProjectCase) {
  activeTab.value = 'case';
  resetForm({
    id: row.id,
    title: '',
    name: row.name,
    industry: row.industry,
    location: row.location,
    summary: row.summary,
    architecture: '',
    recommendedProductIds: [],
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
  if (activeTab.value === 'solution') {
    const payload = {
      title: form.title,
      industry: form.industry,
      summary: form.summary,
      architecture: form.architecture,
      imageUrl: form.carouselImages[0] || '',
      recommendedProductIds: form.recommendedProductIds,
      carouselImages: form.carouselImages,
      detailHtml: form.detailHtml,
      attachments: form.attachments,
      uploadedBy: form.uploadedBy
    };
    form.id ? await putData(`/admin/solutions/${form.id}`, payload) : await postData('/admin/solutions', payload);
    ElMessage.success('解决方案已保存');
  } else {
    const payload = {
      name: form.name,
      location: form.location,
      industry: form.industry,
      summary: form.summary,
      imageUrl: form.carouselImages[0] || '',
      carouselImages: form.carouselImages,
      detailHtml: form.detailHtml,
      attachments: form.attachments,
      uploadedBy: form.uploadedBy
    };
    form.id ? await putData(`/admin/cases/${form.id}`, payload) : await postData('/admin/cases', payload);
    ElMessage.success('典型案例已保存');
  }
  drawerVisible.value = false;
  await refresh();
}

async function removeSolution(row: Solution) {
  await ElMessageBox.confirm(`确认删除方案“${row.title}”？`, '删除确认', { type: 'warning' });
  await deleteData(`/admin/solutions/${row.id}`);
  ElMessage.success('方案已删除');
  await refresh();
}

async function removeCase(row: ProjectCase) {
  await ElMessageBox.confirm(`确认删除案例“${row.name}”？`, '删除确认', { type: 'warning' });
  await deleteData(`/admin/cases/${row.id}`);
  ElMessage.success('案例已删除');
  await refresh();
}

onMounted(refresh);
</script>

<template>
  <section>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索标题、行业、简介" clearable style="width: 320px" />
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="解决方案" name="solution">
        <div class="toolbar">
          <span class="muted">管理小程序方案中心的轮播图、图文详情和关联产品。</span>
          <el-button type="primary" @click="openCreate('solution')">新增方案</el-button>
        </div>
        <div v-loading="loading" class="content-grid">
          <article v-for="item in filteredSolutions" :key="item.id" class="media-card">
            <img :src="item.carouselImages?.[0] || item.imageUrl" :alt="item.title" />
            <div class="body">
              <el-tag size="small">{{ item.industry }}</el-tag>
              <h3>{{ item.title }}</h3>
              <p class="muted">{{ item.summary }}</p>
              <p class="muted small-line">上传人：{{ item.uploadedBy || '管理员' }}</p>
              <div class="card-actions">
                <el-button text type="primary" @click="openSolution(item)">编辑</el-button>
                <el-button text type="danger" @click="removeSolution(item)">删除</el-button>
              </div>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="典型案例" name="case">
        <div class="toolbar">
          <span class="muted">管理小程序案例中心的现场图片、项目过程和交付成果。</span>
          <el-button type="primary" @click="openCreate('case')">新增案例</el-button>
        </div>
        <div v-loading="loading" class="content-grid">
          <article v-for="item in filteredCases" :key="item.id" class="media-card">
            <img :src="item.carouselImages?.[0] || item.imageUrl" :alt="item.name" />
            <div class="body">
              <el-tag size="small" type="success">{{ item.location }}</el-tag>
              <h3>{{ item.name }}</h3>
              <p class="muted">{{ item.summary }}</p>
              <p class="muted small-line">上传人：{{ item.uploadedBy || '管理员' }}</p>
              <div class="card-actions">
                <el-button text type="primary" @click="openCase(item)">编辑</el-button>
                <el-button text type="danger" @click="removeCase(item)">删除</el-button>
              </div>
            </div>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="drawerVisible" :title="activeTab === 'solution' ? '编辑解决方案' : '编辑典型案例'" size="76%">
      <el-form :model="form" label-width="108px" class="editor-form">
        <el-form-item label="展示轮播图">
          <MultiImageUpload
            v-model="form.carouselImages"
            :module="activeTab === 'solution' ? 'solution-carousel' : 'case-carousel'"
            :uploaded-by="form.uploadedBy"
            @uploaded="trackUpload"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item v-if="activeTab === 'solution'" label="方案标题"><el-input v-model="form.title" /></el-form-item>
            <el-form-item v-else label="案例名称"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上传人"><el-input v-model="form.uploadedBy" /></el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="行业"><el-input v-model="form.industry" /></el-form-item>
          </el-col>
          <el-col v-if="activeTab === 'case'" :span="12">
            <el-form-item label="地点"><el-input v-model="form.location" /></el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="简介"><el-input v-model="form.summary" type="textarea" :rows="4" /></el-form-item>
        <el-form-item v-if="activeTab === 'solution'" label="架构说明">
          <el-input v-model="form.architecture" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item v-if="activeTab === 'solution'" label="关联产品">
          <el-select v-model="form.recommendedProductIds" multiple filterable style="width: 100%">
            <el-option v-for="item in products" :key="item.id" :label="`${item.name} / ${item.model}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="图文详情">
          <RichTextEditor
            v-model="form.detailHtml"
            :module="activeTab === 'solution' ? 'solution-editor' : 'case-editor'"
            :uploaded-by="form.uploadedBy"
            @uploaded="trackUpload"
          />
        </el-form-item>
        <el-form-item label="上传记录">
          <el-table :data="form.attachments" size="small" border class="nested-table">
            <el-table-column prop="originalName" label="文件名" min-width="180" />
            <el-table-column prop="module" label="模块" width="150" />
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
  </section>
</template>
