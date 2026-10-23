import neostandard from 'neostandard'
import pluginVue from 'eslint-plugin-vue'

export default [
  // app.js, bin/, routes/, views/, public/ are an unrelated Express skeleton left in this directory.
  { ignores: ['dist/', 'build/', 'node_modules/', 'app.js', 'bin/', 'routes/', 'views/', 'public/'] },
  ...neostandard({ files: ['**/*.js', '**/*.mjs', '**/*.vue'], env: ['browser'] }),
  ...pluginVue.configs['flat/essential']
]
