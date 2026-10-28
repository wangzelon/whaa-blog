<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getData } from '@/api/client'

const albums = ref<any[]>([])

onMounted(async () => {
  albums.value = await getData('/albums')
})
</script>

<template>
  <section>
    <h1>相册</h1>
    <div class="grid">
      <RouterLink v-for="a in albums" :key="a.slug" :to="`/albums/${a.slug}`" class="album">
        <img v-if="a.coverUrl" :src="a.coverUrl" :alt="a.title" />
        <h3>{{ a.title }}</h3>
        <p>{{ a.description }}</p>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}
.album {
  display: block;
  padding: 0.75rem;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--card);
  color: inherit;
  text-decoration: none;
}
.album img {
  width: 100%;
  aspect-ratio: 4/3;
  object-fit: cover;
  border-radius: 8px;
}
</style>
