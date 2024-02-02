<template>
  <div id="wrapper">
    <img id="logo" src="~@/assets/logo.png" alt="electron-vue" align="left"/>

    <div>
      <blockquote>
        Kai Schuhmann, HongKee Moon, Henrik Thomas, Jacobo Miranda Ackerman, Michael Groessl, Nicolai Wagner, Markus Kellmann, Ian Henry, André Nadler, and Andrej Shevchenko.
        <br/>
        <b>Quantitative Fragmentation Model for Bottom-Up Shotgun Lipidomics</b>.
        <br/>
        Analytical Chemistry 2019 91 (18), 12085-12093
        <br/>
        DOI: <a href="https://doi.org/10.1021/acs.analchem.9b03270" target="_blank">10.1021/acs.analchem.9b03270</a>
      </blockquote>
    </div>

    <br/>

    <ul class="nav nav-tabs">
      <li role="presentation">
        <router-link to="/">
          Data Import
        </router-link>
      </li>
      <li role="presentation">
        <router-link to="/slens">
          Validation Samples
        </router-link>
      </li>
      <li role="presentation">
        <router-link to="/poly">
          MS2 spectra calculator
        </router-link>
      </li>
      <li role="presentation" class="active">
        <router-link to="/help">
          Help
        </router-link>
      </li>
    </ul>

    <br/>

    <main>
      <div class="left-side">
        <div class="doc">

          <div class="panel panel-default">
            <div class="panel-heading title" @click="collapse('doc1')">LipidXte Service</div>
            <div class="panel-body" ref="doc1">The LipidXte web application provides automatic correction for lipid HCD MS/MS spectra from Orbitrap instruments and is dedicated to supporting accurate quantification of molecular lipid species. The application is specifically designed to support easy access to established correction functions with minimum interference requirements.</div>
          </div>

          <div class="panel panel-default">
            <div class="panel-heading title" @click="collapse('doc2')">Data Import</div>
            <div class="panel-body" ref="doc2">
              <h4>Format of input *.raw</h4>

              <p>
                LipidXte processes Thermo *.raw files with a defined structure. The *.raw files must contain:
              </p>

              <ul class="list-group">
                <li class="list-group-item">n >= 2 broad-band FT MS with 140k resolution at m/z 200</li>
                <li class="list-group-item">n >= 6 HCD FT MS/MS scans for each precursor with
                  <ul>
                    <li class="margin">140k resolution at m/z 200</li>
                    <li class="margin"><i>Isolation window</i> of 1.0 m/z</li>
                    <li class="margin"><i>Fixed first mass</i> set to 180 m/z</li>
                    <li class="margin"><i>Maximum IT</i> >= 500 ms</li>
                    <li class="margin">3 distinct normalized collision energies (e.g. 25, 30, 35 % defined in the inclusion list)</li>
                  </ul>
                </li>
              </ul>
              <p><i> * Please check our example .raw files shown below</i></p>

              <h4>Example files for LipidXte</h4>

              <p>
                A set of commercial samples was measured by shotgun lipidomics using DIA of HCD MS/MS with multiple normalized collision energies as documented above. Full details of the QExactive acquisition method are documented in the *.raw file "Instrument method" meta-data.
              </p>

              <p>
                Example *.raw were acquired for:
              </p>

              <ul class="list-group">
                <li class="list-group-item">Synthetic PC standards - <a href="dat/PC_ILIS.zip" download>link</a></li>
                <li class="list-group-item">Natural mixtures of:
                  <ul>
                    <li class="margin">Egg PC (Avanti nr. 840051) - <a href="dat/PC_egg.zip" download>link</a></li>
                    <li class="margin">Heart PC (Avanti nr. 840052) - <a href="dat/PC_heart.zip" download>link</a></li>
                    <li class="margin">Brain PC (Avanti nr. 840053) - <a href="dat/PC_brain.zip" download>link</a></li>
                  </ul>
                </li>
              </ul>

              <h4>Sample data input</h4>

              <p>
                n >= 1 .raw file(s) plus 1 internal standard list *.csv are required for running <i>LipidXte</i>. Add your *.raw files to "group 1" for processing them together. Alternatively, *.raw files can be uploaded in 3 separate groups. All files can be inserted to their destination via drag-and-drop. Upload time might significantly increase your processing time. Accepted files are further processed (processing time for example "Egg PC" was about 2 min 40 s).
              </p>
              <img class="fig" src="~@/assets/lipidxte_instructions.gif" width="500px"/>
              <h4>Data processing</h4>

              <p>
                LipidXte web application contains runs a series of applications:
              </p>

              <ol class="margin">
                <li>Input .raw data FT MS and HCD FT MS/MS scans are cleaned from noise by <i>PeakStrainer</i> and converted to *.mzXML by <i>RawFileReader</i><br/>
                  <b style="font-size: smaller">(RawFileReader reading tool. Copyright © 2016 by Thermo Fisher Scientific, Inc. All rights reserved.)</b></li>
                <li>Generated *.mzXML are imported into LipidXplorer</li>
                <li>Measured intensities of precursor ions, carboxylate anions, and potential CO2 loss fragments of all lipid species are corrected for isotope effects and extracted from the temporary data</li>
                <li>The intensities of carboxylate anions and potential CO2 loss fragments
                  are corrected by LipidXte for:
                  <ul>
                    <li class="margin">Fragmentation influences using external calibration database</li>
                    <li class="margin">Correction for instrument performance</li>
                  </ul>
                </li>
                <li>The corrected fragment intensities are, if matching to external calibration, subjected to quantification. Whereas, the corrected intensities of all molecular lipid species are normalized to the given internal standard(s).</li>
              </ol>

            </div>
          </div>

        </div>
      </div>
      <div class="right-side">
        <div class="panel panel-default">
          <div class="panel-heading title" @click="collapse('doc4')">Validation Samples</div>
          <div class="panel-body collapse" ref="doc4">

            The result view enables to browse your own or the provided shotgun lipidomics data sets.

            <img src="~@/assets/result-view.jpg"/>

            Note:

            <ul class="margin">
              <li class="margin">Results are represented as box plots with min/median/max mole percentage (mol%) or quantity (uM ...) depending on the users selection.</li>
              <li class="margin"><i>Remove References</i> check-box enables to removed standards from the result plots</li>
              <li class="margin"><i>No MS/MS Correction</i> check-box enables to review data without correction for HCD fragmentation bias</li>
            </ul>

            <h4>Review pre-calculated data sets</h4>

            <p>
              In this tab, the user can select a one of the provided bottom-up shotgun lipidomics data sets that have been previously acquired with the defined acquisition method and analyzed by LipidXte. The results are directly accessible; no calculations are required.
            </p>

            <h4>Review of lipid species</h4>

            <p>
              Here, the lipid species of a selected lipid class contained by a given sample can be reviewed. This result considers only readout from FT MS and correction for isotopic abundances and overlaps.
            </p>

            <p>
              Lipid species plot for the <a href="dat/PC_ILIS.zip" download>synthetic PC mixture</a>:
            </p>

            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_Lspecies.jpg" width="500px"/>
            </p>

            <h4>Reviewing the molecular lipid species</h4>

            <p>
              In this tab, the molecular lipid species of a selected lipid class contained by a given sample can be reviewed. The user can observe the influence of HCD fragmentation and the carboxylate anion intensity correction on the quantification of molecular lipid species.
            </p>

            <p>
              The plotly interface allows you to select species of interest, modify and export the result graph. Result data can be downloaded by using corresponding button.
            </p>

            <p>
              Concentration of molecular lipid species in the <a href="dat/PC_ILIS.zip" download>synthetic PC mixture</a>:
            </p>

            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_Mspecies.jpg" width="500px"/>
            </p>
            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_Mspecies_uncorrected.jpg" width="500px"/>
            </p>

            <h4>Validation plot</h4>

            <p>
              The validation plot enables to review results from FT MS and HCD FT MS/MS in one graph. The plot shows the median quantity from FT MS (x-coordinate) vs. the median, corrected quantities from HCD FT MS/MS for all selected normalized collision energies (y-coordinate, blue dots) and the corresponding linear fit (blue line). Median values from uncorrected HCD FT MS/MS are shown for the highest selected collision energy.
            </p>

            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_validation_uncorrected.jpg" width="500px"/>
            </p>
            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_validation_corrected.jpg" width="500px"/>
            </p>

          </div>
        </div>

        <div class="panel panel-default">
          <div class="panel-heading title" @click="collapse('doc5')">MS2 spectra calculator</div>
          <div class="panel-body collapse" ref="doc5">

            The result view enables to browse your own or the provided shotgun lipidomics data sets.

            <img src="~@/assets/result-view.jpg"/>

            Note:

            <ul class="margin">
              <li class="margin">Results are represented as box plots with min/median/max mole percentage (mol%) or quantity (uM ...) depending on the users selection.</li>
              <li class="margin"><i>Remove References</i> check-box enables to removed standards from the result plots</li>
              <li class="margin"><i>No MS/MS Correction</i> check-box enables to review data without correction for HCD fragmentation bias</li>
            </ul>

            <h4>Review pre-calculated data sets</h4>

            <p>
              In this tab, the user can select a one of the provided bottom-up shotgun lipidomics data sets that have been previously acquired with the defined acquisition method and analyzed by LipidXte. The results are directly accessible; no calculations are required.
            </p>

            <h4>Review of lipid species</h4>

            <p>
              Here, the lipid species of a selected lipid class contained by a given sample can be reviewed. This result considers only readout from FT MS and correction for isotopic abundances and overlaps.
            </p>

            <p>
              Lipid species plot for the <a href="dat/PC_ILIS.zip" download>synthetic PC mixture</a>:
            </p>

            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_Lspecies.jpg" width="500px"/>
            </p>

            <h4>Reviewing the molecular lipid species</h4>

            <p>
              In this tab, the molecular lipid species of a selected lipid class contained by a given sample can be reviewed. The user can observe the influence of HCD fragmentation and the carboxylate anion intensity correction on the quantification of molecular lipid species.
            </p>

            <p>
              The plotly interface allows you to select species of interest, modify and export the result graph. Result data can be downloaded by using corresponding button.
            </p>

            <p>
              Concentration of molecular lipid species in the <a href="dat/PC_ILIS.zip" download>synthetic PC mixture</a>:
            </p>

            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_Mspecies.jpg" width="500px"/>
            </p>
            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_Mspecies_uncorrected.jpg" width="500px"/>
            </p>

            <h4>Validation plot</h4>

            <p>
              The validation plot enables to review results from FT MS and HCD FT MS/MS in one graph. The plot shows the median quantity from FT MS (x-coordinate) vs. the median, corrected quantities from HCD FT MS/MS for all selected normalized collision energies (y-coordinate, blue dots) and the corresponding linear fit (blue line). Median values from uncorrected HCD FT MS/MS are shown for the highest selected collision energy.
            </p>

            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_validation_uncorrected.jpg" width="500px"/>
            </p>
            <p>
              <img class="fig" src="~@/assets/20180823_ILIS_validation_corrected.jpg" width="500px"/>
            </p>

          </div>
        </div>
      </div>
    </main>

    <footer class="page-footer font-small special-color-dark pt-4">
      <div class="container">
        <ul class="list-unstyled list-inline text-center">
          <li class="list-inline-item"><a href="https://www.mpi-cbg.de/research-groups/current-groups/andrej-shevchenko" target="_blank">Shevchenko Lab</a></li>
          <li class="list-inline-item"><a href="https://www.mpi-cbg.de/imprint.html" target="_blank">Imprint</a></li>
          <li class="list-inline-item"><a href="https://www.mpi-cbg.de/privacy-policy/" target="_blank">Privacy Policy</a></li>
        </ul>
      </div>
    </footer>
  </div>
</template>

<script>
import 'font-awesome/css/font-awesome.css'

export default {
  name: 'landing-page',
  components: { },
  methods: {
    open (link) {
      this.$electron.shell.openExternal(link)
    },
    collapse (ref) {
      let vm = this
      let className = vm.$refs[ref].className
      if (className.indexOf('collapse') < 0) {
        vm.$refs[ref].className = className + ' collapse'
      } else {
        vm.$refs[ref].className = className.replace(' collapse', '')
      }

      if (ref === 'doc2') {
        if (vm.$refs[ref].className.indexOf('collapse') > -1) {
          vm.$refs.doc3.className = vm.$refs.doc3.className.replace(' collapse', '')
        } else {
          vm.$refs.doc3.className = vm.$refs.doc3.className + ' collapse'
        }
      } else if (ref === 'doc3') {
        if (vm.$refs[ref].className.indexOf('collapse') > -1) {
          vm.$refs.doc2.className = vm.$refs.doc2.className.replace(' collapse', '')
        } else {
          vm.$refs.doc2.className = vm.$refs.doc2.className + ' collapse'
        }
      }
    }
  },
  data: function () {
    return {
    }
  },
  mounted: function () {
  }
}
</script>

<style>
@import url('~@/assets/bootstrap.css');
@import url('https://fonts.googleapis.com/css?family=Source+Sans+Pro');

text.legendtext tspan {
  text-decoration: underline;
}

@media (min-width: 360px) {
  body {
    font-size: 1.0em;
  }
  blockquote {
    font-size: 1.2em;
  }
  .left-side {
    display: none;
    flex-direction: unset;
  }
}

@media (min-width: 500px) {
  body {
    font-size: 1.5em;
  }
  blockquote {
    font-size: 1.6em;
  }
  .left-side {
    display: inline-flex;
    flex-direction: column;
  }
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

blockquote{
  font-size: 15px;
  /*background: #f9f9f9;*/
  border-left: 0px solid #ccc;
  margin: 0em 10px;
  padding: 0 10px;
  quotes: "\201C""\201D""\2018""\2019";
  /*padding: 10px 20px;*/
  /*line-height: 1.4;*/
}

blockquote:before {
  content: open-quote;
  display: inline;
  height: 0;
  line-height: 0;
  left: -10px;
  position: relative;
  top: 18px;
  color: #ccc;
  font-size: 3em;
}


body {
  font-family: 'Source Sans Pro', sans-serif;
  background: rgba(255, 255, 255, 1) 40%
}

#wrapper {
  /*background:*/
  /*radial-gradient(*/
  /*ellipse at top left,*/
  /*rgba(255, 255, 255, 1) 40%,*/
  /*rgba(229, 229, 229, .9) 100%*/
  /*);*/
  height: 100vh;
  padding: 60px 80px;
  width: 100vw;
}

#logo {
  height: auto;
  margin-bottom: 20px;
  width: 420px;
}

main {
  display: flex;
  justify-content: space-between;
}

main > div { flex-basis: 45%; }

/*.left-side {*/
/*display: flex;*/
/*flex-direction: column;*/
/*}*/

.welcome {
  color: #555;
  font-size: 23px;
  margin-bottom: 10px;
}

.title {
  color: #2c3e50;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 6px;
}

.title.alt {
  font-size: 18px;
  margin-bottom: 10px;
}

.doc p {
  color: black;
  margin-bottom: 10px;
}

.doc button {
  font-size: .8em;
  cursor: pointer;
  outline: none;
  padding: 0.75em 2em;
  border-radius: 2em;
  display: inline-block;
  color: #fff;
  background-color: #339999;
  transition: all 0.15s ease;
  box-sizing: border-box;
  border: 1px solid #338888;
  margin-bottom: 10px;
}

.doc input {
  width: 200px;
  font-size: .8em;
  outline: none;
  padding: 0.75em 1em;
  border-radius: 0.5em;
  display: inline-block;
  color: #000000;
  background-color: #fcfcfc;
  transition: all 0.15s ease;
  box-sizing: border-box;
  border: 1px solid #338888;
  margin-bottom: 10px;
}

.doc select {
  margin-left: 1em;
  width: 100px;
  font-size: .8em;
  outline: none;
  padding: 0.75em 1em;
  border-radius: 0.5em;
  display: inline-block;
  color: #000000;
  background-color: #fcfcfc;
  transition: all 0.15s ease;
  box-sizing: border-box;
  border: 1px solid #338888;
  margin-bottom: 10px;
}

.doc select.timestamp {
  margin-left: 1em;
  width: 250px;
  font-size: .9em;
  outline: none;
  padding: 0.75em 1em;
  border-radius: 0.5em;
  display: inline-block;
  color: #000000;
  background-color: #fcfcfc;
  transition: all 0.15s ease;
  box-sizing: border-box;
  border: 1px solid #338888;
  margin-bottom: 10px;
}

.doc .vue-input-tag-wrapper {
  overflow: unset;
  width: 200px;
  outline: none;
  padding: 0.5em 0.8em 0.1em 0.8em;
  border-radius: 0.5em;
  display: inline-block;
  color: #000000;
  background-color: #fcfcfc;
  transition: all 0.15s ease;
  box-sizing: border-box;
  border: 1px solid #338888;
  -webkit-appearance: none;
  margin-bottom: 10px;
  margin-left: 1px;
}

.doc button.alt {
  color: #42b983;
  background-color: transparent;
}

::selection {
  background:rgba(255, 255, 125, 0.99);
  color:#032764;
}

/*
* The MIT License
* Copyright (c) 2012 Matias Meno <m@tias.me>
*/
@-webkit-keyframes passing-through {
  0% {
    opacity: 0;
    -webkit-transform: translateY(40px);
    -moz-transform: translateY(40px);
    -ms-transform: translateY(40px);
    -o-transform: translateY(40px);
    transform: translateY(40px); }
  30%, 70% {
    opacity: 1;
    -webkit-transform: translateY(0px);
    -moz-transform: translateY(0px);
    -ms-transform: translateY(0px);
    -o-transform: translateY(0px);
    transform: translateY(0px); }
  100% {
    opacity: 0;
    -webkit-transform: translateY(-40px);
    -moz-transform: translateY(-40px);
    -ms-transform: translateY(-40px);
    -o-transform: translateY(-40px);
    transform: translateY(-40px); } }
@-moz-keyframes passing-through {
  0% {
    opacity: 0;
    -webkit-transform: translateY(40px);
    -moz-transform: translateY(40px);
    -ms-transform: translateY(40px);
    -o-transform: translateY(40px);
    transform: translateY(40px); }
  30%, 70% {
    opacity: 1;
    -webkit-transform: translateY(0px);
    -moz-transform: translateY(0px);
    -ms-transform: translateY(0px);
    -o-transform: translateY(0px);
    transform: translateY(0px); }
  100% {
    opacity: 0;
    -webkit-transform: translateY(-40px);
    -moz-transform: translateY(-40px);
    -ms-transform: translateY(-40px);
    -o-transform: translateY(-40px);
    transform: translateY(-40px); } }
@keyframes passing-through {
  0% {
    opacity: 0;
    -webkit-transform: translateY(40px);
    -moz-transform: translateY(40px);
    -ms-transform: translateY(40px);
    -o-transform: translateY(40px);
    transform: translateY(40px); }
  30%, 70% {
    opacity: 1;
    -webkit-transform: translateY(0px);
    -moz-transform: translateY(0px);
    -ms-transform: translateY(0px);
    -o-transform: translateY(0px);
    transform: translateY(0px); }
  100% {
    opacity: 0;
    -webkit-transform: translateY(-40px);
    -moz-transform: translateY(-40px);
    -ms-transform: translateY(-40px);
    -o-transform: translateY(-40px);
    transform: translateY(-40px); } }
@-webkit-keyframes slide-in {
  0% {
    opacity: 0;
    -webkit-transform: translateY(40px);
    -moz-transform: translateY(40px);
    -ms-transform: translateY(40px);
    -o-transform: translateY(40px);
    transform: translateY(40px); }
  30% {
    opacity: 1;
    -webkit-transform: translateY(0px);
    -moz-transform: translateY(0px);
    -ms-transform: translateY(0px);
    -o-transform: translateY(0px);
    transform: translateY(0px); } }
@-moz-keyframes slide-in {
  0% {
    opacity: 0;
    -webkit-transform: translateY(40px);
    -moz-transform: translateY(40px);
    -ms-transform: translateY(40px);
    -o-transform: translateY(40px);
    transform: translateY(40px); }
  30% {
    opacity: 1;
    -webkit-transform: translateY(0px);
    -moz-transform: translateY(0px);
    -ms-transform: translateY(0px);
    -o-transform: translateY(0px);
    transform: translateY(0px); } }
@keyframes slide-in {
  0% {
    opacity: 0;
    -webkit-transform: translateY(40px);
    -moz-transform: translateY(40px);
    -ms-transform: translateY(40px);
    -o-transform: translateY(40px);
    transform: translateY(40px); }
  30% {
    opacity: 1;
    -webkit-transform: translateY(0px);
    -moz-transform: translateY(0px);
    -ms-transform: translateY(0px);
    -o-transform: translateY(0px);
    transform: translateY(0px); } }
@-webkit-keyframes pulse {
  0% {
    -webkit-transform: scale(1);
    -moz-transform: scale(1);
    -ms-transform: scale(1);
    -o-transform: scale(1);
    transform: scale(1); }
  10% {
    -webkit-transform: scale(1.1);
    -moz-transform: scale(1.1);
    -ms-transform: scale(1.1);
    -o-transform: scale(1.1);
    transform: scale(1.1); }
  20% {
    -webkit-transform: scale(1);
    -moz-transform: scale(1);
    -ms-transform: scale(1);
    -o-transform: scale(1);
    transform: scale(1); } }
@-moz-keyframes pulse {
  0% {
    -webkit-transform: scale(1);
    -moz-transform: scale(1);
    -ms-transform: scale(1);
    -o-transform: scale(1);
    transform: scale(1); }
  10% {
    -webkit-transform: scale(1.1);
    -moz-transform: scale(1.1);
    -ms-transform: scale(1.1);
    -o-transform: scale(1.1);
    transform: scale(1.1); }
  20% {
    -webkit-transform: scale(1);
    -moz-transform: scale(1);
    -ms-transform: scale(1);
    -o-transform: scale(1);
    transform: scale(1); } }
@keyframes pulse {
  0% {
    -webkit-transform: scale(1);
    -moz-transform: scale(1);
    -ms-transform: scale(1);
    -o-transform: scale(1);
    transform: scale(1); }
  10% {
    -webkit-transform: scale(1.1);
    -moz-transform: scale(1.1);
    -ms-transform: scale(1.1);
    -o-transform: scale(1.1);
    transform: scale(1.1); }
  20% {
    -webkit-transform: scale(1);
    -moz-transform: scale(1);
    -ms-transform: scale(1);
    -o-transform: scale(1);
    transform: scale(1); } }
.dropzone, .dropzone * {
  box-sizing: border-box; }

.dropzone {
  min-height: 150px;
  border: 2px solid rgba(0, 0, 0, 0.3);
  background: white;
  padding: 20px 20px; }
.dropzone.dz-clickable {
  cursor: pointer; }
.dropzone.dz-clickable * {
  cursor: default; }
.dropzone.dz-clickable .dz-message, .dropzone.dz-clickable .dz-message * {
  cursor: pointer; }
.dropzone.dz-started .dz-message {
  display: none; }
.dropzone.dz-drag-hover {
  border-style: solid; }
.dropzone.dz-drag-hover .dz-message {
  opacity: 0.5; }
.dropzone .dz-message {
  text-align: center;
  margin: 2em 0; }
.dropzone .dz-preview {
  position: relative;
  display: inline-block;
  vertical-align: top;
  margin: 16px;
  min-height: 100px; }
.dropzone .dz-preview:hover {
  z-index: 1000; }
.dropzone .dz-preview:hover .dz-details {
  opacity: 1; }
.dropzone .dz-preview.dz-file-preview .dz-image {
  border-radius: 20px;
  background: #999;
  background: linear-gradient(to bottom, #eee, #ddd); }
.dropzone .dz-preview.dz-file-preview .dz-details {
  opacity: 1; }
.dropzone .dz-preview.dz-image-preview {
  background: white; }
.dropzone .dz-preview.dz-image-preview .dz-details {
  -webkit-transition: opacity 0.2s linear;
  -moz-transition: opacity 0.2s linear;
  -ms-transition: opacity 0.2s linear;
  -o-transition: opacity 0.2s linear;
  transition: opacity 0.2s linear; }
.dropzone .dz-preview .dz-remove {
  font-size: 14px;
  text-align: center;
  display: block;
  cursor: pointer;
  border: none; }
.dropzone .dz-preview .dz-remove:hover {
  text-decoration: underline; }
.dropzone .dz-preview:hover .dz-details {
  opacity: 1; }
.dropzone .dz-preview .dz-details {
  z-index: 20;
  position: absolute;
  top: 0;
  left: 0;
  opacity: 0;
  font-size: 13px;
  min-width: 100%;
  max-width: 100%;
  padding: 2em 1em;
  text-align: center;
  color: rgba(0, 0, 0, 0.9);
  line-height: 150%; }
.dropzone .dz-preview .dz-details .dz-size {
  margin-bottom: 1em;
  font-size: 16px; }
.dropzone .dz-preview .dz-details .dz-filename {
  white-space: nowrap; }
.dropzone .dz-preview .dz-details .dz-filename:hover span {
  border: 1px solid rgba(200, 200, 200, 0.8);
  background-color: rgba(255, 255, 255, 0.8); }
.dropzone .dz-preview .dz-details .dz-filename:not(:hover) {
  overflow: hidden;
  text-overflow: ellipsis; }
.dropzone .dz-preview .dz-details .dz-filename:not(:hover) span {
  border: 1px solid transparent; }
.dropzone .dz-preview .dz-details .dz-filename span, .dropzone .dz-preview .dz-details .dz-size span {
  background-color: rgba(255, 255, 255, 0.4);
  padding: 0 0.4em;
  border-radius: 3px; }
.dropzone .dz-preview:hover .dz-image img {
  -webkit-transform: scale(1.05, 1.05);
  -moz-transform: scale(1.05, 1.05);
  -ms-transform: scale(1.05, 1.05);
  -o-transform: scale(1.05, 1.05);
  transform: scale(1.05, 1.05);
  -webkit-filter: blur(8px);
  filter: blur(8px); }
.dropzone .dz-preview .dz-image {
  border-radius: 20px;
  overflow: hidden;
  width: 120px;
  height: 120px;
  position: relative;
  display: block;
  z-index: 10; }
.dropzone .dz-preview .dz-image img {
  display: block; }
.dropzone .dz-preview.dz-success .dz-success-mark {
  -webkit-animation: passing-through 3s cubic-bezier(0.77, 0, 0.175, 1);
  -moz-animation: passing-through 3s cubic-bezier(0.77, 0, 0.175, 1);
  -ms-animation: passing-through 3s cubic-bezier(0.77, 0, 0.175, 1);
  -o-animation: passing-through 3s cubic-bezier(0.77, 0, 0.175, 1);
  animation: passing-through 3s cubic-bezier(0.77, 0, 0.175, 1); }
.dropzone .dz-preview.dz-error .dz-error-mark {
  opacity: 1;
  -webkit-animation: slide-in 3s cubic-bezier(0.77, 0, 0.175, 1);
  -moz-animation: slide-in 3s cubic-bezier(0.77, 0, 0.175, 1);
  -ms-animation: slide-in 3s cubic-bezier(0.77, 0, 0.175, 1);
  -o-animation: slide-in 3s cubic-bezier(0.77, 0, 0.175, 1);
  animation: slide-in 3s cubic-bezier(0.77, 0, 0.175, 1); }
.dropzone .dz-preview .dz-success-mark, .dropzone .dz-preview .dz-error-mark {
  pointer-events: none;
  opacity: 0;
  z-index: 500;
  position: absolute;
  display: block;
  top: 50%;
  left: 50%;
  margin-left: -27px;
  margin-top: -27px; }
.dropzone .dz-preview .dz-success-mark svg, .dropzone .dz-preview .dz-error-mark svg {
  display: block;
  width: 54px;
  height: 54px; }
.dropzone .dz-preview.dz-processing .dz-progress {
  opacity: 1;
  -webkit-transition: all 0.2s linear;
  -moz-transition: all 0.2s linear;
  -ms-transition: all 0.2s linear;
  -o-transition: all 0.2s linear;
  transition: all 0.2s linear; }
.dropzone .dz-preview.dz-complete .dz-progress {
  opacity: 0;
  -webkit-transition: opacity 0.4s ease-in;
  -moz-transition: opacity 0.4s ease-in;
  -ms-transition: opacity 0.4s ease-in;
  -o-transition: opacity 0.4s ease-in;
  transition: opacity 0.4s ease-in; }
.dropzone .dz-preview:not(.dz-processing) .dz-progress {
  -webkit-animation: pulse 6s ease infinite;
  -moz-animation: pulse 6s ease infinite;
  -ms-animation: pulse 6s ease infinite;
  -o-animation: pulse 6s ease infinite;
  animation: pulse 6s ease infinite; }
.dropzone .dz-preview .dz-progress {
  opacity: 1;
  z-index: 1000;
  pointer-events: none;
  position: absolute;
  height: 16px;
  left: 50%;
  top: 50%;
  margin-top: -8px;
  width: 80px;
  margin-left: -40px;
  background: rgba(255, 255, 255, 0.9);
  -webkit-transform: scale(1);
  border-radius: 8px;
  overflow: hidden; }
.dropzone .dz-preview .dz-progress .dz-upload {
  background: #333;
  background: linear-gradient(to bottom, #666, #444);
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 0;
  -webkit-transition: width 300ms ease-in-out;
  -moz-transition: width 300ms ease-in-out;
  -ms-transition: width 300ms ease-in-out;
  -o-transition: width 300ms ease-in-out;
  transition: width 300ms ease-in-out; }
.dropzone .dz-preview.dz-error .dz-error-message {
  display: block; }
.dropzone .dz-preview.dz-error:hover .dz-error-message {
  opacity: 1;
  pointer-events: auto; }
.dropzone .dz-preview .dz-error-message {
  pointer-events: none;
  z-index: 1000;
  position: absolute;
  display: block;
  display: none;
  opacity: 0;
  -webkit-transition: opacity 0.3s ease;
  -moz-transition: opacity 0.3s ease;
  -ms-transition: opacity 0.3s ease;
  -o-transition: opacity 0.3s ease;
  transition: opacity 0.3s ease;
  border-radius: 8px;
  font-size: 13px;
  top: 130px;
  left: -10px;
  width: 140px;
  background: #be2626;
  background: linear-gradient(to bottom, #be2626, #a92222);
  padding: 0.5em 1.2em;
  color: white; }
.dropzone .dz-preview .dz-error-message:after {
  content: '';
  position: absolute;
  top: -6px;
  left: 64px;
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid #be2626; }

.vue-dropzone {
  border: 2px solid #E5E5E5;
  font-family: 'Arial', sans-serif;
  letter-spacing: 0.2px;
  color: #777;
  transition: background-color 0.2s linear;
}
.vue-dropzone:hover {
  background-color: #F6F6F6;
}
.vue-dropzone i {
  color: #CCC;
}
.vue-dropzone .dz-preview .dz-image {
  border-radius: 0;
  width: 100%;
  height: 100%;
}
.vue-dropzone .dz-preview .dz-image img:not([src]) {
  width: 200px;
  height: 200px;
}
.vue-dropzone .dz-preview .dz-image:hover img {
  transform: none;
  -webkit-filter: none;
}
.vue-dropzone .dz-preview .dz-details {
  bottom: 0;
  top: 0;
  color: white;
  background-color: rgba(33, 150, 243, 0.8);
  transition: opacity .2s linear;
  text-align: left;
}
.vue-dropzone .dz-preview .dz-details .dz-filename {
  overflow: hidden;
}
.vue-dropzone .dz-preview .dz-details .dz-filename span,
.vue-dropzone .dz-preview .dz-details .dz-size span {
  background-color: transparent;
}
.vue-dropzone .dz-preview .dz-details .dz-filename:not(:hover) span {
  border: none;
}
.vue-dropzone .dz-preview .dz-details .dz-filename:hover span {
  background-color: transparent;
  border: none;
}
.vue-dropzone .dz-preview .dz-progress .dz-upload {
  background: #cccccc;
}
.vue-dropzone .dz-preview .dz-remove {
  position: absolute;
  z-index: 30;
  color: white;
  margin-left: 15px;
  padding: 10px;
  top: inherit;
  bottom: 15px;
  border: 2px white solid;
  text-decoration: none;
  text-transform: uppercase;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 1.1px;
  opacity: 0;
}
.vue-dropzone .dz-preview:hover .dz-remove {
  opacity: 1;
}
.vue-dropzone .dz-preview .dz-success-mark,
.vue-dropzone .dz-preview .dz-error-mark {
  margin-left: auto;
  margin-top: auto;
  width: 100%;
  top: 35%;
  left: 0;
}
.vue-dropzone .dz-preview .dz-success-mark svg,
.vue-dropzone .dz-preview .dz-error-mark svg {
  margin-left: auto;
  margin-right: auto;
}
.vue-dropzone .dz-preview .dz-error-message {
  top: calc(15%);
  margin-left: auto;
  margin-right: auto;
  left: 0;
  width: 100%;
}
.vue-dropzone .dz-preview .dz-error-message:after {
  bottom: -6px;
  top: initial;
  border-top: 6px solid #a92222;
  border-bottom: none;
}

/* customized */

.vue-dropzone {
  border: 2px solid #E5E5E5;
  font-family: 'Arial', sans-serif;
  letter-spacing: 0.2px;
  color: #777;
  transition: background-color 0.2s linear;
  padding: 5px 5px;
}

.dropzone .dz-preview {
  position: relative;
  display: inline-block;
  vertical-align: top;
  margin: 5px;
  width: 96%;
}

.vue-dropzone .dz-preview .dz-details {
  bottom: 0;
  top: 0;
  color: white;
  background-color: rgba(23, 77, 80, 0.76);
  transition: opacity .2s linear;
  text-align: left;
}

.dropzone .dz-preview.dz-file-preview .dz-image {
  display: none;
}

.dropzone .dz-preview .dz-details .dz-filename {
  white-space: normal;
}

.dropzone .dz-preview .dz-progress {
  display: none;
}

.vue-dropzone .dz-preview .dz-remove {
  position: absolute;
  z-index: 30;
  color: white;
  margin-left: 150px;
  padding: 10px;
  top: inherit;
  bottom: 50px;
  border: 2px white solid;
  text-decoration: none;
  text-transform: uppercase;
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 1.1px;
  opacity: 1;
}

.dropzone .dz-preview .dz-details .dz-filename span,
.dropzone .dz-preview .dz-details .dz-size span {
  padding: 0;
  width: 100%;
}

code,
kbd,
pre,
samp {
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
}
code {
  padding: 2px 4px;
  font-size: 90%;
  color: #c7254e;
  background-color: #f9f2f4;
  border-radius: 4px;
}
kbd {
  padding: 2px 4px;
  font-size: 90%;
  color: #fff;
  background-color: #333;
  border-radius: 3px;
  -webkit-box-shadow: inset 0 -1px 0 rgba(0, 0, 0, .25);
  box-shadow: inset 0 -1px 0 rgba(0, 0, 0, .25);
}
</style>
