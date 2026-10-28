<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getData } from '@/api/client'

const categories = ref<{ name: string; slug: string; postCount?: number }[]>([])

onMounted(async () => {
  categories.value = await getData('/categories')
})
</script>

<template>
  <section>
    <h1>分类</h1>
    <ul>
      <li v-for="c in categories" :key="c.slug">
        <RouterLink :to="`/categories/${c.slug}`">{{ c.name }}</RouterLink>
        <span>({{ c.postCount || 0 }})</span>
      </li>
    </ul>
  </section>
</template>
