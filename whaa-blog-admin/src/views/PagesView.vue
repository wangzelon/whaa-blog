<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Plus } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '@/api/client'

const rows = ref<any[]>([])
const dialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const form = ref<any>({})

async function load() {
  loading.value = true
  try {
    rows.value = await apiGet('/admin/pages')
  } finally {
    loading.value = false
  }
}

function edit(row?: any) {
  form.value = row ? { ...row } : { slug: '', title: '', pageType: 'CUSTOM', contentMd: '', allowComment: true }
  dialog.value = true
}

async function save() {
  saving.value = true
  try {
    await apiPost('/admin/pages', form.value)
    ElMessage.success('页面已保存')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-card class="module-card page-list-card" shadow="never">
    <div class="module-toolbar">
      <div class="table-title-block">
        <span class="table-title-icon"><el-icon><Document /></el-icon></span>
        <div><strong>全部页面</strong><span>{{ rows.length }} 个页面</span></div>
      </div>
      <el-button type="primary" :icon="Plus" @click="edit()">新建页面</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" row-key="id">
      <el-table-column label="页面" min-width="240">
        <template #default="{ row }"><div class="page-title-cell"><strong>{{ row.title }}</strong><span>/{{ row.slug }}</span></div></template>
      </el-table-column>
      <el-table-column prop="pageType" label="类型" width="160">
        <template #default="{ row }"><el-tag effect="plain" round>{{ row.pageType === 'CUSTOM' ? '自定义页面' : row.pageType }}</el-tag></template>
      </el-table-column>
      <el-table-column label="评论" width="120">
        <template #default="{ row }">{{ row.allowComment ? '开放' : '关闭' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="right">
        <template #default="{ row }"><el-button link type="primary" @click="edit(row)">编辑</el-button></template>
      </el-table-column>
      <template #empty><el-empty description="还没有自定义页面" :image-size="82" /></template>
    </el-table>
  </el-card>

  <el-dialog v-model="dialog" title="编辑页面" width="720px" destroy-on-close>
    <el-form label-position="top" class="page-editor-form">
      <div class="page-form-grid">
        <el-form-item label="页面标题"><el-input v-model="form.title" placeholder="请输入页面标题" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="page-slug" /></el-form-item>
      </div>
      <el-form-item label="页面类型"><el-input v-model="form.pageType" /></el-form-item>
      <el-form-item label="Markdown 内容"><el-input v-model="form.contentMd" type="textarea" :rows="12" resize="vertical" /></el-form-item>
      <el-form-item label="允许评论"><el-switch v-model="form.allowComment" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存页面</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.module-card { border: 1px solid #e7ecf3; }
.module-card :deep(.el-card__body) { padding: 0 21px 18px; }
.module-toolbar { min-height: 76px; display: flex; align-items: center; justify-content: space-between; gap: 15px; }
.table-title-block { display: flex; align-items: center; gap: 11px; }
.table-title-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #4c78b1; font-size: 16px; border-radius: 10px; background: #eff5fc; }
.table-title-block div { display: flex; flex-direction: column; gap: 3px; }
.table-title-block strong { color: #29394e; font-size: 13px; }
.table-title-block div span { color: #96a1af; font-size: 11px; }
.page-title-cell { display: flex; flex-direction: column; gap: 4px; }
.page-title-cell strong { color: #2b3b51; font-size: 13px; font-weight: 650; }
.page-title-cell span { color: #9aa5b2; font-size: 11px; }
.page-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.page-editor-form :deep(.el-form-item) { margin-bottom: 18px; }
@media (max-width: 640px) {
  .module-card :deep(.el-card__body) { padding: 0 12px 12px; }
  .module-toolbar { min-height: 68px; }
  .page-form-grid { grid-template-columns: 1fr; gap: 0; }
}
</style>
