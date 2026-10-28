<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useHead } from '@vueuse/head'
import { getData } from '@/api/client'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const post = ref<{
  id: number
  title: string
  contentHtml: string
  publishedAt?: string
  authorName?: string
  allowComment?: boolean
}>()

onMounted(async () => {
  post.value = await getData(`/posts/${route.params.slug}`)
  useHead({ title: post.value?.title })
})
</script>

<template>
  <article v-if="post" class="post">
    <h1>{{ post.title }}</h1>
    <p class="meta">{{ post.publishedAt?.slice(0, 10) }} · {{ post.authorName }}</p>
    <div class="prose" v-html="post.contentHtml" />
    <CommentSection v-if="post.allowComment !== false" target-type="POST" :target-id="post.id" />
  </article>
</template>

<style scoped>
.post h1 {
  margin-top: 0;
}
.meta {
  color: var(--muted);
}
</style>
