# Data processing in LipidXte2 — overview for chemists

LipidXte2 turns the fragment-intensity tables produced by **LipidXplorer** into quantified, position-resolved fatty-acid (FA) compositions for each lipid species in a sample. The full chain runs through three stages: import, correction, and quantification.

### 1. Import from LipidXplorer

LipidXplorer delivers, per spectrum and per lipid species, a `merged.csv` containing:

- the **precursor intensity** of the intact lipid,
- the intensities of the **sn-1 and sn-2 FA fragments** (FAI₁, FAI₂), and
- the matching **neutral-loss CO₂ fragments** (CO₂-NL) for the same positions.

LipidXte2 reads this table together with the user's class definitions (PC, PE, PA, PG, PS, PI, PCO, …) and the internal-standard list, and groups the spectra by NCE (normalized collision energy). For each species it records the experimental **FA / CO₂-NL intensity ratio** at every CE — this ratio is the diagnostic that distinguishes sn-1 from sn-2 fragmentation behaviour.

### 2. Reference model — the polynomial database

The `engine` carries a **polynomial database** (`notebooks/polynomials/*.json`) derived from controlled measurements of pure standards. For every lipid class and every CE, the polynomials predict:

- the theoretical sn-1, sn-2, and symmetric **FA and CO₂-NL intensities**, and
- the corresponding **FA-specific correction factors** (CFs), which compensate for differences in ionization efficiency, chain length, and degree of unsaturation between fatty acids.

This is the model that lets the software interpret what the spectrometer measured.

### 3. Iterative correction and isomer estimation

For each species/CE pair the workflow then:

1. **Estimates FA isomers** from the FA/CO₂-NL ratio (step D in [`docs/workflow.md`](./workflow.md)), using the model's reference ratios to decide how the observed signal splits between possible FA combinations.
2. **Assigns the 1st FA to a sn-position** and applies its CF to recover the *true* fragment intensity (steps E–F).
3. **Estimates the 2nd FA position** on the corrected residual signal and applies its CF in turn (steps G–H). Symmetric species (e.g. PC 18:1/18:1) skip the position search and use the symmetric polynomials directly.
4. Optionally applies a **transmission correction** when a TX function has been provided, accounting for the m/z-dependent transmission of the instrument.

### 4. Quantification and output

The corrected sn-1 and sn-2 intensities are summed into a per-species FA total, then **normalized** either to an internal standard (preferred when the standard list is supplied) or to the most abundant species in the run. The normalized values, together with the inferred sn-1/sn-2 assignments and per-CE diagnostics, are written as TSV and surfaced in the JavaFX charts (intensity plots, correction-factor plots, error bars). The result is a quantitative, position-aware lipid profile — molar amounts of each FA at each sn-position, ready for biological interpretation.
