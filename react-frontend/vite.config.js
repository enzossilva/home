import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/products': 'http://localhost:8080',
      '/users': 'http://localhost:8080',
      '/cart': 'http://localhost:8080',
      '/payment': 'http://localhost:8080',
      '/orders': 'http://localhost:8080',
      '/frete': 'http://localhost:8080',
      '/lookbook': 'http://localhost:8080',
      '/videos': 'http://localhost:8080',
    }
  }
})
