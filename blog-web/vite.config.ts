import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    // Element Plus 全量注册 + md-editor 单包约 0.8–1.1MB，属预期体积；已用 manualChunks 拆出 axios 等
    chunkSizeWarningLimit: 1300,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          // @element-plus/icons-vue 路径里也含 "element-plus"，一并打进 UI 包
          if (id.includes('element-plus')) return 'element-plus'
          if (id.includes('md-editor-v3')) return 'md-editor'
          if (id.includes('axios')) return 'axios'
        },
      },
    },
  },
})
