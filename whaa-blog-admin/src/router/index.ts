import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/LoginView.vue') },
    {
      path: '/',
      component: AdminLayout,
      meta: { auth: true },
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'posts', component: () => import('@/views/PostsView.vue') },
        { path: 'posts/edit/:id?', component: () => import('@/views/PostEditView.vue'), props: true },
        { path: 'pages', component: () => import('@/views/PagesView.vue') },
        { path: 'comments', component: () => import('@/views/CommentsView.vue') },
        { path: 'categories', component: () => import('@/views/CategoriesView.vue') },
        { path: 'tags', component: () => import('@/views/TagsView.vue') },
        { path: 'links', component: () => import('@/views/LinksView.vue') },
        { path: 'projects', component: () => import('@/views/ProjectsView.vue') },
        { path: 'timeline', component: () => import('@/views/TimelineView.vue') },
        { path: 'media', component: () => import('@/views/MediaView.vue') },
        { path: 'settings', component: () => import('@/views/SettingsView.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.auth && !localStorage.getItem('whaa_token')) {
    return '/login'
  }
  return true
})

export default router
