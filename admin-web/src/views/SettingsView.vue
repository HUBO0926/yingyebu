<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  deleteData,
  getData,
  postData,
  putData,
  type AiConfigTestResult,
  type AiModelConfig,
  type AiModelConfigPayload,
  type Personnel,
  type QuoteTemplate
} from '../api';

interface ProviderPreset {
  provider: string;
  providerCode: string;
  adapterType: string;
  baseUrl: string;
  chatModel: string;
  embeddingModel: string;
  remark: string;
}

const roleOptions = [
  { label: '普通客户', value: 'CUSTOMER' },
  { label: '销售', value: 'SALES' },
  { label: '技术工程师', value: 'ENGINEER' },
  { label: '管理员', value: 'ADMIN' }
];

const statusOptions = [
  { label: '启用', value: '启用' },
  { label: '禁用', value: '禁用' }
];

const providerPresets: ProviderPreset[] = [
  {
    provider: 'DeepSeek',
    providerCode: 'DeepSeek',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://api.deepseek.com',
    chatModel: 'deepseek-v3',
    embeddingModel: 'BAAI/bge-m3',
    remark: '适合国内网络环境下做通用问答，按控制台实际可用模型调整。'
  },
  {
    provider: 'Gemini',
    providerCode: 'Gemini',
    adapterType: 'Native',
    baseUrl: 'https://generativelanguage.googleapis.com',
    chatModel: 'gemini-2.5-flash',
    embeddingModel: 'gemini-embedding-001',
    remark: '适合知识库问答和向量化，服务器需要能够访问 Google API。'
  },
  {
    provider: '阿里云百炼/Qwen',
    providerCode: 'DashScope',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    chatModel: 'qwen-plus',
    embeddingModel: 'text-embedding-v4',
    remark: '国内优先推荐，兼容 OpenAI 调用方式，适合通义千问系列。'
  },
  {
    provider: '百度千帆/ERNIE',
    providerCode: 'Qianfan',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://qianfan.baidubce.com/v2',
    chatModel: 'ernie-4.0-turbo-8k',
    embeddingModel: 'bge-large-zh',
    remark: '按千帆控制台实际开通模型填写模型 ID。'
  },
  {
    provider: '火山方舟/Doubao',
    providerCode: 'VolcArk',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
    chatModel: 'doubao-seed-1-6',
    embeddingModel: 'doubao-embedding',
    remark: '适合接入豆包和方舟平台上的第三方模型。'
  },
  {
    provider: '腾讯混元',
    providerCode: 'Hunyuan',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://api.hunyuan.cloud.tencent.com/v1',
    chatModel: 'hunyuan-turbo',
    embeddingModel: 'hunyuan-embedding',
    remark: '如账号接口不是兼容模式，后续使用 nativeAdapter 单独适配。'
  },
  {
    provider: '智谱 GLM',
    providerCode: 'Zhipu',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    chatModel: 'glm-4-flash',
    embeddingModel: 'embedding-3',
    remark: '适合 GLM 系列模型，支持按控制台模型 ID 调整。'
  },
  {
    provider: '月之暗面/Kimi',
    providerCode: 'Moonshot',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://api.moonshot.cn/v1',
    chatModel: 'moonshot-v1-8k',
    embeddingModel: '',
    remark: '适合长文本问答，embedding 可先使用其他供应商。'
  },
  {
    provider: 'MiniMax',
    providerCode: 'MiniMax',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://api.minimax.chat/v1',
    chatModel: 'MiniMax-Text-01',
    embeddingModel: '',
    remark: '按 MiniMax 控制台实际模型名称调整。'
  },
  {
    provider: '讯飞星火',
    providerCode: 'Spark',
    adapterType: 'Native',
    baseUrl: 'https://spark-api-open.xf-yun.com',
    chatModel: 'generalv3.5',
    embeddingModel: '',
    remark: '预留原生适配入口，后续按星火鉴权方式单独接入。'
  },
  {
    provider: '硅基流动',
    providerCode: 'SiliconFlow',
    adapterType: 'OpenAI Compatible',
    baseUrl: 'https://api.siliconflow.cn/v1',
    chatModel: 'Qwen/Qwen2.5-72B-Instruct',
    embeddingModel: 'BAAI/bge-m3',
    remark: '适合统一接入多个国产开源和闭源模型。'
  },
  {
    provider: 'OpenAI Compatible',
    providerCode: 'Custom',
    adapterType: 'OpenAI Compatible',
    baseUrl: '',
    chatModel: '',
    embeddingModel: '',
    remark: '自定义兼容接口，可填写代理或私有模型网关。'
  }
];

const templates = ref<QuoteTemplate[]>([]);
const personnel = ref<Personnel[]>([]);
const aiConfigs = ref<AiModelConfig[]>([]);
const loading = ref(false);
const templateDrawer = ref(false);
const personnelDrawer = ref(false);
const aiDrawer = ref(false);
const activeTab = ref('templates');

const templateForm = reactive({
  id: 0,
  templateName: '',
  description: '',
  measurementsText: '标准监测站,传感器,智能位移传感器,1,6800'
});

const personnelForm = reactive({
  id: 0,
  userId: '',
  openId: '',
  unionId: '',
  displayName: '',
  role: 'CUSTOMER' as Personnel['role'],
  phone: '',
  status: '启用',
  remark: '',
  firstLoginAt: '',
  lastLoginAt: ''
});

const aiForm = reactive<AiModelConfigPayload & { id: number }>({
  id: 0,
  provider: '阿里云百炼/Qwen',
  providerCode: 'DashScope',
  adapterType: 'OpenAI Compatible',
  baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  chatModel: 'qwen-plus',
  embeddingModel: 'text-embedding-v4',
  apiKey: '',
  enabled: false,
  remark: '国内优先推荐，兼容 OpenAI 调用方式，适合通义千问系列。'
});

const enabledConfig = computed(() => aiConfigs.value.find((item) => item.enabled));

async function refresh() {
  loading.value = true;
  try {
    const [nextTemplates, nextPersonnel, nextAiConfigs] = await Promise.all([
      getData<QuoteTemplate[]>('/admin/templates'),
      getData<Personnel[]>('/admin/personnel'),
      getData<AiModelConfig[]>('/admin/ai-configs')
    ]);
    templates.value = nextTemplates;
    personnel.value = nextPersonnel;
    aiConfigs.value = nextAiConfigs;
  } finally {
    loading.value = false;
  }
}

function roleLabel(role: string) {
  return roleOptions.find((item) => item.value === role)?.label || role;
}

function roleTagType(role: string) {
  if (role === 'ADMIN') return 'danger';
  if (role === 'ENGINEER') return 'warning';
  if (role === 'SALES') return 'success';
  return 'info';
}

function measurementText(template: QuoteTemplate) {
  return template.measurements
    .map((item) => [item.name, item.categoryName, item.productName, item.quantity, item.unitPrice].join(','))
    .join('\n');
}

function parseMeasurements(text: string) {
  return text.split('\n').map((line) => line.trim()).filter(Boolean).map((line) => {
    const [name, categoryName, productName, quantity, unitPrice] = line.split(',');
    return {
      name: name || '标准监测站',
      categoryName: categoryName || '传感器',
      productName: productName || '智能位移传感器',
      quantity: Number(quantity || 1),
      unitPrice: Number(unitPrice || 0)
    };
  });
}

function openTemplate(row?: QuoteTemplate) {
  Object.assign(templateForm, row ? {
    id: row.id,
    templateName: row.templateName,
    description: row.description,
    measurementsText: measurementText(row)
  } : {
    id: 0,
    templateName: '',
    description: '',
    measurementsText: '标准监测站,传感器,智能位移传感器,1,6800'
  });
  templateDrawer.value = true;
}

async function saveTemplate() {
  const payload = {
    templateName: templateForm.templateName,
    description: templateForm.description,
    measurements: parseMeasurements(templateForm.measurementsText)
  };
  templateForm.id ? await putData(`/admin/templates/${templateForm.id}`, payload) : await postData('/admin/templates', payload);
  ElMessage.success('模板已保存');
  templateDrawer.value = false;
  await refresh();
}

async function removeTemplate(row: QuoteTemplate) {
  await ElMessageBox.confirm(`确认删除模板“${row.templateName}”？`, '删除确认', { type: 'warning' });
  await deleteData(`/admin/templates/${row.id}`);
  ElMessage.success('模板已删除');
  await refresh();
}

function openPersonnel(row: Personnel) {
  Object.assign(personnelForm, row);
  personnelDrawer.value = true;
}

async function savePersonnel() {
  const payload = {
    displayName: personnelForm.displayName,
    phone: personnelForm.phone,
    role: personnelForm.role,
    status: personnelForm.status,
    remark: personnelForm.remark
  };
  await putData(`/admin/personnel/${personnelForm.id}`, payload);
  ElMessage.success('人员权限已更新');
  personnelDrawer.value = false;
  await refresh();
}

async function togglePersonnel(row: Personnel) {
  const nextAction = row.status === '禁用' ? 'enable' : 'disable';
  const nextText = row.status === '禁用' ? '启用' : '禁用';
  await ElMessageBox.confirm(`确认${nextText}“${row.displayName}”？`, `${nextText}人员`, { type: 'warning' });
  await postData(`/admin/personnel/${row.id}/${nextAction}`, {});
  ElMessage.success(`人员已${nextText}`);
  await refresh();
}

function applyProviderPreset(provider: string) {
  const preset = providerPresets.find((item) => item.provider === provider);
  if (!preset) return;
  Object.assign(aiForm, {
    provider: preset.provider,
    providerCode: preset.providerCode,
    adapterType: preset.adapterType,
    baseUrl: preset.baseUrl,
    chatModel: preset.chatModel,
    embeddingModel: preset.embeddingModel,
    remark: preset.remark
  });
}

function openAiConfig(row?: AiModelConfig) {
  if (row) {
    Object.assign(aiForm, {
      id: row.id,
      provider: row.provider,
      providerCode: row.providerCode,
      adapterType: row.adapterType,
      baseUrl: row.baseUrl,
      chatModel: row.chatModel,
      embeddingModel: row.embeddingModel,
      apiKey: '',
      enabled: row.enabled,
      remark: row.remark
    });
  } else {
    const preset = providerPresets[2];
    Object.assign(aiForm, {
      id: 0,
      ...preset,
      apiKey: '',
      enabled: false
    });
  }
  aiDrawer.value = true;
}

function aiPayload(): AiModelConfigPayload {
  return {
    provider: aiForm.provider,
    providerCode: aiForm.providerCode,
    adapterType: aiForm.adapterType,
    baseUrl: aiForm.baseUrl,
    chatModel: aiForm.chatModel,
    embeddingModel: aiForm.embeddingModel,
    apiKey: aiForm.apiKey,
    enabled: aiForm.enabled,
    remark: aiForm.remark
  };
}

async function saveAiConfig() {
  const payload = aiPayload();
  aiForm.id ? await putData(`/admin/ai-configs/${aiForm.id}`, payload) : await postData('/admin/ai-configs', payload);
  ElMessage.success('AI 模型配置已保存，API Key 将脱敏展示');
  aiDrawer.value = false;
  await refresh();
}

async function testAiConfig(row?: AiModelConfig) {
  const payload = row ? {
    id: row.id,
    provider: row.provider,
    providerCode: row.providerCode,
    adapterType: row.adapterType,
    baseUrl: row.baseUrl,
    chatModel: row.chatModel,
    embeddingModel: row.embeddingModel,
    apiKey: aiForm.id === row.id ? aiForm.apiKey : '',
    enabled: row.enabled,
    remark: row.remark
  } : aiPayload();
  const result = await postData<AiConfigTestResult>('/admin/ai-configs/test', payload);
  result.ok ? ElMessage.success(result.message) : ElMessage.warning(result.message);
}

async function enableAiConfig(row: AiModelConfig) {
  await postData(`/admin/ai-configs/${row.id}/enable`, {});
  ElMessage.success(`已启用 ${row.provider}`);
  await refresh();
}

async function clearAiKey(row: AiModelConfig) {
  await ElMessageBox.confirm(`确认清空“${row.provider}”的 API Key？`, '清空 Key', { type: 'warning' });
  await deleteData(`/admin/ai-configs/${row.id}/key`);
  ElMessage.success('API Key 已清空');
  await refresh();
}

async function copyBaseUrl(row: AiModelConfig) {
  await navigator.clipboard.writeText(row.baseUrl || '');
  ElMessage.success('Base URL 已复制');
}

onMounted(refresh);
</script>

<template>
  <section v-loading="loading">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="报价模板" name="templates">
        <div class="toolbar">
          <span class="muted">维护报价工具使用的默认模板和测项建议，后续用于生成正式 Word 报价书。</span>
          <el-button type="primary" @click="openTemplate()">新增模板</el-button>
        </div>
        <el-table :data="templates" border>
          <el-table-column prop="templateName" label="模板名称" min-width="180" />
          <el-table-column prop="description" label="说明" min-width="260" />
          <el-table-column label="默认测项" width="120">
            <template #default="{ row }">{{ row.measurements.length }} 项</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openTemplate(row)">编辑</el-button>
              <el-button text type="danger" @click="removeTemplate(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="人员权限管理" name="personnel">
        <div class="toolbar">
          <span class="muted">所有微信登录用户首次登录会自动建档，默认普通客户；内部角色只能由管理员在这里调整。</span>
        </div>
        <el-table :data="personnel" border>
          <el-table-column prop="displayName" label="人员名称" min-width="140" />
          <el-table-column prop="openId" label="OpenID" min-width="220" show-overflow-tooltip />
          <el-table-column prop="unionId" label="UnionID" min-width="180" show-overflow-tooltip />
          <el-table-column label="角色" width="130">
            <template #default="{ row }">
              <el-tag :type="roleTagType(row.role)">{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === '启用' ? 'success' : 'danger'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="firstLoginAt" label="首次登录" width="160" />
          <el-table-column prop="lastLoginAt" label="最近登录" width="160" />
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openPersonnel(row)">调整权限</el-button>
              <el-button text :type="row.status === '禁用' ? 'success' : 'danger'" @click="togglePersonnel(row)">
                {{ row.status === '禁用' ? '启用' : '禁用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="AI 模型配置" name="ai">
        <div class="toolbar">
          <div>
            <strong>当前启用：</strong>
            <el-tag v-if="enabledConfig" type="success">{{ enabledConfig.provider }} / {{ enabledConfig.chatModel }}</el-tag>
            <el-tag v-else type="info">未启用</el-tag>
          </div>
          <el-button type="primary" @click="openAiConfig()">新增模型配置</el-button>
        </div>
        <el-table :data="aiConfigs" border>
          <el-table-column label="启用" width="76">
            <template #default="{ row }">
              <el-tag v-if="row.enabled" type="success">当前</el-tag>
              <el-tag v-else type="info">备用</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="provider" label="厂商" min-width="150" />
          <el-table-column prop="adapterType" label="接口类型" width="170" />
          <el-table-column prop="baseUrl" label="Base URL" min-width="260" show-overflow-tooltip />
          <el-table-column prop="chatModel" label="对话模型" min-width="170" />
          <el-table-column prop="embeddingModel" label="向量模型" min-width="160" />
          <el-table-column label="Key" width="150">
            <template #default="{ row }">
              <span>{{ row.maskedApiKey || row.keyStatus }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="190" show-overflow-tooltip />
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openAiConfig(row)">编辑</el-button>
              <el-button text @click="testAiConfig(row)">测试</el-button>
              <el-button text @click="copyBaseUrl(row)">复制 URL</el-button>
              <el-button text type="success" :disabled="row.enabled" @click="enableAiConfig(row)">启用</el-button>
              <el-button text type="danger" @click="clearAiKey(row)">清空 Key</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="templateDrawer" :title="templateForm.id ? '编辑报价模板' : '新增报价模板'" size="560px">
      <el-form :model="templateForm" label-width="96px">
        <el-form-item label="模板名称"><el-input v-model="templateForm.templateName" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="templateForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="默认测项">
          <el-input v-model="templateForm.measurementsText" type="textarea" :rows="7" placeholder="每行格式：测项,类别,产品,数量,单价" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDrawer = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="personnelDrawer" title="调整人员权限" size="560px">
      <el-descriptions :column="1" border class="identity-box">
        <el-descriptions-item label="OpenID">{{ personnelForm.openId }}</el-descriptions-item>
        <el-descriptions-item label="UnionID">{{ personnelForm.unionId || '未获取' }}</el-descriptions-item>
        <el-descriptions-item label="系统用户ID">{{ personnelForm.userId }}</el-descriptions-item>
        <el-descriptions-item label="首次登录">{{ personnelForm.firstLoginAt }}</el-descriptions-item>
        <el-descriptions-item label="最近登录">{{ personnelForm.lastLoginAt }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="personnelForm" label-width="96px">
        <el-form-item label="人员名称"><el-input v-model="personnelForm.displayName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="personnelForm.phone" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="personnelForm.role" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="personnelForm.status" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="personnelForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="personnelDrawer = false">取消</el-button>
        <el-button type="primary" @click="savePersonnel">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="aiDrawer" :title="aiForm.id ? '编辑 AI 模型配置' : '新增 AI 模型配置'" size="680px">
      <el-form :model="aiForm" label-width="118px">
        <el-form-item label="厂商预设">
          <el-select v-model="aiForm.provider" filterable style="width: 100%" @change="applyProviderPreset">
            <el-option v-for="item in providerPresets" :key="item.provider" :label="item.provider" :value="item.provider" />
          </el-select>
        </el-form-item>
        <el-form-item label="Provider Code"><el-input v-model="aiForm.providerCode" /></el-form-item>
        <el-form-item label="接口类型">
          <el-select v-model="aiForm.adapterType" style="width: 100%">
            <el-option label="OpenAI Compatible" value="OpenAI Compatible" />
            <el-option label="Native" value="Native" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL"><el-input v-model="aiForm.baseUrl" /></el-form-item>
        <el-form-item label="对话模型"><el-input v-model="aiForm.chatModel" /></el-form-item>
        <el-form-item label="向量模型"><el-input v-model="aiForm.embeddingModel" placeholder="可为空，后续使用其他向量模型" /></el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="aiForm.apiKey" type="password" show-password placeholder="留空保存时保留旧 Key" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="aiForm.enabled" active-text="保存后设为可启用状态" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="aiForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDrawer = false">取消</el-button>
        <el-button @click="testAiConfig()">测试连接</el-button>
        <el-button type="primary" @click="saveAiConfig">保存</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.muted {
  color: #6b778c;
  font-size: 14px;
}

.identity-box {
  margin-bottom: 18px;
}
</style>
