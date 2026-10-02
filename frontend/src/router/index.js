import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// GitHub Pages 为纯静态托管，使用 hash 路由避免刷新 404
const routes = [
  { path: '/', redirect: '/today' },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { bare: true } },

  /* 诗为主体 */
  { path: '/today', name: 'today', component: () => import('../views/TodayView.vue'), meta: { title: '今日一诗' } },
  { path: '/poems', name: 'poems', component: () => import('../views/PoemsView.vue'), meta: { title: '诗选' } },
  { path: '/poem/:id', name: 'poemDetail', component: () => import('../views/PoemDetailView.vue'), meta: { title: '意境' } },
  { path: '/poets', name: 'poets', component: () => import('../views/PoetsView.vue'), meta: { title: '诗人' } },
  { path: '/poet/:id', name: 'poetDetail', component: () => import('../views/PoetDetailView.vue'), meta: { title: '诗人' } },
  { path: '/study', name: 'study', component: () => import('../views/StudyView.vue'), meta: { title: '书斋', requiresAuth: true } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人资料', requiresAuth: true } },

  /* 旧音乐站路由 → 新 IA 重定向 */
  { path: '/discover', redirect: '/today' },
  { path: '/songs', redirect: '/poems' },
  { path: '/singers', redirect: '/poets' },
  { path: '/playlists', redirect: '/study' },
  { path: '/song/:id', redirect: (to) => '/poem/' + to.params.id },
  { path: '/playlist/:id', redirect: '/study' },

  { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('../views/NotFoundView.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !useUserStore().isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  document.title = to.meta.title ? to.meta.title + ' · 弦歌' : '弦歌 · 诗乐雅集'
})

export default router
