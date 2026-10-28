<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getData } from '@/api/client'

interface Group {
  year: number
  items: { slug: string; title: string; publishedAt: string }[]
}

const groups = ref<Group[]>([])

onMounted(async () => {
  groups.value = await getData('/posts/archive')
})
</script>

<template>
  <section>
    <h1>归档</h1>
    <div v-for="g in groups" :key="g.year" class="year">
      <h2>{{ g.year }}</h2>
      <ul>
        <li v-for="item in g.items" :key="item.slug">
          <time>{{ item.publishedAt?.slice(5, 10) }}</time>
          <RouterLink :to="`/posts/${item.slug}`">{{ item.title }}</RouterLink>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.year h2 {
  border-left: 4px solid var(--accent);
  padding-left: 0.5rem;
}
li {
  display: flex;
  gap: 1rem;
  margin: 0.35rem 0;
}
time {
  color: var(--muted);
  min-width: 3rem;
}
</style>
