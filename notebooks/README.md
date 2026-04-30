# notebooks — regression notebooks for engine polynomials

> This directory was previously named `minterpy/`. It holds the Jupyter notebooks that derive the polynomial coefficients in [`polynomials/*.json`](./polynomials), which the Java engine in [`../engine/`](../engine) loads at runtime to predict theoretical fragment intensities.

These notebooks are **not part of the runtime pipeline**. They are run offline whenever the polynomial model needs to be regenerated (e.g. after changes to the reference standard set or the regression methodology). The output JSONs are committed and consumed by the engine at quantification time.

The table below summarizes which fragment each notebook covers and the quick FA 22:6 sanity check used during validation.

We are using PCO data for the other classes.

| class | sequence | Fragment    | with           | FA 22:6 quick check | jpynb |
| ----- | -------- | ----------- | -------------- | ------------------- | ----- |
| PCO   |          | PCO_PR      |                | max ~70%            | ok    |
|       |          | PCO_M60     |                | max ~75%            | ok    |
|       |          | PCO_sn2FANL |                | max ~6%             | ok    |
|       |          | PCO_sn2CO2  |                | max ~8%             | ok    |
|       |          | PCO_sn2FA   |                | max ~25%            | ok    |
| PC    |          | PC_sn2FA    | PCO_sn2FA      | max ~18%            | ok    |
|       |          | PC_sn2CO2   | PCO_sn2CO2     | max ~6%             | ok    |
|       |          | PC_sn1FA    | PCO_sn2FA      | max ~8%             | ok    |
|       |          | PC_sn1CO2   | PCO_sn2CO2     | max ~2.8%           | ok    |
|       |          | PC_symFA    | PCO_sn2FA      | max ~25%            | ok    |
|       |          | PC_symCO2   | PCO_sn2CO2     | max ~8%             | ok    |
| PEO   |          | PEO_sn2FA   | PCO_sn2FA      | max ~26%            |       |
|       |          | PEO_sn2CO2  | PCO_sn2CO2     | max ~8%             | ok    |
| PE    |          | PE_sn2FA    | PCO_sn2FA      | max ~21%            |       |
|       |          | PE_sn2CO2   | PCO_sn2CO2     | max ~7%             | ok    |
|       |          | PE_sn1FA    | PCO_sn2FA      | max ~8%             |       |
|       |          | PE_sn1CO2   | PCO_sn2CO2     | max ~3%             | ok    |
|       |          | PE_symFA    | PCO_sn2FA      | max ~29%            |       |
|       |          | PE_symCO2   | PCO_sn2CO2     | max ~9%             | ok    |
| PA    | 2        | PA_sn2FA    | PCO_sn2FA      | max ~7%             |       |
|       | 1        | PA_sn2CO2   | PCO_sn2CO2     | max ~1.6%           | ok    |
|       |          | PA_sn1FA    | PCO_sn2FA      | max ~16.5%          |       |
|       | 3        | PA_sn1CO2   | PA_sn2CO2_tmpl | max ~4%             | ok    |
|       |          | PA_symFA    | PCO_sn2FA      | max ~21%            |       |
|       |          | PA_symCO2   | PCO_sn2CO2     | max ~6.7%           | ok    |
| PG    | 2        | PG_sn2FA    | PCO_sn2FA      | max ~17%            |       |
|       | 1        | PG_sn2CO2   | PCO_sn2CO2     | max ~6%             | ok    |
|       |          | PG_sn1FA    | PCO_sn2FA      | max ~7%             |       |
|       | 3        | PG_sn1CO2   | PG_sn2CO2_tmpl | max ~2%             | ok    |
|       |          | PG_symFA    | PCO_sn2FA      | max ~22%            |       |
|       |          | PG_symCO2   | PCO_sn2CO2     | max ~8%             | ok    |
| PS    | 2        | PS_sn2FA    | PCO_sn2FA      | max ~5.2%           |       |
|       | 1        | PS_sn2CO2   | PCO_sn2CO2     | max ~1.5%           | ok    |
|       |          | PS_sn1FA    | PCO_sn2FA      | max ~14.5%          |       |
|       | 3        | PS_sn1CO2   | PS_sn2CO2_tmpl | max ~4%             | ok    |
|       |          | PS_symFA    | PCO_sn2FA      | max ~19%            |       |
|       |          | PS_symCO2   | PCO_sn2CO2     | max ~6%             | ok    |
| PI    | 2        | PI_sn2FA    | PCO_sn2FA      | max ~7%             |       |
|       | 1        | PI_sn2CO2   | PCO_sn2CO2     | max ~2%             | ok    |
|       |          | PI_sn1FA    | PCO_sn2FA      | max ~7%             |       |
|       | 3        | PI_sn1CO2   | PI_sn2CO2_tmpl | max ~2%             | ok    |
|       |          | PI_symFA    | PCO_sn2FA      | max ~14%            |       |
|       | 3        | PI_symCO2   | PI_sn2CO2_tmpl | max ~4%             | ok    |
