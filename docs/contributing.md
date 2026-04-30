# Contributing to LipidXte2

## Repository layout

LipidXte2 is a monorepo. Top-level directories are split by intent:

- `engine/` — Java/JavaFX quantification & validation engine (Maven, Java 8). Builds the `LipidXte-1.0-SNAPSHOT-jfx.jar` consumed by the server.
- `preprocessing/` — Python 3 pre-processing scripts: `peakStrainer.py` (RAW → mzXML), `reorder.py`, `lipidXplorer2Lipidx.py`. Pytest suite under `preprocessing/tests/`.
- `server/` — Node.js + Express + SQLite server. The shipped artifact: contains the deployed jar, prebuilt sqlite reference DB, and the `lipidXte/process.js` orchestrator that shells out to Python and Java.
- `desktop/` — Electron-Vue (Vue 2) desktop UI. `npm run build:web` rebuilds the static bundle into `server/web/`.
- `notebooks/` — Jupyter regression notebooks that derive the polynomials in `notebooks/polynomials/*.json` consumed by `engine/`.
- `deploy/` — `Dockerfile`, `docker-compose.yml`, and `nginx/` config.
- `docs/` — chemist-oriented documentation.
- `examples/` — small sample datasets used by the bundled docker-compose run.

## History

This repo is a subtree merge of four originally separate projects:

| Public path | Historical remote |
| --- | --- |
| `engine/` | `lipidxte/master` |
| `server/` | `backend/master` (was `LipidXteServer`; the duplicate `web_app/backend/` was removed in the public-release refactor) |
| `desktop/` | `frontend/master` (was `LipidXteWeb`) |
| `preprocessing/` | `LipidXplorer/main` (`lifs-tools/LipidXplorer`) |

To pull updates from upstream `lifs-tools/LipidXplorer` after the rename:

```bash
git remote add -f LipidXplorer git@git.mpi-cbg.de:scicomp/scidev_team/lipidxplorer_legacy.git
git pull -s subtree -X subtree=preprocessing LipidXplorer main
```

For the other components (now defunct internal remotes), the original recipe was:

```bash
git remote add -f lipidxte git@git.mpi-cbg.de:scicomp/scidev_team/MassSpec.git
git remote add -f backend  git@git.mpi-cbg.de:scicomp/scidev_team/LipidXteServer.git
git remote add -f frontend git@git.mpi-cbg.de:scicomp/scidev_team/LipidXteWeb.git
git pull -s subtree -X subtree=engine        lipidxte master
git pull -s subtree -X subtree=server        backend  master
git pull -s subtree -X subtree=desktop       frontend master
```

## Pre-commit

Python sources go through `ruff` (E4/E7/E9/F, ignoring E402/F403/F405), `black --preview` (line-length 79), `autoflake`, and `docformatter`. Install once:

```bash
pip install pre-commit
pre-commit install --install-hooks
```

## Reporting issues

Please file issues on the public GitHub repository. Include the failing command, the relevant log section (server logs are in `server/log.txt` when running locally), and the dataset characteristics (mass-spec instrument, NCE used, lipid classes targeted) when reporting quantification anomalies.
