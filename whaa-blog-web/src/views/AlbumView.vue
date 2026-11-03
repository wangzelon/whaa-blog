<script setup lang="ts">
import { onBeforeUnmount, onMounted, nextTick, ref } from 'vue'
import { useRoute } from 'vue-router'
import Viewer from 'viewerjs'
import 'viewerjs/dist/viewer.css'
import { getData } from '@/api/client'

interface AlbumMedia {
  id: number
  url: string
  caption?: string
  originalName?: string
  mimeType?: string
}

const route = useRoute()
const detail = ref<{ album: any; photos: AlbumMedia[] }>()
const photoGallery = ref<HTMLElement | null>(null)
let viewer: Viewer | null = null

function mediaName(photo: AlbumMedia, index: number) {
  return photo.originalName?.trim() || photo.caption?.trim() || `媒体 ${String(index + 1).padStart(2, '0')}`
}

function isVideo(photo: AlbumMedia) {
  return photo.mimeType?.startsWith('video/') || /\.(mp4|webm)(?:[?#]|$)/i.test(`${photo.originalName || ''} ${photo.url}`)
}

onMounted(async () => {
  detail.value = await getData(`/albums/${route.params.slug}`)
  await nextTick()

  if (photoGallery.value) {
    viewer = new Viewer(photoGallery.value, {
      loop: true,
      keyboard: true,
      navigation: true,
      navbar: true,
      toolbar: {
        zoomIn: 1,
        zoomOut: 1,
        oneToOne: 1,
        reset: 1,
        prev: 1,
        next: 1,
        rotateLeft: 1,
        rotateRight: 1,
      },
    })
  }
})

onBeforeUnmount(() => viewer?.destroy())
</script>

<template>
  <section v-if="detail">
    <header class="album-heading">
      <p class="album-eyebrow">PHOTO ALBUM <span>{{ detail.photos.length }} 个媒体文件</span></p>
      <h1>{{ detail.album.title }}</h1>
      <p v-if="detail.album.description" class="album-description">{{ detail.album.description }}</p>
    </header>
    <div ref="photoGallery" class="photos">
      <figure v-for="(photo, index) in detail.photos" :key="photo.id" class="photo-card">
        <div class="photo-frame">
          <video
            v-if="isVideo(photo)"
            class="album-video"
            :src="photo.url"
            :aria-label="mediaName(photo, index)"
            controls
            preload="metadata"
            playsinline
          />
          <img v-else :src="photo.url" :alt="mediaName(photo, index)" :title="`${mediaName(photo, index)} · 点击放大查看`" loading="lazy" />
          <span class="photo-index" :aria-label="`第 ${index + 1} 个媒体文件`">{{ String(index + 1).padStart(2, '0') }}</span>
        </div>
        <figcaption class="photo-info">
          <strong class="photo-name" :title="mediaName(photo, index)">{{ mediaName(photo, index) }}</strong>
          <span v-if="photo.caption && photo.originalName && photo.caption !== photo.originalName" class="photo-caption">{{ photo.caption }}</span>
        </figcaption>
      </figure>
    </div>
  </section>
</template>

<style scoped>
.album-heading { margin: 0 0 28px; }
.album-eyebrow { display: flex; align-items: center; gap: 12px; margin: 0 0 9px; color: var(--accent); font-size: 10px; font-weight: 750; letter-spacing: .14em; }
.album-eyebrow span { padding-left: 12px; color: var(--muted); font-size: 11px; font-weight: 500; letter-spacing: 0; border-left: 1px solid var(--border); }
.album-heading h1 { margin: 0; color: var(--text); font-size: clamp(26px, 4vw, 38px); letter-spacing: -.035em; }
.album-description { max-width: 700px; margin: 10px 0 0; color: var(--muted); line-height: 1.75; }
.photos {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 230px), 1fr));
  gap: clamp(12px, 2vw, 20px);
}
.photo-card { min-width: 0; margin: 0; padding: 10px; overflow: hidden; border: 1px solid var(--border); border-radius: 16px; background: var(--card, var(--surface)); box-shadow: 0 6px 20px rgba(24, 39, 61, .04); transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease; }
.photo-card:hover { transform: translateY(-2px); border-color: color-mix(in srgb, var(--accent) 42%, var(--border)); box-shadow: 0 12px 28px rgba(24, 39, 61, .09); }
.photo-frame { position: relative; overflow: hidden; aspect-ratio: 4 / 3; border-radius: 10px; background: var(--surface, #f2f4f7); cursor: zoom-in; }
.photo-frame img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform .3s ease;
}
.photo-frame .album-video { display: block; width: 100%; height: 100%; object-fit: contain; background: #101318; }
.photo-card:hover .photo-frame img { transform: scale(1.025); }
.photo-index { position: absolute; top: 9px; left: 9px; min-width: 32px; padding: 5px 7px; color: #fff; font-size: 10px; font-weight: 700; letter-spacing: .08em; text-align: center; border: 1px solid rgba(255,255,255,.35); border-radius: 7px; background: rgba(20, 31, 46, .68); backdrop-filter: blur(8px); }
.photo-info { display: flex; flex-direction: column; gap: 5px; padding: 11px 3px 3px; }
.photo-name { display: -webkit-box; overflow: hidden; color: var(--text); font-size: 13px; font-weight: 650; line-height: 1.45; overflow-wrap: anywhere; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.photo-caption { overflow: hidden; color: var(--muted); font-size: 11px; line-height: 1.5; text-overflow: ellipsis; white-space: nowrap; }
@media (prefers-reduced-motion: reduce) {
  .photo-card, .photo-frame img { transition: none; }
  .photo-card:hover { transform: none; }
}
</style>
