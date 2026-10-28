<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Link, Plus } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '@/api/client'

const rows = ref<any[]>([])
const saving = ref(false)
const form = ref({ name: '', blogName: '', url: '', description: '', color: '#0078e7', visible: true })

async function load() {
  rows.value = await apiGet('/admin/links')
}

async function save() {
  if (!form.value.name.trim() || !form.value.url.trim()) {
    ElMessage.warning('请至少填写昵称和链接地址')
    return
  }
  saving.value = true
  try {
    await apiPost('/admin/links', form.value)
    ElMessage.success('友链已添加')
    form.value = { name: '', blogName: '', url: '', description: '', color: '#0078e7', visible: true }
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="management-grid">
    <el-card class="module-card create-card" shadow="never">
      <div class="form-card-heading">
        <span class="form-icon"><el-icon><Plus /></el-icon></span>
        <div><h3>添加友链</h3><p>添加你喜欢的站点与作者。</p></div>
      </div>
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="昵称"><el-input v-model="form.name" placeholder="站点作者" /></el-form-item>
        <el-form-item label="站点名称"><el-input v-model="form.blogName" placeholder="博客名称" /></el-form-item>
        <el-form-item label="站点链接"><el-input v-model="form.url" placeholder="https://" /></el-form-item>
        <el-form-item label="一句介绍"><el-input v-model="form.description" placeholder="可选" /></el-form-item>
        <el-form-item label="在站点展示"><el-switch v-model="form.visible" /></el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存友链</el-button>
      </el-form>
    </el-card>

    <el-card class="module-card list-card" shadow="never">
      <div class="table-card-heading">
        <div class="table-title-block"><span class="table-title-icon"><el-icon><Link /></el-icon></span><div><strong>友情链接</strong><span>{{ rows.length }} 个站点</span></div></div>
      </div>
      <el-table :data="rows" row-key="id">
        <el-table-column label="站点" min-width="210">
          <template #default="{ row }"><div class="link-title-cell"><strong>{{ row.blogName || row.name }}</strong><span>{{ row.name }}</span></div></template>
        </el-table-column>
        <el-table-column prop="url" label="链接地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="展示" width="100"><template #default="{ row }"><el-tag :type="row.visible === false ? 'info' : 'success'" effect="light" round>{{ row.visible === false ? '隐藏' : '展示中' }}</el-tag></template></el-table-column>
        <template #empty><el-empty description="还没有友情链接" :image-size="76" /></template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.management-grid { display: grid; grid-template-columns: minmax(250px, 0.75fr) minmax(0, 1.65fr); align-items: start; gap: 16px; }
.module-card { border: 1px solid #e7ecf3; }
.create-card :deep(.el-card__body) { padding: 22px; }
.list-card :deep(.el-card__body) { padding: 0 20px 16px; }
.form-card-heading { display: flex; align-items: center; gap: 11px; margin-bottom: 20px; }
.form-icon { width: 36px; height: 36px; display: grid; place-items: center; color: #3f7bd0; font-size: 16px; border-radius: 11px; background: #edf4fe; }
.form-card-heading h3 { margin: 0; color: #29394e; font-size: 14px; }
.form-card-heading p { margin: 5px 0 0; color: #8b97a7; font-size: 11px; }
.table-card-heading { min-height: 70px; display: flex; align-items: center; justify-content: space-between; }
.table-title-block { display: flex; align-items: center; gap: 11px; }
.table-title-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #4c78b1; font-size: 16px; border-radius: 10px; background: #eff5fc; }
.table-title-block div { display: flex; flex-direction: column; gap: 3px; }
.table-title-block strong { color: #29394e; font-size: 13px; }
.table-title-block div span { color: #96a1af; font-size: 11px; }
.link-title-cell { display: flex; flex-direction: column; gap: 4px; }
.link-title-cell strong { color: #2b3b51; font-size: 12px; }
.link-title-cell span { color: #95a0af; font-size: 11px; }
@media (max-width: 900px) { .management-grid { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .list-card :deep(.el-card__body) { padding: 0 12px 12px; } }
</style>
