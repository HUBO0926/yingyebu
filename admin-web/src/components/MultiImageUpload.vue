<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { Close, Plus } from '@element-plus/icons-vue';
import { uploadFile, type FileRecord } from '../api';

const props = withDefaults(defineProps<{
  modelValue: string[];
  module?: string;
  uploadedBy?: string;
  limit?: number;
}>(), {
  module: 'carousel',
  uploadedBy: '管理员',
  limit: 8
});

const emit = defineEmits<{
  'update:modelValue': [value: string[]];
  uploaded: [record: FileRecord];
}>();

function update(next: string[]) {
  emit('update:modelValue', next);
}

async function handleUpload(options: { file: File; onSuccess?: (response: unknown) => void; onError?: (error: Error) => void }) {
  try {
    if ((props.modelValue || []).length >= props.limit) {
      throw new Error(`最多上传 ${props.limit} 张轮播图`);
    }
    const record = await uploadFile(options.file, props.module, props.uploadedBy);
    update([...(props.modelValue || []), record.url]);
    emit('uploaded', record);
    options.onSuccess?.(record);
  } catch (error) {
    const message = error instanceof Error ? error.message : '上传失败';
    ElMessage.error(message);
    options.onError?.(error instanceof Error ? error : new Error(message));
  }
}

function removeImage(index: number) {
  update((props.modelValue || []).filter((_, currentIndex) => currentIndex !== index));
}
</script>

<template>
  <div class="multi-image-upload">
    <div v-for="(url, index) in modelValue" :key="`${url}-${index}`" class="image-tile">
      <el-image :src="url" fit="cover" />
      <button type="button" class="image-remove" title="移除图片" @click="removeImage(index)">
        <el-icon><Close /></el-icon>
      </button>
    </div>

    <el-upload
      v-if="(modelValue || []).length < limit"
      :http-request="handleUpload"
      :show-file-list="false"
      accept="image/*"
      class="image-uploader"
    >
      <div class="upload-trigger">
        <el-icon><Plus /></el-icon>
        <span>上传轮播图</span>
      </div>
    </el-upload>
  </div>
</template>

<style scoped>
.multi-image-upload {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
  gap: 12px;
  width: 100%;
}

.image-tile,
.upload-trigger {
  position: relative;
  overflow: hidden;
  width: 100%;
  aspect-ratio: 16 / 10;
  border: 1px solid #d9e1ee;
  border-radius: 8px;
  background: #f7f9fc;
}

.image-tile :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.image-remove {
  position: absolute;
  top: 6px;
  right: 6px;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 50%;
  color: #ffffff;
  background: rgba(0, 0, 0, 0.52);
  cursor: pointer;
}

.upload-trigger {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: #4c5f7a;
  cursor: pointer;
}
</style>
