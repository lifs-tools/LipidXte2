# preprocessing — LipidXplorer pre-processing scripts

> This directory was previously named `LipidXplorer/`. It was renamed to `preprocessing/` during the public-release refactor; inside the running Docker container the path is `/app/preprocessing`.

[LipidXplorer](https://lifs.isas.de/wiki/index.php/LipidXplorer_Installation) is a tool for bottom-up and top-down shotgun-lipidomics experiments on tandem mass spectrometers. Lipid identification uses user-defined molecular fragment queries rather than a reference-spectrum database, and supports isotope-corrected quantification from MS1 or MS2 fragments.

In LipidXte2 it is used as the **first stage** of the pipeline. Three scripts in `src/` are invoked in sequence by the Node server (`server/lipidXte/process.js`):

| Script | Role |
| --- | --- |
| `peakStrainer.py` | Converts vendor `.RAW` / `.raw` files to `mzXML` (skipped if the input folder has no RAW files). |
| `reorder.py` | Reorders mzXML spectra after RAW conversion. |
| `lipidXplorer2Lipidx.py` | Runs the LipidXplorer batch and produces `merged.csv`, the input to the Java engine. |

## Install

Dependencies are declared in [`pyproject.toml`](./pyproject.toml). Runtime: `numpy`, `ply`, `numba`, `lxml`, `pandas`, `fisher-py`. Dev extra: `pytest`, `pre-commit`.

```bash
cd preprocessing
pip install -e ".[dev]"
```

The Docker image installs the same set explicitly in `deploy/Dockerfile`.

## Run

```bash
python3 src/peakStrainer.py <folder>                       # RAW -> mzXML
python3 src/reorder.py <folder>                            # reorder spectra
python3 src/lipidXplorer2Lipidx.py <className> <folder>    # batch -> merged.csv
```

On macOS, GUI scripts must be run with `pythonw`, not `python`.

## Tests

```bash
pytest                                                     # tests/
```

Test config is in `pytest.ini` (`pythonpath = src/`).

## Pre-commit

```bash
pip install pre-commit
pre-commit install --install-hooks
```

Tooling config (black `--preview`, line-length 79; ruff E4/E7/E9/F) lives in `pyproject.toml`.

## Authors

- **Ronny Herzog** — initial work
- **Jacobo Miranda Ackermann** — current upstream developer
- **Fadi Al Machot, Nils Hoffmann** — contributors
- **HongKee Moon** — LipidXte2 integration

## License

GNU GPL v2 — see [`LICENSE.md`](./LICENSE.md). Third-party licenses in [`LICENSES-third-party.md`](./LICENSES-third-party.md).

## Citing LipidXplorer

Herzog R, Schwudke D, Shevchenko A. *LipidXplorer: Software for Quantitative Shotgun Lipidomics Compatible with Multiple Mass Spectrometry Platforms.* Current Protocols in Bioinformatics, 2013 Oct 15. [PUBMED](https://www.ncbi.nlm.nih.gov/pubmed/26270171)

For LipidXte2 itself, see [`CITATION.cff`](../CITATION.cff) at the repo root.
