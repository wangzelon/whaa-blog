<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getData } from '@/api/client'
import PostCard from '@/components/PostCard.vue'

interface Post {
  slug: string
  title: string
  excerpt?: string
  description?: string
  publishedAt?: string
  category?: { name: string; slug: string }
  pinned?: boolean
}

interface PageResult {
  records: Post[]
  total: number
  page: number
  size: number
}

const posts = ref<Post[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const error = ref('')
const size = 10

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await getData<PageResult>('/posts', { page: page.value, size })
    posts.value = data.records
    total.value = data.total
  } catch {
    error.value = '文章暂时无法加载，请稍后再试。'
  } finally {
    loading.value = false
  }
}

function changePage(nextPage: number) {
  page.value = nextPage
  void load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(load)
</script>

<template>
  <section class="home-page">
    <div class="feed-heading">
      <div>
        <p class="section-eyebrow">THE LATEST</p>
        <h2>最近的记录</h2>
        <p>一些想法、项目进展，以及偶尔写下的日常。</p>
      </div>
      <RouterLink class="all-posts-link" to="/archives">浏览归档 <span aria-hidden="true">↗</span></RouterLink>
    </div>

    <div v-if="loading && !posts.length" class="feed-state">正在整理最近的文章…</div>
    <div v-else-if="error" class="feed-state feed-error">{{ error }}</div>
    <template v-else>
      <PostCard v-for="p in posts" :key="p.slug" v-bind="p" />
      <div v-if="!posts.length" class="feed-state">这里还没有文章，稍后再来看看吧。</div>
      <nav v-if="total > size" class="pagination" aria-label="文章分页">
        <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">← 较新文章</button>
        <span>第 {{ page }} 页 · 共 {{ total }} 篇</span>
        <button type="button" :disabled="page * size >= total || loading" @click="changePage(page + 1)">更早文章 →</button>
      </nav>
    </template>
  </section>
</template>
