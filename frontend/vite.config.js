import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// base 使用相对路径，构建产物可部署在任意子路径（GitHub Pages 项目页 / 本地预览均可用）
export default defineConfig({
  plugins: [vue()],
  base: './',
  server: {
    port: 5173,
    proxy: {
      // 联调真实 SpringBoot 后端时使用：npm run dev -- --mode real
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
