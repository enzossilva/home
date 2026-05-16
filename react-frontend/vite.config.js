import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import basicSsl from '@vitejs/plugin-basic-ssl'

export default defineConfig({
  plugins: [react(), basicSsl()],
  server: {
    port: 3000,
    https: true,
    proxy: {
      '/products': 'http://localhost:8080',
      '/users': 'http://localhost:8080',
      '/cart': 'http://localhost:8080',
      '/payment': 'http://localhost:8080',
      '/orders': 'http://localhost:8080',
      '/frete': 'http://localhost:8080',
    }
  }
})
