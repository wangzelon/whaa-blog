<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, PriceTag } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '@/api/client'

const rows = ref<any[]>([])
const saving = ref(false)
const form = ref({ name: '', slug: '' })

async function load() {
  rows.value = await apiGet('/admin/tags')
}

async function save() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写标签名称')
    return
  }
  saving.value = true
  try {
    await apiPost('/admin/tags', form.value)
    ElMessage.success('标签已添加')
    form.value = { name: '', slug: '' }
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确认删除这个标签？', '删除标签', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await apiDelete(`/admin/tags/${id}`)
    ElMessage.success('标签已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败，请稍后重试')
  }
}

onMounted(load)
</script>

<template>
  <div class="management-grid">
    <el-card class="module-card create-card" shadow="never">
      <div class="form-card-heading">
        <span class="form-icon"><el-icon><Plus /></el-icon></span>
        <div><h3>新建标签</h3><p>用关键词连接相关内容。</p></div>
      </div>
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="标签名称"><el-input v-model="form.name" placeholder="例如：生活" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="例如：life" /></el-form-item>
        <el-button type="primary" :loading="saving" @click="save">添加标签</el-button>
      </el-form>
    </el-card>

    <el-card class="module-card list-card" shadow="never">
      <div class="table-card-heading">
        <div class="table-title-block"><span class="table-title-icon"><el-icon><PriceTag /></el-icon></span><div><strong>标签列表</strong><span>{{ rows.length }} 个标签</span></div></div>
      </div>
      <el-table :data="rows" row-key="id">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="slug" label="Slug" min-width="180" />
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }"><el-button link type="danger" @click="remove(row.id)">删除</el-button></template>
        </el-table-column>
        <template #empty><el-empty description="还没有标签" :image-size="76" /></template>
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
@media (max-width: 900px) { .management-grid { grid-template-columns: 1fr; } }
</style>
