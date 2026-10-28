<script setup lang="ts">
import { ref } from 'vue'
import { getData } from '@/api/client'
import PostCard from '@/components/PostCard.vue'

const q = ref('')
const results = ref<any[]>([])

async function search() {
  if (!q.value.trim()) return
  results.value = await getData('/posts/search', { q: q.value.trim() })
}
</script>

<template>
  <section>
    <h1>搜索</h1>
    <form @submit.prevent="search">
      <input v-model="q" placeholder="输入关键词..." />
      <button type="submit">搜索</button>
    </form>
    <PostCard v-for="p in results" :key="p.slug" v-bind="p" />
  </section>
</template>

<style scoped>
form {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
input {
  flex: 1;
  padding: 0.5rem;
  border-radius: 6px;
  border: 1px solid var(--border);
}
button {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
}
</style>
