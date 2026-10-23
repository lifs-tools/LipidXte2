# LipidXte web frontend

Vue 3 + Vite single-page application. The production bundle is served by the Node server in `../../docker-build/` (`express.static('web')`).

```bash
corepack enable      # Yarn 4, version pinned by "packageManager" in package.json
yarn install
yarn dev             # http://localhost:9080; API calls go to a server on :8090 (`yarn dev` in docker-build/)
yarn build:web       # builds dist/web/ and syncs it into ../../docker-build/web/
yarn lint
```

Layout: `src/components/` holds the five pages (Data Import, Result View, Samples, MS2 calculator, Help) plus the modal and dropzone wrappers; `src/assets/` holds images, Bootstrap 3 CSS and its fonts; `static/dat/` holds the sample RAW archives linked from the Help page and is copied verbatim into the bundle.

Until October 2026 this directory was an electron-vue project (Vue 2 + Electron 30). The desktop build was dropped together with that toolchain; only the web build remains.
