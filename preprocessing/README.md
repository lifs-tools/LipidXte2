![pipeline](https://git.mpi-cbg.de/scicomp/scidev_team/lipidxplorer_legacy/badges/main/pipeline.svg)
![coverage](https://git.mpi-cbg.de/scicomp/scidev_team/lipidxplorer_legacy/badges/main/coverage.svg)
![pylint](https://git.mpi-cbg.de/scicomp/scidev_team/lipidxplorer_legacy/-/jobs/artifacts/main/raw/pylint/pylint.svg?job=lint-job)

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.3483976.svg)](https://doi.org/10.5281/zenodo.3483976)
# LipidXplorer

> Note: this directory was previously named `LipidXplorer/`. It was renamed to `preprocessing/` during the public-release refactor. Inside the running Docker container the path is `/app/preprocessing`.

LipidXplorer is a software that is designed to support bottom-up and top-down shotgun lipidomics experiments performed
on all types of tandem mass spectrometers. Lipid identification does not rely on a database resource of reference
or simulated mass spectra but uses user-defined molecular fragment queries. It supports accurate, isotope-corrected
quantification based on the identified MS1 or MS2 level fragments.

## Python3 and Docker

This version of source codes are converted for Python 3. We are trying to make it dockerized.
Feel free to use it via Docker.

## Usage of Docker

## Installation and Tutorials

Please see more detailed installation instructions on our [Wiki](https://lifs.isas.de/wiki/index.php/LipidXplorer_Installation).
These also cover the case of working with the source code.

[The Wiki](https://lifs.isas.de/wiki/index.php) also offers an overview of the concepts behind LipidXplorer, as well as tutorial and reference materials.

## Versioning

We use [Semantic Versioning](http://semver.org/) for versioning of the software.

To browse available versions and releases, please see the [tags on this repository](https://gitlab.isas.de/lifs/lipidxplorer/tags).

## Install for Development environment
* Please install pre-commit package `pip install pre-commit`
* Install pre-commit setup by `pre-commit install --install-hooks`
## Authors

* **Ronny Herzog** - *Initial work*
* **Jacobo Miranda Ackermann** - *Current Developer*
* **Fadi Al Machot** - *Contributor*
* **Nils Hoffmann** - *Contributor*
* **HongKee Moon** - *Developer*

## License

This project is licensed under the GNU GPL License, version 2 - see the [LICENSE.md](LICENSE.md) file for details

## Help and Support

Please check our [Wiki](https://lifs.isas.de/wiki/index.php) on details on how to contact us to receive help and report errors.

## known issues
to run it in a virtual environment in on macOS use pythonw instead of python

## Citing the Software
Herzog R, Schwudke D, Shevchenko A: ***LipidXplorer: Software for Quantitative Shotgun Lipidomics Compatible with Multiple Mass Spectrometry Platforms***. **Current Protocols in Bioinformatics 2013 Oct 15** [PUBMED](https://www.ncbi.nlm.nih.gov/pubmed/26270171)
