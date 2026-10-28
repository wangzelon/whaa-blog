<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Refresh } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '@/api/client'

const rows = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await apiGet('/admin/comments/pending')
  } finally {
    loading.value = false
  }
}

async function approve(id: number) {
  try {
    await apiPost(`/admin/comments/${id}/status?status=APPROVED`)
    ElMessage.success('评论已通过审核')
    await load()
  } catch {
    ElMessage.error('审核失败，请稍后重试')
  }
}

onMounted(load)
</script>

<template>
  <el-card class="module-card comment-card" shadow="never">
    <div class="table-card-heading">
      <div class="table-title-block">
        <span class="table-title-icon"><el-icon><ChatDotRound /></el-icon></span>
        <div><strong>待审核评论</strong><span>核对内容后通过评论</span></div>
      </div>
      <el-button class="refresh-button" :icon="Refresh" @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" row-key="id">
      <el-table-column prop="nickname" label="访客" width="150">
        <template #default="{ row }"><span class="comment-author">{{ row.nickname || '匿名访客' }}</span></template>
      </el-table-column>
      <el-table-column prop="content" label="评论内容" min-width="320">
        <template #default="{ row }"><div class="comment-content">{{ row.content }}</div></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="190" />
      <el-table-column label="操作" width="120" align="right">
        <template #default="{ row }"><el-button type="primary" link @click="approve(row.id)">通过审核</el-button></template>
      </el-table-column>
      <template #empty><el-empty description="目前没有待审核评论" :image-size="82" /></template>
    </el-table>
  </el-card>
</template>

<style scoped>
.module-card { border: 1px solid #e7ecf3; }
.module-card :deep(.el-card__body) { padding: 0 21px 18px; }
.table-card-heading { min-height: 75px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.table-title-block { display: flex; align-items: center; gap: 11px; }
.table-title-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #4c78b1; font-size: 16px; border-radius: 10px; background: #eff5fc; }
.table-title-block div { display: flex; flex-direction: column; gap: 3px; }
.table-title-block strong { color: #29394e; font-size: 13px; }
.table-title-block div span { color: #96a1af; font-size: 11px; }
.comment-author { color: #31445c; font-weight: 650; }
.comment-content { max-width: 720px; overflow: hidden; color: #58687d; line-height: 1.65; text-overflow: ellipsis; }
.refresh-button { color: #56667b; }
@media (max-width: 640px) { .module-card :deep(.el-card__body) { padding: 0 12px 12px; } }
</style>
