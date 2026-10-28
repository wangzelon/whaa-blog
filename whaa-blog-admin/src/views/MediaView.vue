<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, UploadFilled } from '@element-plus/icons-vue'
import { apiGet } from '@/api/client'
import client from '@/api/client'

const rows = ref<any[]>([])
const loading = ref(false)
const uploading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await apiGet<any>('/admin/media', { page: 1, size: 50 })
    rows.value = page.records ?? []
  } finally {
    loading.value = false
  }
}

async function onUpload(option: any) {
  const fd = new FormData()
  fd.append('file', option.file)
  uploading.value = true
  try {
    await client.post('/admin/media/upload', fd)
    ElMessage.success('图片已上传')
    await load()
    option.onSuccess?.({})
  } catch (error) {
    option.onError?.(error)
    ElMessage.error('上传失败，请稍后重试')
  } finally {
    uploading.value = false
  }
}

async function copyUrl(url: string) {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制链接')
  }
}

onMounted(load)
</script>

<template>
  <div class="media-page">
    <el-card class="upload-panel" shadow="never">
      <div class="upload-copy">
        <span class="upload-icon"><el-icon><Picture /></el-icon></span>
        <div><h3>上传图片</h3><p>将图片添加到媒体库，供文章和页面使用。</p></div>
      </div>
      <el-upload class="media-uploader" drag :http-request="onUpload" :show-file-list="false" accept="image/*">
        <div class="upload-drop-content">
          <span class="drop-icon"><el-icon><UploadFilled /></el-icon></span>
          <strong>{{ uploading ? '正在上传…' : '拖放图片到这里' }}</strong>
          <span>或点击选择本地文件</span>
        </div>
      </el-upload>
    </el-card>

    <el-card class="module-card media-list-card" shadow="never">
      <div class="media-list-heading">
        <div class="table-title-block"><span class="table-title-icon"><el-icon><Picture /></el-icon></span><div><strong>媒体文件</strong><span>{{ rows.length }} 个文件</span></div></div>
        <el-button link type="primary" @click="load">刷新列表</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="文件" min-width="260">
          <template #default="{ row }">
            <div class="media-file-cell">
              <el-image v-if="row.url" class="media-thumb" :src="row.url" fit="cover" :preview-src-list="[row.url]" preview-teleported />
              <span v-else class="media-thumb media-thumb-empty"><el-icon><Picture /></el-icon></span>
              <div><strong>{{ row.originalName }}</strong><span>{{ row.url }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="190"><template #default="{ row }">{{ row.createdAt || '—' }}</template></el-table-column>
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }"><el-button link type="primary" @click="copyUrl(row.url)">复制链接</el-button></template>
        </el-table-column>
        <template #empty><el-empty description="媒体库还是空的，上传一张图片开始使用" :image-size="82" /></template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.media-page { display: flex; flex-direction: column; gap: 16px; }
.upload-panel { overflow: hidden; border: 1px solid #dfe9f5; background: linear-gradient(115deg, #fff 35%, #f4f8fe); }
.upload-panel :deep(.el-card__body) { display: grid; grid-template-columns: minmax(220px, 0.65fr) minmax(280px, 1fr); align-items: center; gap: 30px; padding: 22px 24px; }
.upload-copy { display: flex; align-items: flex-start; gap: 12px; }
.upload-icon { width: 39px; height: 39px; display: grid; place-items: center; color: #4c80c2; font-size: 18px; border-radius: 12px; background: #edf4fc; }
.upload-copy h3 { margin: 2px 0 0; color: #29394e; font-size: 14px; }
.upload-copy p { margin: 7px 0 0; color: #8390a1; font-size: 12px; line-height: 1.6; }
.media-uploader { width: 100%; }
.media-uploader :deep(.el-upload), .media-uploader :deep(.el-upload-dragger) { width: 100%; }
.media-uploader :deep(.el-upload-dragger) { height: 120px; display: grid; place-items: center; padding: 14px; border-color: #cfdded; border-radius: 12px; background: rgba(255,255,255,0.76); }
.media-uploader :deep(.el-upload-dragger:hover) { border-color: #7da9e2; background: #f9fbff; }
.upload-drop-content { display: flex; flex-direction: column; align-items: center; gap: 5px; color: #8492a4; font-size: 11px; }
.upload-drop-content strong { color: #52657c; font-size: 12px; }
.drop-icon { margin-bottom: 1px; color: #6d99d1; font-size: 20px; }
.module-card { border: 1px solid #e7ecf3; }
.module-card :deep(.el-card__body) { padding: 0 21px 18px; }
.media-list-heading { min-height: 72px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.table-title-block { display: flex; align-items: center; gap: 11px; }
.table-title-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #4c78b1; font-size: 16px; border-radius: 10px; background: #eff5fc; }
.table-title-block div { display: flex; flex-direction: column; gap: 3px; }
.table-title-block strong { color: #29394e; font-size: 13px; }
.table-title-block div span { color: #96a1af; font-size: 11px; }
.media-file-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.media-thumb { width: 42px; height: 42px; flex: none; border-radius: 9px; background: #f1f4f8; }
.media-thumb-empty { display: grid; place-items: center; color: #9eacbd; font-size: 16px; }
.media-file-cell div { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.media-file-cell strong, .media-file-cell div span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.media-file-cell strong { color: #2d3e54; font-size: 12px; }
.media-file-cell div span { color: #95a0af; font-size: 11px; }
@media (max-width: 760px) { .upload-panel :deep(.el-card__body) { grid-template-columns: 1fr; gap: 18px; padding: 18px; } }
@media (max-width: 640px) { .module-card :deep(.el-card__body) { padding: 0 12px 12px; } }
</style>


