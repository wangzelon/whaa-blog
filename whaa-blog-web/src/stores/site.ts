import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getData } from '@/api/client'

export const useSiteStore = defineStore('site', () => {
  const config = ref<Record<string, unknown>>({})
  const loaded = ref(false)

  async function load() {
    if (loaded.value) return
    config.value = await getData<Record<string, unknown>>('/site/config')
    loaded.value = true
  }

  return { config, loaded, load }
})
