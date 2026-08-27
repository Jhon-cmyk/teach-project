import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return

          if (
            id.includes('/ant-design-vue/') ||
            id.includes('/@ant-design/icons-vue/')
          ) {
            return 'vendor-ant-design'
          }
          if (id.includes('/echarts/') || id.includes('/zrender/')) return 'vendor-echarts'
          if (
            id.includes('/@codemirror/') ||
            id.includes('/codemirror/') ||
            id.includes('/@lezer/')
          ) {
            return 'vendor-codemirror'
          }
          if (id.includes('/@vue-flow/')) return 'vendor-vue-flow'
          if (id.includes('/@wangeditor/') || id.includes('/slate')) return 'vendor-editor'
          if (
            id.includes('/html2pdf.js/') ||
            id.includes('/html2canvas/') ||
            id.includes('/jspdf/')
          ) {
            return 'vendor-pdf'
          }
          if (id.includes('/pptxgenjs/')) return 'vendor-pptx'
          if (id.includes('/xlsx/')) return 'vendor-xlsx'
          if (id.includes('/markdown-it/') || id.includes('/highlight.js/')) {
            return 'vendor-markdown'
          }
          if (
            id.includes('/vue/') ||
            id.includes('/vue-router/') ||
            id.includes('/pinia/') ||
            id.includes('/@vue/')
          ) {
            return 'vendor-vue'
          }
        },
      },
    },
  },
  server: {
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate, max-age=0',
      Pragma: 'no-cache',
      Expires: '0',
    },
    proxy: {
      '/ai': {
        target: 'http://localhost:8820',  // ← 改成你 Spring Boot 实际端口
        changeOrigin: true,
        rewrite: (path) => '/api' + path,
      },
    },
  },
})
