<script setup lang="ts">
defineProps<{
  slug: string
  title: string
  excerpt?: string
  description?: string
  publishedAt?: string
  category?: { name: string; slug: string }
  pinned?: boolean
}>()
</script>

<template>
  <article class="post-card">
    <div class="post-card-topline">
      <span class="post-type"><span class="post-type-dot"></span>{{ category?.name || '随笔' }}</span>
      <span v-if="pinned" class="pin">精选文章</span>
    </div>
    <h2><RouterLink :to="`/posts/${slug}`" class="title">{{ title }}</RouterLink></h2>
    <p v-if="description || excerpt" class="desc">{{ description || excerpt }}</p>
    <footer class="post-meta">
      <time v-if="publishedAt">{{ publishedAt.slice(0, 10) }}</time>
      <RouterLink v-if="category" :to="`/categories/${category.slug}`" class="category-pill">{{ category.name }}</RouterLink>
      <RouterLink :to="`/posts/${slug}`" class="more">阅读全文 <span aria-hidden="true">→</span></RouterLink>
    </footer>
  </article>
</template>

<style scoped>
.post-card { position: relative; margin-bottom: 18px; padding: 27px 30px 22px; border: 1px solid var(--border); border-radius: 14px; background: var(--surface); box-shadow: var(--card-shadow); transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease; }
.post-card:hover { transform: translateY(-2px); border-color: color-mix(in srgb, var(--accent) 30%, var(--border)); box-shadow: var(--card-shadow-hover); }
.post-card-topline { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 22px; }
.post-type { display: inline-flex; align-items: center; gap: 8px; color: var(--muted); font-size: 11px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
.post-type-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px var(--accent-soft); }
.pin { padding: 5px 9px; border-radius: 999px; color: #956500; background: #fff5dc; font-size: 10px; font-weight: 700; }
.post-card h2 { margin: 14px 0 10px; text-align: center; font-size: 21px; line-height: 1.5; font-weight: 700; letter-spacing: -.02em; }
.title { color: var(--text); text-decoration: none; }
.title:hover { color: var(--accent-strong); text-decoration: none; }
.desc { max-width: 720px; margin: 0 auto 20px; color: var(--muted); font-size: 13px; line-height: 1.9; }
.post-meta { display: flex; align-items: center; gap: 12px; padding-top: 15px; border-top: 1px solid var(--border-soft); color: var(--muted); font-size: 11px; }
.category-pill { padding: 4px 9px; border: 1px solid var(--accent-border); border-radius: 999px; color: var(--accent-strong); background: var(--accent-soft); font-size: 10px; text-decoration: none; }
.more { display: inline-flex; align-items: center; gap: 6px; margin-left: auto; color: var(--accent-strong); font-weight: 650; text-decoration: none; }
.more:hover { text-decoration: none; }
@media (max-width: 600px) { .post-card { padding: 22px 18px 17px; } .post-card h2 { font-size: 18px; } .post-meta { gap: 8px; } }
</style>
