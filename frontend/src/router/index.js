import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// GitHub Pages 为纯静态托管，使用 hash 路由避免刷新 404
const routes = [
  { path: '/', redirect: '/discover' },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { bare: true } },
  { path: '/discover', name: 'discover', component: () => import('../views/DiscoverView.vue'), meta: { title: '发现音乐' } },
  { path: '/songs', name: 'songs', component: () => import('../views/SongsView.vue'), meta: { title: '歌曲库' } },
  { path: '/song/:id', name: 'songDetail', component: () => import('../views/SongDetailView.vue'), meta: { title: '歌曲详情' } },
  { path: '/singers', name: 'singers', component: () => import('../views/SingersView.vue'), meta: { title: '歌手' } },
  { path: '/singer/:id', name: 'singerDetail', component: () => import('../views/SingerDetailView.vue'), meta: { title: '歌手详情' } },
  { path: '/playlists', name: 'playlists', component: () => import('../views/PlaylistsView.vue'), meta: { title: '我的歌单', requiresAuth: true } },
  { path: '/playlist/:id', name: 'playlistDetail', component: () => import('../views/PlaylistDetailView.vue'), meta: { title: '歌单详情' } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人资料', requiresAuth: true } },
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
  document.title = to.meta.title ? `${to.meta.title} · 弦歌` : '弦歌 · 诗乐雅集'
})

export default router
