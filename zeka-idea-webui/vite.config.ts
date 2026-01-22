import {defineConfig, type Plugin} from 'vite'
import react from '@vitejs/plugin-react'
import type {IncomingMessage, ServerResponse} from 'http'

// Plugin to add custom middleware
function versionPlugin(): Plugin {
    return {
        name: 'version-plugin',
        configureServer(server) {
            server.middlewares.use('/api/plugin/version', (_req: IncomingMessage, res: ServerResponse) => {
                res.setHeader('Content-Type', 'text/plain');
                res.end('2026.1.dev');
            });
        }
    };
}

// https://vite.dev/config/
export default defineConfig({
    plugins: [react(), versionPlugin()],
    server: {
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true,
                // 不再重写路径，保留 /api 前缀以匹配后端 context-path
            }
        }
    }
})