<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getData } from '@/api/client'

const route = useRoute()
const detail = ref<{ album: any; photos: any[] }>()

onMounted(async () => {
  detail.value = await getData(`/albums/${route.params.slug}`)
})
</script>

<template>
  <section v-if="detail">
    <h1>{{ detail.album.title }}</h1>
    <p>{{ detail.album.description }}</p>
    <div class="photos">
      <figure v-for="p in detail.photos" :key="p.id">
        <img :src="p.url" :alt="p.caption || ''" />
        <figcaption v-if="p.caption">{{ p.caption }}</figcaption>
      </figure>
    </div>
  </section>
</template>

<style scoped>
.photos {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 0.75rem;
}
img {
  width: 100%;
  border-radius: 8px;
}
figcaption {
  font-size: 0.85rem;
  color: var(--muted);
}
</style>
