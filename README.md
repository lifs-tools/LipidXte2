# LipidXte2

LipidXte2 is a shotgun-lipidomics analysis pipeline that turns the fragment-intensity tables produced by [LipidXplorer](./preprocessing) into quantified, position-resolved fatty-acid (FA) compositions for each lipid species in a sample.

A chemist-oriented overview of the data processing is in [`docs/description.md`](./docs/description.md); the corresponding workflow diagram is in [`docs/workflow.md`](./docs/workflow.md).

## Repository layout

| Directory | Stack | Role |
| --- | --- | --- |
| [`engine/`](./engine) | Java 8 / JavaFX / Maven | Quantification & validation engine. Built as `LipidXte-1.0-SNAPSHOT-jfx.jar`. |
| [`preprocessing/`](./preprocessing) | Python 3 | Pre-processing: `peakStrainer.py` (RAW → mzXML), `reorder.py`, `lipidXplorer2Lipidx.py`. |
| [`server/`](./server) | Node + bundled jar + Python | The shipped artifact: orchestrator (`lipidXte/process.js`), prebuilt SQLite DB, deployed web bundle under `server/web/`. |
| [`desktop/`](./desktop) | Electron-Vue (Vue 2) | Desktop UI and web build. `npm run build:web` rebuilds the static bundle into `server/web/`. |
| [`notebooks/`](./notebooks) | Jupyter / numpy / scipy | Regression notebooks that derive the polynomials in `notebooks/polynomials/*.json` consumed by `engine/`. |
| [`deploy/`](./deploy) | Docker / nginx | `Dockerfile`, `docker-compose.yml`, and `nginx/` config for the deployed stack. |
| [`docs/`](./docs) | Markdown | Chemist-oriented documentation and contributor guide. |
| [`examples/`](./examples) | Sample data | Small sample datasets used by the bundled docker-compose run. |

See [`CLAUDE.md`](./CLAUDE.md) for a deeper map of the codebase.

## Pipeline at a glance

The Node server (`server/index.js`) is the entry point. On `/process`, it shells out to:

1. `python3 src/peakStrainer.py <folder>` — converts `.RAW`/`.raw` to mzXML (only if RAW files are present).
2. `python3 src/reorder.py <folder>` — reorders spectra after RAW conversion.
3. `python3 src/lipidXplorer2Lipidx.py <className> <folder>` — produces `merged.csv`.
4. `java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant …` — runs the JavaFX engine headless under `xvfb` and writes TSV results.

Status is tracked in `server/db.sqlite`; reference data (FA anions, etc.) lives in `server/LipidXteSqlite.db`.

## Build & run

### Docker (primary)

```bash
cd deploy && docker compose build --no-cache lipidserver nginx
cd deploy && docker compose up --watch
```

`nginx` listens on `:80`/`:443`, the lipidserver container on `:8090`. The compose file mounts `../download`, `../examples/sample`, and `../server/web` into the container — host-side edits are visible without rebuild. The `develop.watch` block syncs `../server/lipidXte` into `/app/lipidXte` live.

To rebuild the image directly:

```bash
cd server && ./build.sh         # builds lipid-server:latest
```

### Java engine

```bash
cd engine
mvn package
java -jar target/LipidXte-1.0-SNAPSHOT-jfx.jar \
    --op=quant \
    --standard-list=<standard_list.csv> \
    --merged-file=<merged.csv> \
    --output-path=<out_dir>
```

Java 8 is required (see `<java.version>1.8</java.version>` in `engine/pom.xml`). The committed jar under `server/LipidXte-1.0-SNAPSHOT-jfx.jar` is what the Docker image ships — rebuild from `engine/` and copy it in if you change Java code.

### Node server

```bash
cd server
yarn dev             # development on PORT=8090
yarn start           # production (TLS certs read from /app/certs or /etc/pki/tls)
yarn test            # jest
```

### LipidXplorer (Python)

```bash
cd preprocessing
pytest                                          # tests/
python3 src/peakStrainer.py <folder>
python3 src/reorder.py <folder>
python3 src/lipidXplorer2Lipidx.py <className> <folder>
```

On macOS, GUI scripts must be run with `pythonw`.

### Frontend (Electron-Vue)

```bash
cd desktop
npm run dev                # hot-reload electron at localhost:9080
npm run build              # electron build
npm run build:web          # web build, rsynced into server/web/
```

## Development

Install pre-commit hooks (ruff, black `--preview`, autoflake, docformatter — Python line-length 79):

```bash
pip install pre-commit
pre-commit install --install-hooks
```

See [`docs/contributing.md`](./docs/contributing.md) for the upstream subtree-pull recipe and other contributor notes.

## Authors

- Kai Schuhmann — initial ideas and work
- HongKee Moon — programming
- See `preprocessing/README.md` for the upstream LipidXplorer authors.

## License

GNU GPL v2 — see [`preprocessing/LICENSE.md`](./preprocessing/LICENSE.md).
