<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { FolderOpened, Plus } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '@/api/client'

const rows = ref<any[]>([])
const saving = ref(false)
const form = ref({ title: '', description: '', url: '', categoryCode: 'tool', visible: true })

async function load() {
  rows.value = await apiGet('/admin/projects')
}

async function save() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写项目标题')
    return
  }
  saving.value = true
  try {
    await apiPost('/admin/projects', form.value)
    ElMessage.success('项目已添加')
    form.value = { title: '', description: '', url: '', categoryCode: 'tool', visible: true }
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
        <div><h3>添加项目</h3><p>展示你正在制作或已经完成的作品。</p></div>
      </div>
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="项目标题"><el-input v-model="form.title" placeholder="项目名称" /></el-form-item>
        <el-form-item label="项目简介"><el-input v-model="form.description" type="textarea" :rows="3" resize="vertical" /></el-form-item>
        <el-form-item label="项目链接"><el-input v-model="form.url" placeholder="https://" /></el-form-item>
        <div class="project-form-row">
          <el-form-item label="分类代码"><el-input v-model="form.categoryCode" /></el-form-item>
          <el-form-item label="在站点展示"><el-switch v-model="form.visible" /></el-form-item>
        </div>
        <el-button type="primary" :loading="saving" @click="save">保存项目</el-button>
      </el-form>
    </el-card>

    <el-card class="module-card list-card" shadow="never">
      <div class="table-card-heading">
        <div class="table-title-block"><span class="table-title-icon"><el-icon><FolderOpened /></el-icon></span><div><strong>项目列表</strong><span>{{ rows.length }} 个项目</span></div></div>
      </div>
      <el-table :data="rows" row-key="id">
        <el-table-column prop="title" label="项目" min-width="180" />
        <el-table-column prop="categoryCode" label="分类" width="140" />
        <el-table-column prop="url" label="链接" min-width="190" show-overflow-tooltip />
        <el-table-column label="展示" width="100"><template #default="{ row }"><el-tag :type="row.visible === false ? 'info' : 'success'" effect="light" round>{{ row.visible === false ? '隐藏' : '展示中' }}</el-tag></template></el-table-column>
        <template #empty><el-empty description="还没有展示项目" :image-size="76" /></template>
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
.project-form-row { display: grid; grid-template-columns: 1fr 1fr; align-items: center; gap: 14px; }
.project-form-row :deep(.el-form-item:last-child) { padding-top: 8px; }
.table-card-heading { min-height: 70px; display: flex; align-items: center; justify-content: space-between; }
.table-title-block { display: flex; align-items: center; gap: 11px; }
.table-title-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #4c78b1; font-size: 16px; border-radius: 10px; background: #eff5fc; }
.table-title-block div { display: flex; flex-direction: column; gap: 3px; }
.table-title-block strong { color: #29394e; font-size: 13px; }
.table-title-block div span { color: #96a1af; font-size: 11px; }
@media (max-width: 900px) { .management-grid { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .list-card :deep(.el-card__body) { padding: 0 12px 12px; } }
</style>
