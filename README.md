# LipidXte2

LipidXte2 is a shotgun-lipidomics analysis pipeline that turns the fragment-intensity tables produced by [LipidXplorer](./LipidXplorer) into quantified, position-resolved fatty-acid (FA) compositions for each lipid species in a sample.

A chemist-oriented overview of the data processing is in [`core_app/doc/description.md`](./core_app/doc/description.md); the corresponding workflow diagram is in [`core_app/doc/workflow.md`](./core_app/doc/workflow.md).

## Repository layout

| Directory | Stack | Role |
| --- | --- | --- |
| [`core_app/`](./core_app) | Java 8 / JavaFX / Maven | Quantification & validation engine. Built as `LipidXte-1.0-SNAPSHOT-jfx.jar`. |
| [`LipidXplorer/`](./LipidXplorer) | Python 3 | Pre-processing: `peakStrainer.py` (RAW → mzXML), `reorder.py`, `lipidXplorer2Lipidx.py`. |
| [`web_app/backend/`](./web_app/backend) | Node.js + Express + SQLite | Original `LipidXteServer` source. |
| [`web_app/frontend/`](./web_app/frontend) | Electron-Vue (Vue 2) | Desktop UI and web build. |
| [`docker-build/`](./docker-build) | Node + bundled jar + Python | The shipped artifact: orchestrator, prebuilt SQLite DB, deployed web bundle. |
| [`web/`](./web) | Static (prebuilt) | Compiled frontend served by the Node server. |
| [`minterpy/`](./minterpy) | Jupyter / numpy / scipy | Regression notebooks that derive the polynomials in `minterpy/polynomials/*.json` consumed by `core_app`. |

> Production deployment runs from `docker-build/`; `web_app/backend/` is the historical source and has drifted from it. See [`CLAUDE.md`](./CLAUDE.md) for the full picture.

## Pipeline at a glance

The Node server (`docker-build/index.js`) is the entry point. On `/process`, it shells out to:

1. `python3 src/peakStrainer.py <folder>` — converts `.RAW`/`.raw` to mzXML (only if RAW files are present).
2. `python3 src/reorder.py <folder>` — reorders spectra after RAW conversion.
3. `python3 src/lipidXplorer2Lipidx.py <className> <folder>` — produces `merged.csv`.
4. `java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant …` — runs the JavaFX engine headless under `xvfb` and writes TSV results.

Status is tracked in `docker-build/db.sqlite`; reference data (FA anions, etc.) lives in `docker-build/LipidXteSqlite.db`.

## Build & run

### Docker (primary)

```bash
docker compose build --no-cache lipidserver nginx
docker compose up --watch
```

`nginx` listens on `:80`/`:443`, the lipidserver container on `:8090`. The compose file mounts `./docker-build/download`, `./docker-build/sample`, and `./web` into the container — host-side edits are visible without rebuild.

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

### Frontend (Electron-Vue)

```bash
cd web_app/frontend
npm run dev                # hot-reload electron at localhost:9080
npm run build              # electron build
npm run build:web          # web build, rsynced into the server's web/
```

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
