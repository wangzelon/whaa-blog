<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getData } from '@/api/client'

const links = ref<any[]>([])

onMounted(async () => {
  links.value = await getData('/links', { random: true })
})
</script>

<template>
  <section>
    <h1>友链</h1>
    <div class="grid">
      <a
        v-for="l in links"
        :key="l.id"
        class="link-card"
        :href="l.url"
        target="_blank"
        rel="noopener"
        :style="{ borderColor: l.color || '#888' }"
      >
        <img v-if="l.avatar" :src="l.avatar" alt="" />
        <div>
          <strong>{{ l.blogName || l.name }}</strong>
          <p>{{ l.description }}</p>
        </div>
      </a>
    </div>
  </section>
</template>

<style scoped>
.grid {
  display: grid;
  gap: 1rem;
}
.link-card {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--border);
  border-left-width: 4px;
  border-radius: 10px;
  background: var(--card);
  color: inherit;
  text-decoration: none;
}
.link-card img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}
</style>
