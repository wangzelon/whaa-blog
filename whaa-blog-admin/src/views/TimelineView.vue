<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Plus } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '@/api/client'

const rows = ref<any[]>([])
const saving = ref(false)
const form = ref({ eventDate: '', title: '', content: '' })

async function load() {
  rows.value = await apiGet('/admin/timeline')
}

async function save() {
  if (!form.value.eventDate || !form.value.title.trim()) {
    ElMessage.warning('请填写日期和事件标题')
    return
  }
  saving.value = true
  try {
    await apiPost('/admin/timeline', form.value)
    ElMessage.success('时间节点已添加')
    form.value = { eventDate: '', title: '', content: '' }
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
        <div><h3>记录时间节点</h3><p>留存站点与创作过程中的重要时刻。</p></div>
      </div>
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="日期"><el-date-picker v-model="form.eventDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" /></el-form-item>
        <el-form-item label="事件标题"><el-input v-model="form.title" placeholder="例如：站点正式上线" /></el-form-item>
        <el-form-item label="事件描述"><el-input v-model="form.content" type="textarea" :rows="4" resize="vertical" /></el-form-item>
        <el-button type="primary" :loading="saving" @click="save">添加时间节点</el-button>
      </el-form>
    </el-card>

    <el-card class="module-card list-card" shadow="never">
      <div class="table-card-heading">
        <div class="table-title-block"><span class="table-title-icon"><el-icon><Clock /></el-icon></span><div><strong>站点历程</strong><span>{{ rows.length }} 个时间节点</span></div></div>
      </div>
      <el-table :data="rows" row-key="id">
        <el-table-column prop="eventDate" label="日期" width="150" />
        <el-table-column prop="title" label="事件" min-width="200" />
        <el-table-column prop="content" label="描述" min-width="230" show-overflow-tooltip />
        <template #empty><el-empty description="还没有编年史记录" :image-size="76" /></template>
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
@media (max-width: 640px) { .list-card :deep(.el-card__body) { padding: 0 12px 12px; } }
</style>
