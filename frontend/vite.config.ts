import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// При разработке (npm run dev) сайт открывается на http://localhost:5173,
// а запросы /api перенаправляются на бэкенд http://localhost:8080.
// В готовом приложении сайт и API отдаёт один сервер (ZettaBilling) — адреса те же.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  },
  build: {
    outDir: 'dist'
  }
});
