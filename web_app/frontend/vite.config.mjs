import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // Copied verbatim into the bundle: the sample RAW archives linked from the Help page (dat/).
  publicDir: 'static',
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  // Some plotly.js dependencies read Node's `global`; point it at the browser global object.
  define: { global: 'globalThis' },
  server: { port: 9080 },
  build: {
    outDir: 'dist/web',
    emptyOutDir: true,
    chunkSizeWarningLimit: 2500 // plotly.js alone is about 1 MB minified
  }
})
