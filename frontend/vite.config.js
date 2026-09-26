import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// Dezvoltare locală fără VITE_API_URL: frontend-ul cheamă /api/*, iar Vite scoate prefixul și trimite
// cererea către Spring pe :8080. Browserul vede o singură origine, deci nu intervine CORS.
// Cu VITE_API_URL setat (ex. în producție), api.js cheamă direct backend-ul și proxy-ul nu mai e folosit.
const backend = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, ''),
  },
}

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')

  if (command === 'build' && !env.VITE_API_URL) {
    // Pe Vercel un build fără VITE_API_URL ar produce un site care cheamă /api pe propriul domeniu (404).
    if (process.env.VERCEL) {
      throw new Error('VITE_API_URL nu e setat. Adaug-o în Vercel → Settings → Environment Variables (URL-ul backend-ului de pe Render).')
    }
    console.warn('\n⚠ VITE_API_URL nu e setat: build-ul va chema /api (merge doar cu `npm run preview`, prin proxy).\n')
  }

  return {
    plugins: [react()],
    server: { proxy: backend },
    preview: { proxy: backend },
  }
})
