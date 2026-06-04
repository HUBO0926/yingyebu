<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { UploadFilled } from '@element-plus/icons-vue';
import { getData, postData } from '../api';

interface AiDocument {
  id: number;
  title: string;
  sourceType: string;
  status: string;
  createdAt: string;
}

const documents = ref<AiDocument[]>([]);
const question = ref('边坡自动化监测方案需要哪些核心设备？');
const answer = ref('');
const loading = ref(false);

async function ask() {
  loading.value = true;
  try {
    const result = await postData<{ answer?: string }>('/ai/chat', { question: question.value });
    answer.value = result.answer || JSON.stringify(result, null, 2);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  documents.value = await getData<AiDocument[]>('/ai/documents');
});
</script>

<template>
  <el-row :gutter="16">
    <el-col :span="14">
      <el-card shadow="never">
        <template #header>知识库文档</template>
        <el-upload drag action="/api/files" multiple>
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽技术资料到这里，或点击上传</div>
        </el-upload>
        <el-table :data="documents" border style="margin-top: 16px" empty-text="暂无文档">
          <el-table-column prop="title" label="文档标题" min-width="220" />
          <el-table-column prop="sourceType" label="来源" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="createdAt" label="创建时间" width="180" />
        </el-table>
      </el-card>
    </el-col>
    <el-col :span="10">
      <el-card shadow="never">
        <template #header>小恒 AI 助手</template>
        <el-input v-model="question" type="textarea" :rows="4" />
        <el-button type="primary" :loading="loading" style="margin-top: 12px" @click="ask">发送问题</el-button>
        <el-alert v-if="answer" :title="answer" type="info" show-icon :closable="false" style="margin-top: 16px" />
      </el-card>
    </el-col>
  </el-row>
</template>
