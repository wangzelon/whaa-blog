import { createRouter, createWebHistory } from 'vue-router'
import SiteLayout from '@/layouts/SiteLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: SiteLayout,
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
        { path: 'messages', name: 'messages', component: () => import('@/views/ChatView.vue'), meta: { auth: true } },
        { path: 'posts/:slug', name: 'post', component: () => import('@/views/PostView.vue') },
        { path: 'categories', name: 'categories', component: () => import('@/views/CategoriesView.vue') },
        { path: 'categories/:slug', name: 'category', component: () => import('@/views/CategoryPostsView.vue') },
        { path: 'tags', name: 'tags', component: () => import('@/views/TagsView.vue') },
        { path: 'tags/:slug', name: 'tag', component: () => import('@/views/TagPostsView.vue') },
        { path: 'archives', name: 'archives', component: () => import('@/views/ArchivesView.vue') },
        { path: 'projects', name: 'projects', component: () => import('@/views/ProjectsView.vue') },
        { path: 'links', name: 'links', component: () => import('@/views/LinksView.vue') },
        { path: 'albums', name: 'albums', component: () => import('@/views/AlbumsView.vue') },
        { path: 'albums/:slug', name: 'album', component: () => import('@/views/AlbumView.vue') },
        { path: 'about', name: 'about', component: () => import('@/views/PageView.vue'), props: { slug: 'about' } },
        { path: 'about/site', name: 'about-site', component: () => import('@/views/AboutSiteView.vue') },
        { path: 'pages/:slug', name: 'page', component: () => import('@/views/PageView.vue'), props: true },
        { path: 'search', name: 'search', component: () => import('@/views/SearchView.vue') },
      ],
    },
    { path: '/login', name: 'login', component: () => import('@/views/AuthView.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/AuthView.vue') },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  if (to.meta.auth && (!localStorage.getItem('whaa_token') || !localStorage.getItem('whaa_user'))) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
