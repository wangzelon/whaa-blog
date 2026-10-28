<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  ChatDotRound,
  Clock,
  CollectionTag,
  DataBoard,
  Document,
  EditPen,
  Files,
  FolderOpened,
  Link,
  Menu,
  Picture,
  PriceTag,
  Setting,
  View,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const mobileNavOpen = ref(false)

const navigation = [
  {
    label: '工作台',
    items: [{ path: '/dashboard', label: '仪表盘', icon: DataBoard }],
  },
  {
    label: '内容管理',
    items: [
      { path: '/posts', label: '文章', icon: Document },
      { path: '/pages', label: '页面', icon: Files },
      { path: '/comments', label: '评论', icon: ChatDotRound },
      { path: '/chat', label: '站内私信', icon: ChatDotRound },
    ],
  },
  {
    label: '站点运营',
    items: [
      { path: '/categories', label: '分类', icon: CollectionTag },
      { path: '/tags', label: '标签', icon: PriceTag },
      { path: '/links', label: '友链', icon: Link },
      { path: '/projects', label: '项目', icon: FolderOpened },
      { path: '/timeline', label: '站点编年史', icon: Clock },
      { path: '/media', label: '媒体库', icon: Picture },
    ],
  },
  {
    label: '偏好设置',
    items: [{ path: '/settings', label: '站点配置', icon: Setting }],
  },
]

const pageInfo: Record<string, { title: string; description: string; section: string }> = {
  '/dashboard': { title: '仪表盘', description: '查看内容发布与读者互动概况。', section: '工作台' },
  '/posts': { title: '文章管理', description: '编辑、整理并发布博客文章。', section: '内容管理' },
  '/pages': { title: '页面管理', description: '维护关于页和自定义内容页面。', section: '内容管理' },
  '/comments': { title: '评论审核', description: '查看并处理读者提交的评论。', section: '内容管理' },
  '/chat': { title: '站内私信', description: '和读者或其他用户进行一对一实时交流。', section: '内容管理' },
  '/categories': { title: '分类管理', description: '整理文章所属的内容分类。', section: '站点运营' },
  '/tags': { title: '标签管理', description: '用标签为文章建立关联。', section: '站点运营' },
  '/links': { title: '友链管理', description: '维护博客的友情链接信息。', section: '站点运营' },
  '/projects': { title: '项目展示', description: '管理展示在站点上的项目。', section: '站点运营' },
  '/timeline': { title: '站点编年史', description: '记录站点的重要时间节点。', section: '站点运营' },
  '/media': { title: '媒体库', description: '上传并整理博客使用的图片资源。', section: '站点运营' },
  '/settings': { title: '站点配置', description: '编辑博客站点的 JSON 配置。', section: '偏好设置' },
}

const activePath = computed(() => route.path.startsWith('/posts') ? '/posts' : route.path)
const currentPage = computed(() => {
  if (route.path.startsWith('/posts/edit')) {
    return { title: route.params.id ? '编辑文章' : '写作编辑器', description: '完善标题、分类、标签与正文内容。', section: '内容管理' }
  }
  return pageInfo[route.path] ?? pageInfo['/dashboard']
})
const publicSiteUrl = import.meta.env.VITE_PUBLIC_SITE_URL || 'http://127.0.0.1:5173/'

function closeMobileMenu() {
  mobileNavOpen.value = false
}

function openPublicSite() {
  window.open(publicSiteUrl, '_blank', 'noopener,noreferrer')
}

function logout() {
  localStorage.removeItem('whaa_token')
  localStorage.removeItem('whaa_role')
  localStorage.removeItem('whaa_user_id')
  localStorage.removeItem('whaa_username')
  localStorage.removeItem('whaa_nickname')
  mobileNavOpen.value = false
  router.push('/login')
}
</script>

<template>
  <div class="admin-shell">
    <div v-if="mobileNavOpen" class="sidebar-scrim" @click="closeMobileMenu"></div>

    <aside class="admin-sidebar" :class="{ 'is-open': mobileNavOpen }">
      <div class="sidebar-brand">
        <div class="brand-mark"><el-icon><EditPen /></el-icon></div>
        <div class="brand-copy">
          <strong>WhaaBlog</strong>
          <span>管理工作台</span>
        </div>
      </div>

      <div class="sidebar-scroll">
        <section v-for="group in navigation" :key="group.label" class="sidebar-group">
          <p class="sidebar-group-title">{{ group.label }}</p>
          <el-menu
            :default-active="activePath"
            class="admin-menu"
            router
            @select="closeMobileMenu"
          >
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-menu>
        </section>
      </div>

      <div class="sidebar-bottom">
        <div class="sidebar-note">
          <span class="sidebar-note-icon"><el-icon><View /></el-icon></span>
          <div>
            <strong>查看博客</strong>
            <span>前往公开站点</span>
          </div>
          <el-icon class="note-arrow"><ArrowDown /></el-icon>
        </div>
        <el-button class="sidebar-site-button" plain @click="openPublicSite">打开站点</el-button>
      </div>
    </aside>

    <div class="admin-main-column">
      <header class="admin-topbar">
        <div class="topbar-left">
          <el-button class="mobile-menu-button" text aria-label="打开导航" @click="mobileNavOpen = true">
            <el-icon><Menu /></el-icon>
          </el-button>
          <div class="topbar-wordmark"><span class="wordmark-dot"></span>WhaaBlog <span>/</span> Admin</div>
        </div>

        <div class="topbar-right">
          <div class="topbar-profile">
            <el-avatar :size="36" class="profile-avatar">W</el-avatar>
            <div class="profile-copy">
              <strong>站点管理员</strong>
              <span>内容工作台</span>
            </div>
          </div>
          <el-button class="logout-button" text @click="logout">退出</el-button>
        </div>
      </header>

      <main class="admin-main">
        <div class="page-heading">
          <div>
            <div class="page-breadcrumb"><span>WhaaBlog</span><span class="breadcrumb-separator">/</span>{{ currentPage.section }}</div>
            <h1>{{ currentPage.title }}</h1>
            <p>{{ currentPage.description }}</p>
          </div>
          <el-button class="visit-site-button" @click="openPublicSite">
            <el-icon><View /></el-icon>
            <span>访问博客</span>
          </el-button>
        </div>

        <div class="page-route-view">
          <RouterView v-slot="{ Component }">
            <Transition name="route-fade" mode="out-in">
              <component :is="Component" />
            </Transition>
          </RouterView>
        </div>

        <footer class="admin-footer">
          <span>WhaaBlog 管理工作台</span>
          <span>让每一次记录都有自己的位置。</span>
        </footer>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  --sidebar-width: 256px;
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  background: var(--admin-canvas, #f4f7fb);
  color: var(--admin-ink, #17253b);
}

.sidebar-scrim {
  display: none;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: 24px 16px 18px;
  overflow: hidden;
  color: #e8eff8;
  background:
    radial-gradient(circle at 10% 0%, rgba(56, 120, 193, 0.2), transparent 30%),
    linear-gradient(180deg, #12243a 0%, #0d1d31 100%);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 9px 26px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 20px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 14px;
  background: linear-gradient(145deg, #438bed, #2367c7);
  box-shadow: 0 8px 20px rgba(10, 31, 60, 0.35);
}

.brand-copy,
.profile-copy {
  display: flex;
  flex-direction: column;
}

.brand-copy strong {
  color: #fff;
  font-size: 16px;
  letter-spacing: -0.02em;
}

.brand-copy span {
  margin-top: 3px;
  color: #98acc3;
  font-size: 12px;
}

.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
  padding-right: 2px;
  scrollbar-width: thin;
  scrollbar-color: rgba(153, 176, 202, 0.25) transparent;
}

.sidebar-group + .sidebar-group {
  margin-top: 19px;
}

.sidebar-group-title {
  margin: 0 10px 8px;
  color: #7f96b0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.admin-menu {
  border-right: 0;
  background: transparent;
}

.admin-menu :deep(.el-menu-item) {
  height: 42px;
  margin: 3px 0;
  padding: 0 12px !important;
  color: #b7c6d8;
  font-size: 13px;
  border-radius: 10px;
  transition: background 160ms ease, color 160ms ease, transform 160ms ease;
}

.admin-menu :deep(.el-menu-item .el-icon) {
  width: 20px;
  margin-right: 11px;
  color: #8ea6c0;
  font-size: 17px;
}

.admin-menu :deep(.el-menu-item:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.admin-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  font-weight: 600;
  background: linear-gradient(100deg, rgba(55, 126, 223, 0.32), rgba(53, 105, 174, 0.2));
  box-shadow: inset 3px 0 #67a7ff;
}

.admin-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #82b8ff;
}

.sidebar-bottom {
  padding: 17px 7px 0;
  border-top: 1px solid rgba(183, 202, 224, 0.13);
}

.sidebar-note {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 3px 13px;
}

.sidebar-note-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: #9cc9ff;
  border: 1px solid rgba(139, 184, 236, 0.22);
  border-radius: 11px;
  background: rgba(87, 145, 210, 0.12);
}

.sidebar-note div {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.sidebar-note strong {
  color: #eaf2fb;
  font-size: 12px;
}

.sidebar-note div span {
  color: #8fa5bf;
  font-size: 11px;
}

.note-arrow {
  color: #90a7c0;
  font-size: 13px;
  transform: rotate(-90deg);
}

.sidebar-site-button {
  width: 100%;
  height: 37px;
  color: #d5e6f9;
  border-color: rgba(157, 185, 215, 0.22);
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.04);
}

.sidebar-site-button:hover {
  color: #fff;
  border-color: rgba(157, 185, 215, 0.48);
  background: rgba(255, 255, 255, 0.09);
}

.admin-main-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-topbar {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(22px, 3vw, 42px);
  border-bottom: 1px solid #e7ebf1;
  background: rgba(255, 255, 255, 0.9);
}

.topbar-left,
.topbar-right,
.topbar-profile {
  display: flex;
  align-items: center;
}

.topbar-wordmark {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #29384d;
  font-size: 13px;
  font-weight: 650;
}

.topbar-wordmark > span:last-child {
  color: #a3adba;
  font-weight: 400;
}

.wordmark-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4285e8;
  box-shadow: 0 0 0 4px #eaf2ff;
}

.topbar-right {
  gap: 18px;
}

.topbar-profile {
  gap: 10px;
}

.profile-avatar {
  color: #245ea9;
  font-size: 13px;
  font-weight: 700;
  background: #e8f1ff;
}

.profile-copy strong {
  color: #26364b;
  font-size: 12px;
  font-weight: 650;
}

.profile-copy span {
  margin-top: 2px;
  color: #8995a5;
  font-size: 11px;
}

.logout-button {
  color: #707d8d;
  font-size: 12px;
}

.logout-button:hover {
  color: #d05050;
}

.mobile-menu-button {
  display: none;
}

.admin-main {
  width: 100%;
  max-width: 1600px;
  box-sizing: border-box;
  flex: 1;
  margin: 0 auto;
  padding: 30px clamp(22px, 3vw, 42px) 20px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 26px;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 9px;
  color: #8592a4;
  font-size: 12px;
  font-weight: 500;
}

.breadcrumb-separator {
  color: #c4cbd5;
}

.page-heading h1 {
  margin: 0;
  color: #18273d;
  font-size: clamp(24px, 2vw, 29px);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.2;
}

.page-heading p {
  margin: 8px 0 0;
  color: #7b8797;
  font-size: 13px;
  line-height: 1.6;
}

.visit-site-button {
  height: 39px;
  padding: 0 14px;
  color: #315f9e;
  border-color: #d6e3f3;
  border-radius: 10px;
  background: #fff;
}

.visit-site-button:hover {
  color: #1e65c2;
  border-color: #9fc4f2;
  background: #f7fbff;
}

.visit-site-button .el-icon {
  margin-right: 7px;
}

.page-route-view {
  min-height: 320px;
}

.admin-footer {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 30px 2px 8px;
  color: #99a3b1;
  font-size: 11px;
}

.route-fade-enter-active,
.route-fade-leave-active {
  transition: opacity 140ms ease, transform 140ms ease;
}

.route-fade-enter-from,
.route-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

@media (max-width: 920px) {
  .admin-shell {
    --sidebar-width: 224px;
  }

  .admin-sidebar {
    padding-right: 12px;
    padding-left: 12px;
  }
}

@media (max-width: 720px) {
  .admin-shell {
    display: block;
  }

  .admin-sidebar {
    position: fixed;
    z-index: 101;
    inset: 0 auto 0 0;
    width: 272px;
    transform: translateX(-105%);
    transition: transform 200ms ease;
    box-shadow: 16px 0 40px rgba(12, 28, 47, 0.18);
  }

  .admin-sidebar.is-open {
    transform: translateX(0);
  }

  .sidebar-scrim {
    position: fixed;
    z-index: 100;
    inset: 0;
    display: block;
    background: rgba(11, 25, 42, 0.42);
    backdrop-filter: blur(2px);
  }

  .mobile-menu-button {
    display: inline-flex;
    margin-right: 8px;
    color: #43556d;
    font-size: 18px;
  }

  .admin-topbar {
    height: 64px;
    padding: 0 18px;
  }

  .admin-main {
    padding: 25px 18px 18px;
  }

  .page-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 20px;
  }

  .admin-footer {
    flex-direction: column;
    gap: 5px;
    padding-top: 24px;
  }
}

@media (max-width: 460px) {
  .topbar-profile {
    display: none;
  }

  .topbar-right {
    gap: 6px;
  }

  .admin-main {
    padding-right: 14px;
    padding-left: 14px;
  }

  .page-heading h1 {
    font-size: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
  }
}
</style>



