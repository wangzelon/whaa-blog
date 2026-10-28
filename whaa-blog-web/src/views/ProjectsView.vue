<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getData } from '@/api/client'

const projects = ref<any[]>([])
const active = ref('all')

const categories = [
  { code: 'all', label: '全部' },
  { code: 'fun', label: '整活' },
  { code: 'app', label: '应用' },
  { code: 'tool', label: '工具' },
  { code: 'design', label: '设计' },
]

const filtered = computed(() => {
  if (active.value === 'all') return projects.value
  return projects.value.filter((p) => p.categoryCode === active.value)
})

onMounted(async () => {
  projects.value = await getData('/projects')
})
</script>

<template>
  <section>
    <h1>项目</h1>
    <div class="tabs">
      <button
        v-for="c in categories"
        :key="c.code"
        :class="{ active: active === c.code }"
        type="button"
        @click="active = c.code"
      >
        {{ c.label }}
      </button>
    </div>
    <div class="grid">
      <a v-for="p in filtered" :key="p.id" class="project" :href="p.url" target="_blank" rel="noopener">
        <h3>{{ p.title }}</h3>
        <p>{{ p.description }}</p>
      </a>
    </div>
  </section>
</template>

<style scoped>
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.tabs button {
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--text);
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  cursor: pointer;
}
.tabs button.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}
.project {
  display: block;
  padding: 1rem;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--card);
  color: inherit;
  text-decoration: none;
}
</style>
