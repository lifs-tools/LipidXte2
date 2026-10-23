# LipidXte2

LipidXte2 is a shotgun-lipidomics analysis pipeline that turns the fragment-intensity tables produced by [LipidXplorer](./LipidXplorer) into quantified, position-resolved fatty-acid (FA) compositions for each lipid species in a sample.

A chemist-oriented overview of the data processing is in [`core_app/doc/description.md`](./core_app/doc/description.md); the corresponding workflow diagram is in [`core_app/doc/workflow.md`](./core_app/doc/workflow.md).

## Repository layout

| Directory | Stack | Role |
| --- | --- | --- |
| [`core_app/`](./core_app) | Java 8 / JavaFX / Maven | Quantification & validation engine. Built as `LipidXte-1.0-SNAPSHOT-jfx.jar`. |
| [`LipidXplorer/`](./LipidXplorer) | Python 3 | Pre-processing: `peakStrainer.py` (RAW → mzXML), `reorder.py`, `lipidXplorer2Lipidx.py`. |
| [`web_app/backend/`](./web_app/backend) | Node.js + Express + SQLite | Original `LipidXteServer` checkout; carries an identical copy of the server source in `docker-build/`. |
| [`web_app/frontend/`](./web_app/frontend) | Vue 3 + Vite | Web UI; `yarn build:web` writes the bundle into `docker-build/web/`. |
| [`docker-build/`](./docker-build) | Node + bundled jar + Python | The shipped artifact: orchestrator, prebuilt SQLite DB, deployed web bundle. |
| [`web/`](./web) | Static (prebuilt) | Compiled frontend served by the Node server. |
| [`minterpy/`](./minterpy) | Jupyter / numpy / scipy | Regression notebooks that derive the polynomials in `minterpy/polynomials/*.json` consumed by `core_app`. |

> Production deployment runs from `docker-build/`. `web_app/backend/` carries the same server files (`index.js`, `lipidXte/`, `package.json`, `yarn.lock`, `tests/`, `www/`, jar, database); `docker-build/tests/backend-copy.test.js` fails as soon as the two copies differ, so after editing one directory copy the changed files to the other. See [`CLAUDE.md`](./CLAUDE.md) for the full picture.

## Pipeline at a glance

The Node server (`docker-build/index.js`) is the entry point. On `/process`, it shells out to:

1. `python3 src/peakStrainer.py <folder>` — converts `.RAW`/`.raw` to mzXML (only if RAW files are present).
2. `python3 src/reorder.py <folder>` — reorders spectra after RAW conversion.
3. `python3 src/lipidXplorer2Lipidx.py <className> <folder>` — produces `merged.csv`.
4. `java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant …` — runs the JavaFX engine headless under `xvfb` and writes TSV results.

Status is tracked in `docker-build/download/db.sqlite` (created on first start); reference data (FA anions, etc.) lives in `docker-build/LipidXteSqlite.db`.

## Build & run

### Docker (primary)

```bash
docker compose build --no-cache lipidserver nginx
docker compose up --watch
```

`nginx` listens on `:80`/`:443`, the lipidserver container on `:8090`. The compose file mounts `./docker-build/download`, `./docker-build/sample`, and `./web` into the container — host-side edits are visible without rebuild.

The Node.js runtime comes from the official `node:24.22.1-bookworm-slim` image (first `FROM` in `Dockerfile`), not from Debian's apt package, which is stuck on the end-of-life Node 18. When Node.js publishes a security release, bump that tag and rebuild. Server dependencies are installed from the committed `docker-build/yarn.lock` with `yarn workspaces focus --production` (runtime dependencies only; `enableImmutableInstalls` in `docker-build/.yarnrc.yml` makes the build fail if the lockfile is out of date). The Yarn version is pinned by `packageManager` in `docker-build/package.json`. After editing `docker-build/package.json`, regenerate the lockfile without installing anything locally:

```bash
cd docker-build && corepack yarn install --mode=update-lockfile
```

Server tests (jest + supertest, in `docker-build/tests/`) run on the host with Node 24 or newer; they need the dev dependencies, which stay out of the image:

```bash
cd docker-build && corepack yarn install && yarn test
```

The tests write their scratch batches and the status database to `docker-build/download/`, which is ignored by git and by the image build.

To rebuild the image directly:

```bash
cd docker-build && ./build.sh         # builds lipid-server:latest
```

### Java engine

```bash
cd core_app
mvn package
java -jar target/LipidXte-1.0-SNAPSHOT-jfx.jar \
    --op=quant \
    --standard-list=<standard_list.csv> \
    --merged-file=<merged.csv> \
    --output-path=<out_dir>
```

Java 8 is required (see `<java.version>1.8</java.version>` in `core_app/pom.xml`). The committed jar under `docker-build/LipidXte-1.0-SNAPSHOT-jfx.jar` is what the Docker image ships — rebuild from `core_app/` and copy it in if you change Java code.

### Node server

```bash
cd docker-build      # or web_app/backend
yarn dev             # development on PORT=8090
yarn start           # production (TLS certs read from /app/certs or /etc/pki/tls)
yarn test            # jest
```

### LipidXplorer (Python)

```bash
cd LipidXplorer
pytest                                          # tests/
python3 src/peakStrainer.py <folder>
python3 src/reorder.py <folder>
python3 src/lipidXplorer2Lipidx.py <className> <folder>
```

On macOS, GUI scripts must be run with `pythonw`.

### Frontend (Vue 3 + Vite)

```bash
cd web_app/frontend
corepack enable            # Yarn 4, pinned by "packageManager"
yarn install
yarn dev                   # dev server at localhost:9080, API calls go to the Node server on :8090
yarn build:web             # production bundle, rsynced into docker-build/web/
yarn lint
```

The Electron desktop build was removed in October 2026 (Vue 2 and Electron 30 were end-of-life); only the web build remains.

## Development

Install pre-commit hooks (ruff, black `--preview`, autoflake, docformatter — Python line-length 79):

```bash
pip install pre-commit
pre-commit install --install-hooks
```

## Authors

- Kai Schuhmann — initial ideas and work
- HongKee Moon — programming
- See `LipidXplorer/README.md` for the upstream LipidXplorer authors.

## License

GNU GPL v2 — see [`LipidXplorer/LICENSE.md`](./LipidXplorer/LICENSE.md).
