<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import { apiGet, apiPut } from '@/api/client'

const keys = ['site', 'sidebar', 'social', 'theme', 'comment', 'links']
const labels: Record<string, string> = {
  site: '站点信息',
  sidebar: '侧边栏',
  social: '社交账号',
  theme: '主题外观',
  comment: '评论设置',
  links: '友链设置',
}
const active = ref('site')
const jsonText = ref('')
const loading = ref(false)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const all = await apiGet<Record<string, unknown>>('/admin/site/config')
    jsonText.value = JSON.stringify(all[active.value] ?? {}, null, 2)
  } finally {
    loading.value = false
  }
}

async function save() {
  let value: unknown
  try {
    value = JSON.parse(jsonText.value)
  } catch {
    ElMessage.error('JSON 格式无效，请检查括号、引号和逗号。')
    return
  }

  saving.value = true
  try {
    await apiPut(`/admin/site/config/${active.value}`, value)
    ElMessage.success('配置已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-card class="settings-card" shadow="never">
    <div class="settings-heading">
      <div>
        <h3>站点 JSON 配置</h3>
        <p>选择配置项后编辑 JSON，保存后会更新对应站点设置。</p>
      </div>
      <el-button class="refresh-button" :icon="Refresh" :loading="loading" @click="load">重新载入</el-button>
    </div>

    <el-tabs v-model="active" class="settings-tabs" @tab-change="load">
      <el-tab-pane v-for="key in keys" :key="key" :name="key" :label="labels[key]" />
    </el-tabs>

    <div v-loading="loading" class="json-editor-wrap">
      <div class="editor-toolbar"><span class="editor-file-mark">{ }</span><span>{{ active }}.json</span><span class="editor-format">JSON</span></div>
      <el-input v-model="jsonText" class="json-editor" type="textarea" :rows="21" resize="vertical" spellcheck="false" />
    </div>

    <div class="settings-footer">
      <span>保存前请确认 JSON 语法有效。</span>
      <el-button type="primary" :loading="saving" :icon="Check" @click="save">保存配置</el-button>
    </div>
  </el-card>
</template>

<style scoped>
.settings-card { border: 1px solid #e7ecf3; }
.settings-card :deep(.el-card__body) { padding: 22px 24px 20px; }
.settings-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.settings-heading h3 { margin: 0; color: #29394e; font-size: 15px; }
.settings-heading p { margin: 7px 0 0; color: #8793a3; font-size: 12px; line-height: 1.6; }
.refresh-button { color: #56667b; }
.settings-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.settings-tabs :deep(.el-tabs__item) { height: 40px; color: #768498; font-size: 12px; }
.settings-tabs :deep(.el-tabs__item.is-active) { color: #367ce0; font-weight: 650; }
.settings-tabs :deep(.el-tabs__active-bar) { height: 2px; border-radius: 2px; }
.settings-tabs :deep(.el-tabs__content) { display: none; }
.json-editor-wrap { overflow: hidden; border: 1px solid #e2e9f1; border-radius: 12px; background: #fbfcfe; }
.editor-toolbar { height: 40px; display: flex; align-items: center; gap: 9px; padding: 0 14px; color: #60718a; border-bottom: 1px solid #e7ecf2; background: #f6f8fb; font-size: 11px; }
.editor-file-mark { color: #4d83c5; font-weight: 700; }
.editor-format { margin-left: auto; color: #99a5b4; font-size: 10px; letter-spacing: 0.08em; }
.json-editor :deep(.el-textarea__inner) {
  min-height: 340px !important;
  padding: 18px 20px;
  color: #29394e;
  border: 0;
  border-radius: 0;
  background: #fbfcfe;
  box-shadow: none;
  font-family: Consolas, "SFMono-Regular", monospace;
  font-size: 12px;
  line-height: 1.75;
  tab-size: 2;
}
.json-editor :deep(.el-textarea__inner:focus) { box-shadow: none; }
.settings-footer { display: flex; align-items: center; justify-content: space-between; gap: 15px; padding-top: 17px; color: #8b97a7; font-size: 11px; }
@media (max-width: 640px) {
  .settings-card :deep(.el-card__body) { padding: 18px 15px; }
  .settings-heading { align-items: flex-start; flex-direction: column; }
  .json-editor :deep(.el-textarea__inner) { padding: 14px; }
  .settings-footer { align-items: flex-start; flex-direction: column; }
}
</style>

