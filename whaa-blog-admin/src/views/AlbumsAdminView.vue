<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowUp, Collection, Delete, EditPen, Picture, Plus, Upload } from '@element-plus/icons-vue'
import client, { apiDelete, apiGet, apiPost, type Result } from '@/api/client'

interface AlbumMedia {
  url: string
  caption: string
  mimeType?: string
  originalName?: string
}

interface Album {
  id: number
  title: string
  slug: string
  description?: string
  coverUrl?: string
  visible: boolean
  createdAt?: string
}

interface MediaItem {
  id: number
  url: string
  originalName: string
  mimeType: string
  sizeBytes?: number
}

const MAX_IMAGE_SIZE_BYTES = 20 * 1024 * 1024
const MAX_VIDEO_SIZE_BYTES = 500 * 1024 * 1024
const allowedVideoTypes = new Set(['video/mp4', 'video/webm'])

function emptyForm() {
  return {
    id: undefined as number | undefined,
    title: '',
    slug: '',
    description: '',
    coverUrl: '',
    visible: true,
    photos: [] as AlbumMedia[],
  }
}

const albums = ref<Album[]>([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const editorVisible = ref(false)
const mediaPickerVisible = ref(false)
const mediaLoading = ref(false)
const mediaItems = ref<MediaItem[]>([])
const selectedMediaIds = ref<number[]>([])
const form = ref(emptyForm())
const editorTitle = computed(() => form.value.id ? '编辑相册' : '新建相册')
const imagePreviewUrls = computed(() => form.value.photos.filter((item) => !isVideoMedia(item)).map((item) => item.url))

function isVideoMedia(item: Pick<AlbumMedia, 'url' | 'mimeType' | 'originalName'>) {
  return item.mimeType?.startsWith('video/') || /\.(mp4|webm)(?:[?#]|$)/i.test(`${item.originalName || ''} ${item.url}`)
}

function firstImageUrl() {
  return form.value.photos.find((item) => !isVideoMedia(item))?.url || ''
}

function beforeMediaUpload(file: File) {
  const isVideo = file.type.startsWith('video/') || /\.(mp4|webm)$/i.test(file.name)
  if (isVideo && file.type && file.type !== 'application/octet-stream' && !allowedVideoTypes.has(file.type)) {
    ElMessage.error('目前支持 MP4 和 WebM 视频')
    return false
  }
  const maxSize = isVideo ? MAX_VIDEO_SIZE_BYTES : MAX_IMAGE_SIZE_BYTES
  if (file.size > maxSize) {
    ElMessage.error(isVideo ? '视频不能超过 500 MB' : '图片不能超过 20 MB')
    return false
  }
  return true
}

async function loadAlbums() {
  loading.value = true
  try {
    albums.value = await apiGet<Album[]>('/admin/albums')
  } catch {
    ElMessage.error('相册列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = emptyForm()
  editorVisible.value = true
}

async function openEdit(id: number) {
  loading.value = true
  try {
    const detail = await apiGet<{ album: Album; photos: Array<{ url: string; caption?: string; mimeType?: string; originalName?: string }> }>(`/admin/albums/${id}`)
    form.value = {
      id: detail.album.id,
      title: detail.album.title || '',
      slug: detail.album.slug || '',
      description: detail.album.description || '',
      coverUrl: detail.album.coverUrl || '',
      visible: detail.album.visible !== false,
      photos: (detail.photos || []).map((photo) => ({
        url: photo.url,
        caption: photo.caption || '',
        mimeType: photo.mimeType,
        originalName: photo.originalName,
      })),
    }
    editorVisible.value = true
  } catch {
    ElMessage.error('相册详情加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function saveAlbum() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写相册名称')
    return
  }
  if (!form.value.slug.trim()) {
    ElMessage.warning('请填写相册 Slug，例如 travel-2026')
    return
  }
  if (!form.value.photos.length) {
    ElMessage.warning('请至少添加一张图片或视频')
    return
  }

  saving.value = true
  try {
    const coverUrl = form.value.coverUrl.trim() || firstImageUrl()
    await apiPost('/admin/albums', {
      ...form.value,
      title: form.value.title.trim(),
      slug: form.value.slug.trim(),
      coverUrl,
      photos: form.value.photos.map(({ url, caption }) => ({ url, caption: caption.trim() })),
    })
    ElMessage.success(form.value.id ? '相册已更新' : '相册已创建')
    editorVisible.value = false
    await loadAlbums()
  } catch (error) {
    ElMessage.error(error instanceof Error ? `保存失败：${error.message}` : '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function removeAlbum(album: Album) {
  try {
    await ElMessageBox.confirm(`确认删除相册“${album.title}”？其中的媒体关联也会移除。`, '删除相册', {
      confirmButtonText: '删除相册',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await apiDelete(`/admin/albums/${album.id}`)
    ElMessage.success('相册已删除')
    await loadAlbums()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败，请稍后重试')
  }
}

async function openMediaPicker() {
  mediaPickerVisible.value = true
  mediaLoading.value = true
  selectedMediaIds.value = []
  try {
    const page = await apiGet<{ records: MediaItem[] }>('/admin/media', { page: 1, size: 100 })
    mediaItems.value = (page.records || []).filter((item) => item.mimeType?.startsWith('image/') || item.mimeType?.startsWith('video/'))
  } catch {
    ElMessage.error('媒体库加载失败，请稍后重试')
  } finally {
    mediaLoading.value = false
  }
}

function addPhotos(items: MediaItem[]) {
  for (const item of items) {
    if (!form.value.photos.some((photo) => photo.url === item.url)) {
      form.value.photos.push({ url: item.url, caption: '', mimeType: item.mimeType, originalName: item.originalName })
    }
  }
  if (!form.value.coverUrl && form.value.photos.length) {
    form.value.coverUrl = firstImageUrl()
  }
}

function addSelectedMedia() {
  const selected = new Set(selectedMediaIds.value)
  addPhotos(mediaItems.value.filter((item) => selected.has(item.id)))
  mediaPickerVisible.value = false
}

async function uploadMedia(option: any) {
  const data = new FormData()
  data.append('file', option.file)
  uploading.value = true
  uploadPercent.value = 0
  try {
    const response = await client.post<Result<MediaItem>>('/admin/media/upload', data, {
      timeout: 0,
      onUploadProgress: (event) => {
        if (event.total) uploadPercent.value = Math.round((event.loaded / event.total) * 100)
      },
    })
    addPhotos([response.data.data])
    ElMessage.success('媒体已上传并加入当前相册')
    option.onSuccess?.(response.data.data)
  } catch (error) {
    ElMessage.error(error instanceof Error ? `媒体上传失败：${error.message}` : '媒体上传失败，请稍后重试')
    option.onError?.(error)
  } finally {
    uploading.value = false
    uploadPercent.value = 0
  }
}

function removePhoto(index: number) {
  const [removed] = form.value.photos.splice(index, 1)
  if (removed?.url === form.value.coverUrl) {
    form.value.coverUrl = firstImageUrl()
  }
}

function movePhoto(index: number, offset: -1 | 1) {
  const target = index + offset
  if (target < 0 || target >= form.value.photos.length) return
  const [photo] = form.value.photos.splice(index, 1)
  form.value.photos.splice(target, 0, photo)
}

onMounted(loadAlbums)
</script>

<template>
  <div class="albums-page">
    <div class="module-toolbar">
      <div class="toolbar-copy">
        <strong>相册内容</strong>
        <span>管理前台展示的相册、封面和照片顺序。</span>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建相册</el-button>
    </div>

    <el-card class="module-card" shadow="never">
      <div class="table-card-heading">
        <div class="table-title-block">
          <span class="table-title-icon"><el-icon><Collection /></el-icon></span>
          <div><strong>全部相册</strong><span>{{ albums.length }} 个相册</span></div>
        </div>
      </div>
      <el-table v-loading="loading" :data="albums" row-key="id">
        <el-table-column label="相册" min-width="260">
          <template #default="{ row }">
            <div class="album-cell">
              <el-image v-if="row.coverUrl" class="album-cover" :src="row.coverUrl" fit="cover" :preview-src-list="[row.coverUrl]" preview-teleported />
              <span v-else class="album-cover album-cover-empty"><el-icon><Picture /></el-icon></span>
              <div><strong>{{ row.title }}</strong><span>/albums/{{ row.slug }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="简介" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '—' }}</template>
        </el-table-column>
        <el-table-column label="前台展示" width="120">
          <template #default="{ row }">
            <el-tag :type="row.visible ? 'success' : 'info'" effect="light" round>{{ row.visible ? '展示中' : '已隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ row.createdAt || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="EditPen" @click="openEdit(row.id)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="removeAlbum(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="还没有相册，创建后即可整理照片" :image-size="82" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="editorVisible" :title="editorTitle" width="min(920px, 94vw)" top="4vh" destroy-on-close>
      <div class="album-editor" v-loading="saving">
        <el-form label-position="top" class="album-form">
          <div class="form-grid">
            <el-form-item label="相册名称" required>
              <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：周末旅行" />
            </el-form-item>
            <el-form-item label="Slug（前台地址）" required>
              <el-input v-model="form.slug" placeholder="例如：weekend-trip" />
            </el-form-item>
          </div>
          <el-form-item label="相册简介">
            <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="简单介绍这个相册" />
          </el-form-item>
          <div class="form-grid cover-row">
            <el-form-item label="封面图片链接">
              <el-input v-model="form.coverUrl" placeholder="默认使用第一张照片作为封面" />
            </el-form-item>
            <el-form-item label="前台展示">
              <div class="visible-control"><el-switch v-model="form.visible" /><span>{{ form.visible ? '访客可以查看' : '仅后台可见' }}</span></div>
            </el-form-item>
          </div>
        </el-form>

        <div class="photos-heading">
          <div><strong>相册媒体</strong><span>{{ form.photos.length }} 个文件，顺序即前台展示顺序</span></div>
          <div class="photo-tools">
            <el-button :icon="Picture" @click="openMediaPicker">从媒体库选择</el-button>
            <el-upload accept="image/jpeg,image/png,image/gif,image/webp,image/svg+xml,video/mp4,video/webm,.mp4,.webm" :before-upload="beforeMediaUpload" :show-file-list="false" :http-request="uploadMedia">
              <el-button type="primary" plain :icon="Upload" :loading="uploading">{{ uploading ? `上传中 ${uploadPercent}%` : '上传图片或视频' }}</el-button>
            </el-upload>
          </div>
        </div>

        <div v-if="form.photos.length" class="photo-list">
          <article v-for="(photo, index) in form.photos" :key="`${photo.url}-${index}`" class="photo-row">
            <video v-if="isVideoMedia(photo)" class="photo-video-preview" :src="photo.url" controls preload="metadata" playsinline />
            <el-image v-else class="photo-preview" :src="photo.url" fit="cover" :preview-src-list="imagePreviewUrls" :initial-index="Math.max(0, imagePreviewUrls.indexOf(photo.url))" preview-teleported />
            <div class="photo-fields">
              <el-input v-model="photo.caption" placeholder="图片或视频说明（可选）" maxlength="200" />
              <div class="photo-meta">
                <el-tag v-if="isVideoMedia(photo)" effect="plain">视频</el-tag>
                <el-tag v-else-if="form.coverUrl === photo.url" type="warning" effect="plain">封面</el-tag>
                <el-button v-else link type="primary" @click="form.coverUrl = photo.url">设为封面</el-button>
                <span class="photo-url" :title="photo.originalName || photo.url">{{ photo.originalName || photo.url }}</span>
              </div>
            </div>
            <div class="photo-actions">
              <el-button link :icon="ArrowUp" :disabled="index === 0" aria-label="上移媒体" @click="movePhoto(index, -1)" />
              <el-button link :icon="ArrowDown" :disabled="index === form.photos.length - 1" aria-label="下移媒体" @click="movePhoto(index, 1)" />
              <el-button link type="danger" :icon="Delete" aria-label="移除媒体" @click="removePhoto(index)" />
            </div>
          </article>
        </div>
        <el-empty v-else description="还没有媒体，可从媒体库选择或直接上传" :image-size="72" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveAlbum">保存相册</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="mediaPickerVisible" title="从媒体库选择媒体" width="min(780px, 94vw)" append-to-body>
      <div v-loading="mediaLoading" class="media-picker">
        <el-checkbox-group v-model="selectedMediaIds" class="media-grid">
          <div v-for="item in mediaItems" :key="item.id" class="media-choice">
            <el-checkbox :value="item.id" :label="item.id" />
            <video v-if="item.mimeType.startsWith('video/')" :src="item.url" controls preload="metadata" playsinline />
            <el-image v-else :src="item.url" fit="cover" :preview-src-list="[item.url]" preview-teleported />
            <span :title="item.originalName">{{ item.originalName }}</span>
          </div>
        </el-checkbox-group>
        <el-empty v-if="!mediaLoading && !mediaItems.length" description="媒体库还没有文件，请先上传图片或视频" :image-size="72" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="mediaPickerVisible = false">取消</el-button>
          <el-button type="primary" @click="addSelectedMedia">添加选中媒体</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.albums-page { display: flex; flex-direction: column; gap: 15px; }
.module-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.toolbar-copy { display: flex; flex-direction: column; gap: 5px; }
.toolbar-copy strong { color: #2b3b51; font-size: 13px; }
.toolbar-copy span { color: #8d99a8; font-size: 11px; }
.module-card { border: 1px solid #e7ecf3; }
.module-card :deep(.el-card__body) { padding: 0 20px 16px; }
.table-card-heading { min-height: 70px; display: flex; align-items: center; }
.table-title-block { display: flex; align-items: center; gap: 11px; }
.table-title-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #4c78b1; font-size: 16px; border-radius: 10px; background: #eff5fc; }
.table-title-block div { display: flex; flex-direction: column; gap: 3px; }
.table-title-block strong { color: #29394e; font-size: 13px; }
.table-title-block div span { color: #96a1af; font-size: 11px; }
.album-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.album-cover { width: 54px; height: 42px; flex: none; border-radius: 8px; background: #f1f4f8; }
.album-cover-empty { display: grid; place-items: center; color: #9eacbd; font-size: 17px; }
.album-cell div { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.album-cell strong, .album-cell span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.album-cell strong { color: #2d3e54; font-size: 12px; }
.album-cell div span { color: #95a0af; font-size: 11px; }
.album-editor { max-height: 70vh; overflow-y: auto; padding: 0 2px; }
.album-form { padding: 0 8px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-grid :deep(.el-form-item) { margin-bottom: 13px; }
.visible-control { height: 32px; display: flex; align-items: center; gap: 10px; color: #7b8797; font-size: 12px; }
.photos-heading { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin: 4px 8px 13px; padding-top: 17px; border-top: 1px solid #edf0f4; }
.photos-heading > div:first-child { display: flex; flex-direction: column; gap: 4px; }
.photos-heading strong { color: #29394e; font-size: 13px; }
.photos-heading span { color: #96a1af; font-size: 11px; }
.photo-tools { display: flex; align-items: center; gap: 8px; }
.photo-list { display: flex; flex-direction: column; gap: 9px; padding: 0 8px 8px; }
.photo-row { display: grid; grid-template-columns: 88px minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 9px; border: 1px solid #e9edf3; border-radius: 11px; background: #fff; }
.photo-preview { width: 88px; height: 68px; border-radius: 8px; background: #f1f4f8; }
.photo-video-preview { width: 136px; height: 88px; border-radius: 8px; background: #111; }
.photo-fields { min-width: 0; display: flex; flex-direction: column; gap: 7px; }
.photo-meta { min-width: 0; display: flex; align-items: center; gap: 8px; }
.photo-url { overflow: hidden; color: #9aa5b2; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.photo-actions { display: flex; align-items: center; gap: 1px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; }
.media-picker { min-height: 180px; max-height: 58vh; overflow-y: auto; }
.media-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 12px; }
.media-choice { position: relative; display: flex; flex-direction: column; gap: 7px; min-width: 0; padding: 8px; border: 1px solid #e8edf3; border-radius: 10px; }
.media-choice :deep(.el-checkbox) { position: absolute; z-index: 1; top: 12px; left: 12px; margin: 0; padding: 3px; border-radius: 5px; background: rgba(255,255,255,.92); }
.media-choice :deep(.el-image), .media-choice video { width: 100%; height: 108px; border-radius: 7px; background: #111; object-fit: cover; }
.media-choice > span { overflow: hidden; color: #6f7f92; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 700px) {
  .module-card :deep(.el-card__body) { padding: 0 12px 12px; }
  .form-grid { grid-template-columns: 1fr; gap: 0; }
  .photos-heading { align-items: flex-start; flex-direction: column; }
  .photo-row { grid-template-columns: minmax(88px, 28vw) minmax(0, 1fr); gap: 9px; }
  .photo-preview, .photo-video-preview { width: 100%; height: 64px; }
  .photo-actions { grid-column: 2; justify-content: flex-end; }
}
</style>
