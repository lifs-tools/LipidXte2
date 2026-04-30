# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

LipidXte2 is a multi-component shotgun-lipidomics analysis pipeline. The repo is a *subtree merge* of four originally separate projects (see `web_app/backend/README.md` "git subtree" section). Each top-level directory has a different toolchain — pick the right one before running anything.

| Dir | Stack | Role |
| --- | --- | --- |
| `core_app/` | Java 8 + JavaFX + Maven | CLI/GUI quantification & validation engine. Built as `LipidXte-1.0-SNAPSHOT-jfx.jar`. Main: `de.mpicbg.ms.MainApplication`. |
| `LipidXplorer/` | Python 3 (numpy, ply, numba, lxml, pandas, fisher-py) | Pre-processing scripts: `peakStrainer.py` (RAW→mzXML), `reorder.py`, `lipidXplorer2Lipidx.py`. Tests under `tests/`, pytest config in `pytest.ini`. |
| `web_app/backend/` | Node.js + Express + sqlite3 | Original `LipidXteServer` source. **Production deployment uses `docker-build/index.js`, not this directory** — they have diverged. |
| `web_app/frontend/` | Electron-Vue (Vue 2) | Desktop UI + web build. `npm run build:web` rsyncs to LipidXteServer's `web/`. |
| `docker-build/` | Node + bundled jar + Python | The actual shipped artifact. Contains the built jar, the orchestrator (`lipidXte/process.js`), prebuilt sqlite db, and the deployed web bundle. |
| `web/` | Static (prebuilt) | Compiled frontend output served by the Node server. Don't hand-edit. |
| `minterpy/` | Jupyter + numpy/scipy/numba | Regression notebooks that derive the polynomials in `minterpy/polynomials/*.json` consumed by `core_app`. Not part of the runtime pipeline. |

## Runtime pipeline (what actually happens at request time)

The Node server (`docker-build/index.js`) is the entrypoint. On `/process`, it shells out via `child_process.execSync` (in `docker-build/lipidXte/process.js`) to:

1. `python3 src/peakStrainer.py <folder>` — converts `.RAW`/`.raw` to mzXML (skipped if no RAW files).
2. `python3 src/reorder.py <folder>` — only runs if step 1 ran.
3. `python3 src/lipidXplorer2Lipidx.py <className> <folder>` — produces `merged.csv` (skipped if `merged.csv` already present).
4. `java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=… --merged-file=merged.csv --output-path=… --group1=… --group2=… --group3=… --quant-option=… --output-option=… <flags>` — the JavaFX app runs headless under `xvfb` to produce TSV outputs.

Status is tracked in a sqlite db (`docker-build/db.sqlite`, tables `Batches`, `Tags`, `Batches_Tags`, `BatchesFiles`). A separate sqlite db `docker-build/LipidXteSqlite.db` holds reference data (FA anions, etc.) consumed by the Java app via `org.xerial:sqlite-jdbc`.

When debugging "the pipeline broke", trace through `docker-build/lipidXte/process.js` first — that's the glue. The Java jar is invoked with `--op=quant` or `--op=valid`; CLI parsing lives in `core_app/src/main/java/de/mpicbg/ms/MainApplication.java` and `Pipeline.java`.

## Common commands

### Docker (primary deployment path)
```bash
# Build (called from docker-build/)
./build.sh                          # builds lipid-server:latest from ../Dockerfile
docker compose build --no-cache lipidserver nginx
docker compose up --watch           # dev mode; nginx on :80/:443, lipidserver on :8090
```
The compose file mounts `./docker-build/download`, `./docker-build/sample`, and `./web` into the container — edit those on the host to see changes without rebuilding. The `develop.watch` block syncs `./docker-build/lipidXte` into `/app/lipidXte` live.

### Node server (in `docker-build/` or `web_app/backend/`)
```bash
yarn dev      # cross-env NODE_ENV=development PORT=8090 node index.js
yarn start    # production (reads TLS certs from /app/certs or /etc/pki/tls)
yarn test     # jest with --testTimeout=10000
npm run lint  # eslint index.js (runs automatically before `start`)
```

### Java core_app (Maven)
```bash
cd core_app
mvn package           # produces target/LipidXte-1.0-SNAPSHOT-jfx.jar (via javafx-maven-plugin)
# Headless run (mirrors how the server calls it):
java -jar target/LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=... --merged-file=... --output-path=...
```
Java 8 is required (see `<java.version>1.8</java.version>` in `core_app/pom.xml`). The `pom.xml` references EBI and `mvnrepository.com` over plain HTTP and a pinned snapshot of `javafx-maven-plugin 8.8.4-SNAPSHOT` from oss-sonatype-snapshots — fresh checkouts may need `~/.m2/settings.xml` tweaks for HTTP repos on modern Maven.

### LipidXplorer (Python)
```bash
cd LipidXplorer
pytest                                           # see pytest.ini, runs tests/
python3 src/peakStrainer.py <folder>
python3 src/reorder.py <folder>
python3 src/lipidXplorer2Lipidx.py <className> <folder>
```
On macOS, GUI scripts must be run with `pythonw`, not `python` (per `LipidXplorer/README.md`).

### Frontend (Electron-Vue)
```bash
cd web_app/frontend
npm run dev            # hot-reload electron at localhost:9080
npm run build          # electron build via electron-builder
npm run build:web      # web build, rsyncs to ../../../LipidXteServer/web/
npm run lint:fix
```

### Pre-commit (Python sources)
`.pre-commit-config.yaml` runs `ruff` (selecting `E4,E7,E9,F`, ignoring `E402,F403,F405`), `black --preview` (line-length **79**), `autoflake`, `docformatter`. Install once with `pre-commit install --install-hooks`. Match these in any Python edits.

## Conventions worth knowing

- **Two copies of the Node server exist.** `web_app/backend/index.js` is the historical source; `docker-build/index.js` is what ships. They have drifted. When fixing server behaviour, edit `docker-build/index.js` (and the `lipidXte/process.js` orchestrator) — that's what the Dockerfile copies and what `docker compose` runs. Don't assume changes to `web_app/backend/` reach production.
- **Java jar is committed under `docker-build/LipidXte-1.0-SNAPSHOT-jfx.jar`** (and its mirror in `web_app/backend/`). Rebuild from `core_app/` and copy in if you change Java code — the Dockerfile does not run `mvn` itself.
- **Polynomials feed Java from notebooks.** `minterpy/polynomials/*.json` is regenerated by the Jupyter notebooks in `minterpy/` and consumed by `core_app`. If a quant result looks wrong, check whether the polynomial JSON or the notebook that produced it is the source of truth for the change.
- **TLS certs are read from disk paths baked into `index.js`.** Production reads `/app/certs/lipidxte.{key,pem}` (in container) or absolute host paths like `/etc/pki/tls/...` (bare-metal mode). Check `web_app/backend/README.md` "Install certificates" before any cert rotation.
- **The git history is a subtree merge.** When rebasing or cherry-picking, expect overlapping histories from `lipidxte`, `backend`, `frontend`, `LipidXplorer` remotes — see `web_app/backend/README.md` for the original `git subtree` recipe used to assemble this repo.
