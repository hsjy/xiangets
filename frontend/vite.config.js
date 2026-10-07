import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

// base 使用相对路径，构建产物可部署在任意子路径（GitHub Pages 项目页 / 本地预览均可用）
export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon-64.png', 'pwa-180.png'],
      manifest: {
        name: '弦歌 · 诗乐雅集',
        short_name: '弦歌',
        description: '古诗配乐欣赏——诗为经，乐为纬',
        theme_color: '#a5402d',
        background_color: '#f1ead9',
        display: 'standalone',
        start_url: './',
        lang: 'zh-CN',
        icons: [
          { src: 'pwa-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'pwa-maskable-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
          { src: 'pwa-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
        ]
      },
      workbox: {
        // 静态资源预缓存
        globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],
        // API 请求走 NetworkFirst（有网用最新，断网用缓存）
        runtimeCaching: [
          {
            urlPattern: /^\/api\/poem\//,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-poem',
              expiration: { maxEntries: 100, maxAgeSeconds: 86400 }
            }
          },
          {
            urlPattern: /\/api\/audio\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'api-audio',
              expiration: { maxEntries: 60, maxAgeSeconds: 604800 },
              rangeRequests: true
            }
          }
        ]
      }
    })
  ],
  base: './',
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
