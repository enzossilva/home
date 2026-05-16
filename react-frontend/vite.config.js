import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const isDev = process.env.NODE_ENV !== 'production';

export default defineConfig(async () => {
  const plugins = [react()];

  if (isDev) {
    const { default: basicSsl } = await import('@vitejs/plugin-basic-ssl');
    plugins.push(basicSsl());
  }

  return {
    plugins,
    server: {
      port: 3000,
      https: isDev,
      proxy: {
        '/products': 'http://localhost:8080',
        '/users': 'http://localhost:8080',
        '/cart': 'http://localhost:8080',
        '/payment': 'http://localhost:8080',
        '/orders': 'http://localhost:8080',
        '/frete': 'http://localhost:8080',
      }
    }
  };
});
