import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      // 개발 중에는 프록시로 백엔드에 붙는다.
      // 같은 오리진으로 보이므로 CORS 설정이 필요 없고, SSE도 그대로 통과한다.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
