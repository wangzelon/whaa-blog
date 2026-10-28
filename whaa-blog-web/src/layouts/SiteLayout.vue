<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSiteStore } from '@/stores/site'
import { clearAuth, currentUser } from '@/stores/auth'
import ClickBubbles from '@/components/ClickBubbles.vue'

const site = useSiteStore()
const route = useRoute()
const router = useRouter()

onMounted(() => site.load())

const siteInfo = computed(() => (site.config.site as Record<string, string>) || {})
const sidebar = computed(() => (site.config.sidebar as Record<string, unknown>) || {})
const social = computed(() => (site.config.social as Array<{ name: string; url: string }>) || [])
const configuredNav = computed(() => (sidebar.value.nav as Array<{ label: string; path: string }>) || [])
const defaultNav = [
  { label: '首页', path: '/' },
  { label: '归档', path: '/archives' },
  { label: '分类', path: '/categories' },
  { label: '标签', path: '/tags' },
  { label: '项目', path: '/projects' },
  { label: '友链', path: '/links' },
]
const nav = computed(() => {
  const source = configuredNav.value.length ? configuredNav.value : defaultNav
  const items = source.filter((item) => item.path !== '/search' && item.path !== '/about/site')
  if (!items.some((item) => item.path === '/')) items.unshift(defaultNav[0])
  return items
})
const isHome = computed(() => route.name === 'home')
const pageTitle = computed(() => {
  if (route.path.startsWith('/posts/')) return '文章详情'
  if (route.path.startsWith('/categories/')) return '分类文章'
  if (route.path.startsWith('/tags/')) return '标签文章'
  const current = nav.value.find((item) => item.path === route.path)
  const extra: Record<string, string> = {
    '/search': '站内搜索',
    '/about/site': '关于站点',
    '/about': '关于我',
    '/messages': '站内私信',
  }
  return current?.label || extra[route.path] || '页面'
})

function isActive(path: string) {
  return path === '/' ? route.path === '/' : route.path === path || route.path.startsWith(`${path}/`)
}

function logout() {
  clearAuth()
  router.push('/')
}

function navMark(label: string) {
  const marks: Record<string, string> = {
    首页: '⌂', 归档: '◷', 分类: '▦', 标签: '#', 项目: '◇', 友链: '↗', 相册: '▧',
  }
  return marks[label] || '·'
}

function toggleTheme() {
  document.documentElement.classList.toggle('dark')
}
</script>

<template>
  <div class="site-shell">
    <header class="site-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/" aria-label="返回首页">
          <span class="brand-mark">W</span>
          <span class="brand-copy"><strong>{{ siteInfo.title || 'WhaaBlog' }}</strong><small>PERSONAL BLOG</small></span>
        </RouterLink>

        <nav class="main-nav" aria-label="主导航">
          <RouterLink v-for="item in nav" :key="item.path" :to="item.path" :class="{ active: isActive(item.path) }">
            <span class="nav-mark" aria-hidden="true">{{ navMark(item.label) }}</span>{{ item.label }}
          </RouterLink>
        </nav>

        <div class="header-actions">
          <RouterLink v-if="currentUser" class="auth-nav-link" to="/messages">消息</RouterLink>
          <template v-else>
            <RouterLink class="auth-nav-link" to="/login">登录</RouterLink>
            <RouterLink class="auth-nav-link auth-nav-primary" to="/register">注册</RouterLink>
          </template>
          <RouterLink class="icon-action" to="/search" aria-label="搜索" title="搜索">⌕</RouterLink>
          <button class="icon-action" type="button" aria-label="切换主题" title="切换主题" @click="toggleTheme">◐</button>
          <button v-if="currentUser" class="logout-link" type="button" @click="logout">退出</button>
        </div>
      </div>
      <nav class="mobile-nav" aria-label="移动端主导航">
        <RouterLink v-for="item in nav" :key="item.path" :to="item.path" :class="{ active: isActive(item.path) }">{{ item.label }}</RouterLink>
        <RouterLink to="/search" :class="{ active: route.path === '/search' }">搜索</RouterLink>
        <RouterLink v-if="currentUser" to="/messages" :class="{ active: route.path === '/messages' }">消息</RouterLink>
        <template v-else>
          <RouterLink to="/login" :class="{ active: route.path === '/login' }">登录</RouterLink>
          <RouterLink to="/register" :class="{ active: route.path === '/register' }">注册</RouterLink>
        </template>
      </nav>
    </header>

    <section v-if="isHome" class="hero">
      <div class="hero-avatar-wrap">
        <img class="hero-avatar" :src="(sidebar.avatar as string) || '/avatar.svg'" :alt="`${siteInfo.title || 'WhaaBlog'} 头像`" />
        <span class="avatar-status" aria-hidden="true"></span>
      </div>
      <p class="hero-eyebrow">A PERSONAL SPACE FOR THOUGHTS</p>
      <h1>{{ siteInfo.title || 'WhaaBlog' }}</h1>
      <p class="hero-subtitle">{{ siteInfo.subtitle || 'All at sea.' }}</p>
      <p class="hero-description">{{ siteInfo.description || '记录灵感、项目与日常，在这里慢慢整理自己的想法。' }}</p>
      <div v-if="social.length" class="hero-socials" aria-label="社交链接">
        <a v-for="item in social" :key="item.url" :href="item.url" target="_blank" rel="noopener noreferrer" :aria-label="item.name" :title="item.name">
          {{ item.name.slice(0, 1).toUpperCase() }}
        </a>
      </div>
      <div v-else class="hero-socials hero-socials-placeholder" aria-hidden="true">
        <span>W</span><span>✦</span><span>↗</span>
      </div>
    </section>

    <main class="main-content">
      <div v-if="!isHome" class="page-context">
        <span class="context-dot"></span>
        <span>{{ siteInfo.title || 'WhaaBlog' }}</span>
        <span class="context-separator">/</span>
        <strong>{{ pageTitle }}</strong>
      </div>
      <div class="page-content"><RouterView /></div>
      <footer class="site-footer">
        <span>© {{ new Date().getFullYear() }} {{ siteInfo.title || 'WhaaBlog' }}</span>
        <span>让每一次记录都有自己的位置。</span>
      </footer>
    </main>
    <ClickBubbles />
  </div>
</template>
