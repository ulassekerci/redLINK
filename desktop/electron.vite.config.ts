import { defineConfig } from 'electron-vite'
import react from '@vitejs/plugin-react-swc'
import tailwindcss from '@tailwindcss/vite'
import svgr from 'vite-plugin-svgr'
import { buildVersion } from './scripts/version'

const version = buildVersion(import.meta.dirname)

// https://electron-vite.org/config/
export default defineConfig({
  main: {
    define: { __APP_VERSION__: JSON.stringify(version) },
  },
  preload: {
    // The renderer is sandboxed, and a sandboxed preload must be CommonJS.
    build: { rollupOptions: { output: { format: 'cjs', entryFileNames: '[name].cjs' } } },
  },
  renderer: {
    plugins: [react(), tailwindcss(), svgr()],
  },
})
