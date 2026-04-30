# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

LipidXte2 is a multi-component shotgun-lipidomics analysis pipeline. The repo is a *subtree merge* of four originally separate projects (see [`docs/contributing.md`](./docs/contributing.md) for the rewritten subtree-pull recipe). Each top-level directory has a different toolchain — pick the right one before running anything.

| Dir | Stack | Role |
| --- | --- | --- |
| `engine/` | Java 8 + JavaFX + Maven | CLI/GUI quantification & validation engine. Built as `LipidXte-1.0-SNAPSHOT-jfx.jar`. Main: `de.mpicbg.ms.MainApplication`. |
| `preprocessing/` | Python 3 (numpy, ply, numba, lxml, pandas, fisher-py) | Pre-processing scripts: `peakStrainer.py` (RAW→mzXML), `reorder.py`, `lipidXplorer2Lipidx.py`. Tests under `tests/`, pytest config in `pytest.ini`. |
| `server/` | Node + bundled jar + Python | The shipped artifact. Contains the built jar, the orchestrator (`lipidXte/process.js`), prebuilt sqlite db, the Node server (`index.js`), and the deployed web bundle under `server/web/`. |
| `desktop/` | Electron-Vue (Vue 2) | **Optional for production.** Source of the Electron desktop app and the web bundle. The prebuilt `server/web/` is what's served — rebuild here only when modifying the UI. |
| `notebooks/` | Jupyter + numpy/scipy/numba | Regression notebooks that derive the polynomials in `notebooks/polynomials/*.json` consumed by `engine/`. Not part of the runtime pipeline. |
| `deploy/` | Docker + nginx | `Dockerfile`, `docker-compose.yml`, and `nginx/` config — the deployed stack lives here. |
| `docs/` | Markdown | Chemist-oriented documentation (`description.md`, `workflow.md`) and the contributor guide (`contributing.md`). |
| `examples/` | Sample data | Small sample datasets used by the bundled docker-compose run. |

## Runtime pipeline (what actually happens at request time)

The Node server (`server/index.js`) is the entrypoint. On `/process`, it shells out via `child_process.execSync` (in `server/lipidXte/process.js`) to:

1. `python3 src/peakStrainer.py <folder>` — converts `.RAW`/`.raw` to mzXML (skipped if no RAW files). Run from `/app/preprocessing` inside the container.
2. `python3 src/reorder.py <folder>` — only runs if step 1 ran.
3. `python3 src/lipidXplorer2Lipidx.py <className> <folder>` — produces `merged.csv` (skipped if `merged.csv` already present).
4. `java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=… --merged-file=merged.csv --output-path=… --group1=… --group2=… --group3=… --quant-option=… --output-option=… <flags>` — the JavaFX app runs headless under `xvfb` to produce TSV outputs.

Status is tracked in a sqlite db (`server/db.sqlite`, tables `Batches`, `Tags`, `Batches_Tags`, `BatchesFiles`). A separate sqlite db `server/LipidXteSqlite.db` holds reference data (FA anions, etc.) consumed by the Java app via `org.xerial:sqlite-jdbc`.

When debugging "the pipeline broke", trace through `server/lipidXte/process.js` first — that's the glue. The Java jar is invoked with `--op=quant` or `--op=valid`; CLI parsing lives in `engine/src/main/java/de/mpicbg/ms/MainApplication.java` and `Pipeline.java`.

## Common commands

### Docker (primary deployment path)
```bash
# Build (called from server/)
cd server && ./build.sh             # builds lipid-server:latest from ../deploy/Dockerfile
# Compose lives in deploy/
cd deploy && docker compose build --no-cache lipidserver nginx
cd deploy && docker compose up --watch   # dev mode; nginx on :80/:443, lipidserver on :8090
```
The compose file (in `deploy/`) mounts `../download`, `../examples/sample`, and `../server/web` into the container — edit those on the host to see changes without rebuilding. The `develop.watch` block syncs `../server/lipidXte` into `/app/lipidXte` live.

### Node server (in `server/`)
```bash
cd server
yarn dev      # cross-env NODE_ENV=development PORT=8090 node index.js
yarn start    # production (reads TLS certs from /app/certs or /etc/pki/tls)
yarn test     # jest with --testTimeout=10000
npm run lint  # eslint index.js (runs automatically before `start`)
```

### Java engine (Maven)
```bash
cd engine
mvn package           # produces target/LipidXte-1.0-SNAPSHOT-jfx.jar (via javafx-maven-plugin)
# Headless run (mirrors how the server calls it):
java -jar target/LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=... --merged-file=... --output-path=...
```
Java 8 is required (see `<java.version>1.8</java.version>` in `engine/pom.xml`). The `pom.xml` references EBI and `mvnrepository.com` over plain HTTP and a pinned snapshot of `javafx-maven-plugin 8.8.4-SNAPSHOT` from oss-sonatype-snapshots — fresh checkouts may need `~/.m2/settings.xml` tweaks for HTTP repos on modern Maven.

### Preprocessing (Python)
```bash
cd preprocessing
pip install -e ".[dev]"                          # deps from pyproject.toml
pytest                                           # see pytest.ini, runs tests/
python3 src/peakStrainer.py <folder>
python3 src/reorder.py <folder>
python3 src/lipidXplorer2Lipidx.py <className> <folder>
```
Runtime deps live in `preprocessing/pyproject.toml` (`numpy`, `ply`, `numba`, `lxml`, `pandas`, `fisher-py`). The old exhaustive `requirements.txt` was removed — pyproject.toml is now the single source of truth. On macOS, GUI scripts must be run with `pythonw`, not `python`. Inside the running container the path is `/app/preprocessing`.

### Frontend (Electron-Vue) — optional
The Node server serves `server/web/` (prebuilt). Only rebuild from `desktop/` when modifying the UI:
```bash
cd desktop
npm install            # cold install ~5 min (Electron deps)
npm run dev            # hot-reload electron at localhost:9080
npm run build          # electron build via electron-builder
npm run build:web      # web build, rsyncs to ../server/web/
npm run lint:fix
```

### Pre-commit (Python sources)
The root `.pre-commit-config.yaml` runs `ruff` (selecting `E4,E7,E9,F`, ignoring `E402,F403,F405`), `black --preview` (line-length **79**), `autoflake`, `docformatter`. Python tool config (ruff/black) lives at `preprocessing/pyproject.toml`. Install once with `pre-commit install --install-hooks`. Match these in any Python edits.

## Conventions worth knowing

- **The historical `web_app/backend/` was the original source of the Node server.** It was deleted in the public-release refactor; production lives in `server/`. If you need that history, `git log --follow server/index.js` walks back into it.
- **Java jar is committed under `server/LipidXte-1.0-SNAPSHOT-jfx.jar`.** Rebuild from `engine/` and copy in if you change Java code — the Dockerfile does not run `mvn` itself.
- **Polynomials feed Java from notebooks.** `notebooks/polynomials/*.json` is regenerated by the Jupyter notebooks in `notebooks/` and consumed by `engine/`. If a quant result looks wrong, check whether the polynomial JSON or the notebook that produced it is the source of truth for the change.
- **TLS certs are read from disk paths baked into `index.js`.** Production reads `/app/certs/lipidxte.{key,pem}` (in container) or absolute host paths like `/etc/pki/tls/...` (bare-metal mode). Check `server/README.md` "Install certificates" before any cert rotation.
- **The git history is a subtree merge.** When rebasing or cherry-picking, expect overlapping histories from `lipidxte`, `backend`, `frontend`, `LipidXplorer` remotes — see [`docs/contributing.md`](./docs/contributing.md) for the rewritten `git subtree` recipe used to assemble (and update) this repo.
