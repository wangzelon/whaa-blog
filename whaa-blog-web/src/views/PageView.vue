<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { getData } from '@/api/client'
import CommentSection from '@/components/CommentSection.vue'

const props = defineProps<{ slug: string }>()
const page = ref<any>()

async function load() {
  page.value = await getData(`/pages/${props.slug}`)
}

onMounted(load)
watch(() => props.slug, load)
</script>

<template>
  <article v-if="page">
    <h1>{{ page.title }}</h1>
    <div class="prose" v-html="page.contentHtml" />
    <CommentSection
      v-if="page.allowComment"
      target-type="PAGE"
      :target-id="page.id"
    />
  </article>
</template>
