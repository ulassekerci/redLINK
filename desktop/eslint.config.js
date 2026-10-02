import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { globalIgnores } from 'eslint/config'
import { builtinModules } from 'node:module'

export default tseslint.config([
  globalIgnores(['dist', 'out']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs['recommended-latest'],
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
  },
  {
    files: ['electron.vite.config.ts', 'scripts/**/*.ts', 'src/main/**/*.ts', 'src/preload/**/*.ts'],
    languageOptions: { globals: globals.node },
  },
  {
    // The protocol module is shared by main and the renderer, so it may import
    // nothing from Electron or Node. tsconfig.protocol.json keeps the DOM out.
    files: ['src/protocol/**/*.ts'],
    ignores: ['src/protocol/**/*.test.ts'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['electron', 'electron/*', 'node:*', ...builtinModules],
              message: 'The protocol module imports nothing from Electron or Node.',
            },
          ],
        },
      ],
    },
  },
])
