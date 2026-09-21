import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Frontend-ul cheamă /api/*; Vite scoate prefixul și trimite cererea către Spring pe :8080.
// Astfel browserul vede o singură origine și nu avem nevoie de CORS.
const backend = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, ''),
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: { proxy: backend },
  preview: { proxy: backend },
})
