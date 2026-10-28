<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getData } from '@/api/client'

const page = ref<any>()
const timeline = ref<any[]>([])

onMounted(async () => {
  page.value = await getData('/pages/about-site')
  timeline.value = await getData('/timeline/site')
})
</script>

<template>
  <section v-if="page">
    <h1>{{ page.title }}</h1>
    <div class="prose" v-html="page.contentHtml" />
    <h2>站点编年史</h2>
    <ul class="timeline">
      <li v-for="t in timeline" :key="t.id">
        <time>{{ t.eventDate }}</time>
        <div>
          <strong>{{ t.title }}</strong>
          <p>{{ t.content }}</p>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.timeline {
  list-style: none;
  padding: 0;
}
.timeline li {
  display: grid;
  grid-template-columns: 7rem 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}
time {
  color: var(--muted);
}
</style>
