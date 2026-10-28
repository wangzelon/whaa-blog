<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, EditPen } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '@/api/client'

const route = useRoute()
const router = useRouter()
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const form = ref<any>({
  title: '',
  slug: '',
  description: '',
  contentMd: '',
  status: 'DRAFT',
  categoryId: null,
  tagIds: [] as number[],
  pinned: false,
  allowComment: true,
})
const wordCount = computed(() => form.value.contentMd?.trim() ? form.value.contentMd.trim().length : 0)

async function load() {
  loading.value = true
  try {
    const [categoryRows, tagRows] = await Promise.all([
      apiGet<any[]>('/admin/categories'),
      apiGet<any[]>('/admin/tags'),
    ])
    categories.value = categoryRows
    tags.value = tagRows
    const id = route.params.id
    if (id) {
      form.value = await apiGet<any>(`/admin/posts/${id}`)
      form.value.tagIds = (form.value.tags || []).map((tag: any) => tag.id)
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请先填写文章标题')
    return
  }
  saving.value = true
  try {
    await apiPost('/admin/posts', form.value)
    ElMessage.success('文章已保存')
    router.push('/posts')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="post-editor-layout">
    <el-card class="editor-content-card" shadow="never">
      <div class="editor-card-heading">
        <span class="editor-heading-icon"><el-icon><EditPen /></el-icon></span>
        <div><h3>文章内容</h3><p>写下标题、摘要和正文。</p></div>
      </div>
      <el-form label-position="top" class="post-form">
        <el-form-item label="文章标题"><el-input v-model="form.title" size="large" placeholder="给这篇文章起个标题" maxlength="120" show-word-limit /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="article-slug" /></el-form-item>
        <el-form-item label="文章摘要"><el-input v-model="form.description" type="textarea" :rows="3" resize="vertical" placeholder="用一两句话介绍文章内容" /></el-form-item>
        <el-form-item label="Markdown 正文">
          <el-input v-model="form.contentMd" class="markdown-editor" type="textarea" :rows="20" resize="vertical" placeholder="从这里开始写作…" />
        </el-form-item>
      </el-form>
      <div class="editor-word-count">{{ wordCount.toLocaleString() }} 个字符</div>
    </el-card>

    <aside class="editor-sidebar">
      <el-card class="editor-settings-card" shadow="never">
        <div class="editor-card-heading compact-heading">
          <span class="editor-heading-icon"><el-icon><Check /></el-icon></span>
          <div><h3>发布设置</h3><p>选择文章的展示方式。</p></div>
        </div>
        <el-form label-position="top" class="publish-form">
          <el-form-item label="发布状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="保存为草稿" value="DRAFT" />
              <el-option label="立即发布" value="PUBLISHED" />
            </el-select>
          </el-form-item>
          <el-form-item label="文章分类">
            <el-select v-model="form.categoryId" clearable placeholder="选择分类" style="width: 100%">
              <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="文章标签">
            <el-select v-model="form.tagIds" multiple collapse-tags collapse-tags-tooltip placeholder="选择标签" style="width: 100%">
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </el-form-item>
          <div class="publish-switch-row"><span><strong>置顶文章</strong><small>在文章列表优先展示</small></span><el-switch v-model="form.pinned" /></div>
          <div class="publish-switch-row"><span><strong>开放评论</strong><small>允许读者留言互动</small></span><el-switch v-model="form.allowComment" /></div>
        </el-form>
        <div class="publish-actions">
          <el-button type="primary" :loading="saving" :icon="Check" @click="save">保存文章</el-button>
          <el-button :icon="ArrowLeft" @click="router.push('/posts')">返回列表</el-button>
        </div>
      </el-card>
    </aside>
  </div>
</template>

<style scoped>
.post-editor-layout { display: grid; grid-template-columns: minmax(0, 1fr) 310px; align-items: start; gap: 16px; }
.editor-content-card, .editor-settings-card { border: 1px solid #e7ecf3; }
.editor-content-card :deep(.el-card__body) { padding: 24px 25px 17px; }
.editor-settings-card :deep(.el-card__body) { padding: 20px; }
.editor-card-heading { display: flex; align-items: center; gap: 11px; margin-bottom: 21px; }
.editor-heading-icon { width: 36px; height: 36px; display: grid; place-items: center; color: #3f7bd0; font-size: 16px; border-radius: 11px; background: #edf4fe; }
.editor-card-heading h3 { margin: 0; color: #29394e; font-size: 14px; }
.editor-card-heading p { margin: 5px 0 0; color: #8b97a7; font-size: 11px; }
.post-form :deep(.el-form-item) { margin-bottom: 19px; }
.post-form :deep(.el-form-item__label), .publish-form :deep(.el-form-item__label) { color: #4b5b70; font-size: 12px; font-weight: 650; }
.markdown-editor :deep(.el-textarea__inner) { min-height: 380px !important; padding: 16px; color: #29394e; background: #fbfcfe; font-family: Consolas, "SFMono-Regular", monospace; font-size: 13px; line-height: 1.75; }
.editor-word-count { color: #9aa5b3; font-size: 11px; text-align: right; }
.compact-heading { margin-bottom: 17px; }
.publish-form :deep(.el-form-item) { margin-bottom: 17px; }
.publish-switch-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 0; border-top: 1px solid #eff2f6; }
.publish-switch-row span { display: flex; flex-direction: column; gap: 4px; }
.publish-switch-row strong { color: #3b4b60; font-size: 12px; font-weight: 600; }
.publish-switch-row small { color: #96a1af; font-size: 10px; }
.publish-actions { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; padding-top: 15px; border-top: 1px solid #eff2f6; }
.publish-actions .el-button { width: 100%; margin-left: 0; }
@media (max-width: 960px) { .post-editor-layout { grid-template-columns: 1fr; } .editor-sidebar { order: -1; } .publish-actions { flex-direction: row; } }
@media (max-width: 640px) { .editor-content-card :deep(.el-card__body) { padding: 18px 15px 15px; } .editor-settings-card :deep(.el-card__body) { padding: 17px; } .markdown-editor :deep(.el-textarea__inner) { min-height: 280px !important; } }
</style>
