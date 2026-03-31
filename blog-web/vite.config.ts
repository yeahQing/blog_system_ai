import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const __dirname = dirname(fileURLToPath(import.meta.url))
/** 与仓库根目录 VERSION 对齐；`npm run build` 时注入 */
const appVersion = readFileSync(resolve(__dirname, '../VERSION'), 'utf-8').trim()

// https://vite.dev/config/
export default defineConfig({
  define: {
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(appVersion),
  },
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
