<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getData } from '@/api/client'
import PostCard from '@/components/PostCard.vue'

const route = useRoute()
const category = ref<{ id: number; name: string }>()
const posts = ref<any[]>([])

async function load() {
  category.value = await getData(`/categories/${route.params.slug}`)
  const page = await getData<any>('/posts', { page: 1, size: 50, categoryId: category.value?.id })
  posts.value = page.records
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <section>
    <h1>分类：{{ category?.name }}</h1>
    <PostCard v-for="p in posts" :key="p.slug" v-bind="p" />
  </section>
</template>
