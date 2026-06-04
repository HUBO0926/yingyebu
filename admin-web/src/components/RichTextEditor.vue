<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css';

import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import { uploadFile, type FileRecord } from '../api';

const props = withDefaults(defineProps<{
  modelValue: string;
  module?: string;
  uploadedBy?: string;
  height?: number;
  placeholder?: string;
}>(), {
  module: 'editor',
  uploadedBy: '管理员',
  height: 420,
  placeholder: '请输入图文详情，可插入图片、表格、链接和说明文字'
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  uploaded: [record: FileRecord];
}>();

const editorRef = shallowRef<any>(null);
const valueHtml = ref(props.modelValue || '');

const toolbarConfig = {};
const editorConfig = computed(() => {
  const config: any = {
    placeholder: props.placeholder,
    MENU_CONF: {}
  };
  config.MENU_CONF.uploadImage = {
    async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
      try {
        const record = await uploadFile(file, props.module, props.uploadedBy);
        insertFn(record.url, record.originalName, record.url);
        emit('uploaded', record);
      } catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '图片上传失败');
      }
    }
  };
  return config;
});

watch(() => props.modelValue, (next) => {
  if ((next || '') !== valueHtml.value) valueHtml.value = next || '';
});

watch(valueHtml, (next) => {
  emit('update:modelValue', next);
});

function handleCreated(editor: any) {
  editorRef.value = editor;
}

onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor) editor.destroy();
});
</script>

<template>
  <div class="rich-editor">
    <Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" class="rich-editor-toolbar" />
    <Editor
      v-model="valueHtml"
      :default-config="editorConfig"
      mode="default"
      :style="{ height: `${height}px`, overflowY: 'hidden' }"
      @on-created="handleCreated"
    />
  </div>
</template>

<style scoped>
.rich-editor {
  overflow: hidden;
  width: 100%;
  border: 1px solid #d9e1ee;
  border-radius: 8px;
  background: #ffffff;
}

.rich-editor-toolbar {
  border-bottom: 1px solid #edf1f7;
}
</style>
