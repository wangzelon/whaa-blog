<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { getData } from '@/api/client'
// @ts-expect-error no types
import WordCloud from 'wordcloud'

const canvasRef = ref<HTMLCanvasElement | null>(null)
const tags = ref<{ name: string; slug: string; weight?: number }[]>([])

onMounted(async () => {
  tags.value = await getData('/tags', { cloud: true })
  if (canvasRef.value) {
    WordCloud(canvasRef.value, {
      list: tags.value.map((t) => [t.name, Math.max(12, (t.weight || 1) * 8)]),
      gridSize: 8,
      weightFactor: 1.2,
      color: () => '#0078e7',
    })
  }
})

onUnmounted(() => {})
</script>

<template>
  <section>
    <h1>标签</h1>
    <canvas ref="canvasRef" width="700" height="320" />
    <ul class="tag-list">
      <li v-for="t in tags" :key="t.slug">
        <RouterLink :to="`/tags/${t.slug}`">{{ t.name }} ({{ t.weight || 0 }})</RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped>
canvas {
  max-width: 100%;
  background: var(--card);
  border-radius: 12px;
  border: 1px solid var(--border);
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  list-style: none;
  padding: 0;
}
</style>
