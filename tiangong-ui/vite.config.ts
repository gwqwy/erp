import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5173,
    proxy: {
      // 开发环境代理到后端，生产由 Nginx 同源反代
      '/admin-api': { target: 'http://localhost:48080', changeOrigin: true },
      // IM WebSocket（ws:true 代理握手升级）
      '/ws': { target: 'http://localhost:48080', ws: true, changeOrigin: true }
    }
  }
})
