<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { apiDelete, apiGet } from '@/api/client'

const router = useRouter()
const rows = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('ALL')

const filteredRows = computed(() => rows.value.filter((row) => {
  const matchesKeyword = !keyword.value || [row.title, row.slug, row.description]
    .some((value) => String(value ?? '').toLowerCase().includes(keyword.value.trim().toLowerCase()))
  const matchesStatus = statusFilter.value === 'ALL' || row.status === statusFilter.value
  return matchesKeyword && matchesStatus
}))

async function load() {
  loading.value = true
  try {
    const page = await apiGet<any>('/admin/posts', { page: 1, size: 100 })
    rows.value = page.records ?? []
  } finally {
    loading.value = false
  }
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确认删除这篇文章？此操作无法撤销。', '删除文章', {
      confirmButtonText: '删除文章',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await apiDelete(`/admin/posts/${id}`)
    ElMessage.success('文章已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败，请稍后重试')
  }
}

onMounted(load)
</script>

<template>
  <div class="module-page">
    <div class="module-toolbar">
      <div class="toolbar-filters">
        <el-input v-model="keyword" class="post-search" clearable placeholder="搜索文章标题或 Slug" :prefix-icon="Search" />
        <el-select v-model="statusFilter" class="post-status-filter" aria-label="文章状态筛选">
          <el-option label="全部状态" value="ALL" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="草稿" value="DRAFT" />
        </el-select>
      </div>
      <div class="toolbar-actions">
        <el-button class="refresh-button" :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="router.push('/posts/edit')">新建文章</el-button>
      </div>
    </div>

    <el-card class="module-card" shadow="never">
      <div class="table-card-heading">
        <div class="table-title-block">
          <span class="table-title-icon"><el-icon><Document /></el-icon></span>
          <div><strong>全部文章</strong><span>{{ filteredRows.length }} 条记录</span></div>
        </div>
        <el-tag effect="plain" round>{{ statusFilter === 'ALL' ? '全部内容' : statusFilter === 'PUBLISHED' ? '已发布' : '草稿' }}</el-tag>
      </div>

      <el-table v-loading="loading" :data="filteredRows" row-key="id">
        <el-table-column label="文章" min-width="210">
          <template #default="{ row }">
            <div class="post-title-cell">
              <strong>{{ row.title || '未命名文章' }}</strong>
              <span>{{ row.slug ? '/' + row.slug : '尚未设置 Slug' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" class-name="post-status-column" label-class-name="post-status-column">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="light" round>
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="160" class-name="post-date-column" label-class-name="post-date-column">
          <template #default="{ row }">{{ row.publishedAt || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push('/posts/edit/' + row.id)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有找到文章" :image-size="82" /></template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.module-page { display: flex; flex-direction: column; gap: 15px; }
.module-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.toolbar-filters, .toolbar-actions { display: flex; align-items: center; gap: 9px; }
.post-search { width: min(340px, 38vw); }
.post-status-filter { width: 142px; }
.refresh-button { color: #56667b; }
.module-card { border: 1px solid #e7ecf3; }
.module-card :deep(.el-card__body) { padding: 0 21px 18px; }
.table-card-heading { min-height: 72px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.table-title-block { display: flex; align-items: center; gap: 11px; }
.table-title-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #4c78b1; font-size: 16px; border-radius: 10px; background: #eff5fc; }
.table-title-block div { display: flex; flex-direction: column; gap: 3px; }
.table-title-block strong { color: #29394e; font-size: 13px; }
.table-title-block span:not(.table-title-icon) { color: #96a1af; font-size: 11px; }
.post-title-cell { display: flex; flex-direction: column; gap: 5px; }
.post-title-cell strong { color: #2b3b51; font-size: 13px; font-weight: 650; }
.post-title-cell span { color: #9aa5b2; font-size: 11px; }
@media (max-width: 1050px) {
  .module-card :deep(.post-date-column) { display: none; }
  .module-toolbar { align-items: stretch; flex-direction: column; }
  .toolbar-filters { width: 100%; }
  .post-search { flex: 1; width: auto; }
  .toolbar-actions { justify-content: flex-end; }
}
@media (max-width: 700px) {
  .module-card :deep(.post-status-column) { display: none; }
}
@media (max-width: 480px) {
  .module-card :deep(.el-card__body) { padding: 0 12px 12px; }
  .post-status-filter { display: none; }
  .toolbar-actions .refresh-button { display: none; }
}
</style>



