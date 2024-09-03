<template>
  <div id="wrapper">
    <img id="logo" src="~@/assets/logo.png" alt="electron-vue"/>

    <a href="https://github.com/lifs-tools/LipidXte2">
      <svg aria-hidden="true" height="24" viewBox="0 0 16 16" version="1.1" width="24" data-view-component="true" class="octicon octicon-mark-github">
        <path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.21-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"></path>
      </svg>
      https://github.com/lifs-tools/LipidXte2
    </a>

    <ul class="nav nav-tabs">
      <li role="presentation">
        <router-link :to="'/?timestamp=' + $route.query.timestamp" v-if="$route.query.timestamp">
          Data Import
        </router-link>
        <router-link to="/" v-else>
          Data Import
        </router-link>
      </li>
      <li role="presentation" v-show="$route.query.timestamp">
        <router-link :to="'/sample?timestamp=' + $route.query.timestamp">
          Result View
        </router-link>
      </li>
      <li role="presentation">
        <router-link :to="'/slens?timestamp=' + $route.query.timestamp" v-if="$route.query.timestamp">
          Samples
        </router-link>
        <router-link to="/slens" v-else>
          Samples
        </router-link>
      </li>
      <li role="presentation">
        <router-link :to="'/poly?timestamp=' + $route.query.timestamp" v-if="$route.query.timestamp">
          MS2 spectra calculator
        </router-link>
        <router-link to="/poly" v-else>
          MS2 spectra calculator
        </router-link>
      </li>
      <li role="presentation" class="active">
        <router-link :to="'/help?timestamp=' + $route.query.timestamp" v-if="$route.query.timestamp">
          Help
        </router-link>
        <router-link to="/help" v-else>
          Help
        </router-link>
      </li>
    </ul>

    <br/>

    <main>
      <div class="doc_help">
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

        <div class="panel panel-default">
          <div class="panel-heading title">LipidXte Service</div>
          <div class="panel-body" ref="doc1">The LipidXte web application provides automatic correction for lipid HCD MS/MS spectra from Orbitrap instruments and is dedicated to supporting accurate quantification of molecular lipid species. The application is specifically designed to support easy access to established correction functions with minimum interference requirements.</div>
        </div>

        <div class="panel panel-default">
          <div class="panel-heading title">Data Import</div>
          <div class="panel-body" ref="doc2">
            <h4>Format of input data</h4>

            <p>
              LipidXte processes shotgun lipidomics DIA data containing HCD FTMS/MS with varying normalized collision energies. The current prototype supports Thermo *.raw files that follow a specific structure. A short description is available below. Comprehensive information about the QExactive acquisition method is given in the “Instrument method” metadata of the listed *.raw sample files.
            </p>
            <p>
              Example *.raw were acquired as following:
            </p>

            <ul class="list-group">
              <li class="list-group-item">FT MS with more than 2 scans and a resolution of 140k at m/z 200</li>
              <li class="list-group-item">HCD FT MS/MS with
                <ul>
                  <li class="margin">More than 2 MS/MS scans per precursor</li>
                  <li class="margin">Resolution of 140k at m/z 200</li>
                  <li class="margin"><i>Isolation window</i> of 1.0 m/z</li>
                  <li class="margin"><i>Fixed first mass</i> set to 180 m/z</li>
                  <li class="margin">1 or 3 distinct normalized collision energies (e.g. 25, 30, 35 % defined in the inclusion list)</li>
                </ul>
              </li>
            </ul>
            <p><i> * Please check our example .raw files shown below</i></p>

            <h4>Example files for LipidXte</h4>

            <p>
              A set of commercial samples was measured by shotgun lipidomics using DIA of HCD MS/MS with multiple normalized collision energies as documented above. Full details of the QExactive acquisition method are documented in the *.raw file "Instrument method" meta-data.
            </p>

            <p>
              Example *.raw files for LipidXte
            </p>

            <ul class="list-group">
              <li class="list-group-item">
                <ul>
                  <li class="margin">Custom PC mix - <a href="dat/PC_ILIS.zip" download>link</a></li>
                  <li class="margin">Ultimate SPLASH One (Avanti #330820) - <a href="" download>link</a></li>
                  <li class="margin">Egg PC (Avanti #840051) - <a href="dat/PC_egg.zip" download>link</a></li>
                  <li class="margin">Heart PC (Avanti #840052) - <a href="dat/PC_heart.zip" download>link</a></li>
                  <li class="margin">Brain PC (Avanti #840053) - <a href="dat/PC_brain.zip" download>link</a></li>
                  <li class="margin">Brain PS (Avanti #840032) - <a href="" download>link</a></li>
                  <li class="margin">Egg PG (Avanti #841138) - <a href="" download>link</a></li>
                  <li class="margin">Egg PE (Avanti #841138) - coming soon</li>
                </ul>
              </li>
            </ul>

            <h4>Sample data upload</h4>

            <p>
              To run <i>LipidXte</i>, you must upload at least one Thermo *.raw file along with one *.csv the reference compounds. You can group your *.raw files into "group 1" for joint processing. Files can be easily added using the drag-and-drop feature. Please note the time to upload files may extend the overall processing duration. Once uploaded, the files will be processed. For instance, processing time for the samples of "Egg PC" is ~ 2 minutes and 40 seconds.
            </p>
<!--            <img src="~@/assets/lipidxte_instructions.gif" width="500px"/>-->
            <h4>Data processing by LipidXte2</h4>

            <p>
              The LipidXte web application processes data through a series of steps:
            </p>

            <ol class="margin">
              <li>Data Filtering: Input data from FT MS and HCD FTMS/MS are filtered to remove noise peaks using the <i>PeakStrainer</i>. The data is then converted to the *.mzXML format by the <i>RawFileReader</i><br/>
                <span style="font-size: smaller">(RawFileReader reading tool. Copyright © 2016 by Thermo Fisher Scientific, Inc. All rights reserved.)</span></li>
              <li>Data Analysis: The resulting *.mzXML files are imported into <i>LipidXplorer</i> for analysis. The software extracts the intensities of precursor ions, carboxylate anions, and potential CO2 loss fragments from all lipid species of the selected lipid class. In this process isotopic correction type 1 and 2 are applied.</li>
              <li>Intensity Correction and Normalization: LipidXte2 removes the fragmentation bias from the MS/MS data by adjusting the intensities of carboxylate anions using an external calibration database. The corrected fragment intensities are normalized based on specified internal standards, facilitating accurate quantification of molecular lipid species.</li>
            </ol>

            <p>
              Following, the users can review the processed data in the 'Result View' tab of the application.
            </p>

          </div>
        </div>

        <div class="panel panel-default">
          <div class="panel-heading title">Result View</div>
          <div class="panel-body" ref="doc3">

            The Result View tab offers an interface for browsing either user-uploaded or preprocessed shotgun lipidomics datasets. This section is structured to provide easy access to your lipidomics data.

            <h4>
              Data selection and visualization options
            </h4>

            <p>
              Within the batch field, you can select the dataset of interest.
            </p>

            <p>
              For the visualization, there are several options available:
            </p>

            <ul class="margin">
              <li class="margin"><b>Display Mode</b> Choose how you want the quantitative data to be displayed: either in mol% (Fraction) or μM (Concentration).</li>
              <li class="margin"><b>Remove References</b> Eliminates the reference compounds from the visualization.</li>
              <li class="margin"><b>No Correction</b> Displays the fragment data without any correction.</li>
              <li class="margin"><b>Thresholding</b> Excludes molecular species with less than 10% relative abundance compared to the most abundant analyte.</li>
            </ul>

            <p>
              Use the Quantify button to update the displayed data. To download the quantitative results for the precursor and corresponding molecular lipid species, click the Download button.
            </p>

            <p>
              <img src="~@/assets/fig-1.png"/>
              <figcaption>Figure 1: Result View window data selection and visualization options.</figcaption>
            </p>

            <h4>General Plot functionalities</h4>

            <p>
              All plots are generated using Plotly. You can interact with individual data points by hovering over them, select species of interest, modify, and export the results. To zoom in, hold down the left mouse button and select the region of interest. To zoom out, double-click on the plot. Basic functionalities include exporting plots as PNG (<i class="fa fa-camera" aria-hidden="true"/>) and accessing the data (<i class="fa fa-save" aria-hidden="true"/>).
            </p>

            <h4>Lipid species plot</h4>

            <p>
              In this section, the lipid species of a selected lipid class within a given sample are displayed. The results include readouts from FT MS and account for potential corrections for isotopic abundances and overlaps.
            </p>

            <p>
              <img src="~@/assets/fig-2.png" width="500px"/>
              <figcaption>Figure 2: Lipid species plot for Ultimate SPLASH One #2.</figcaption>
            </p>

            <h4>
              Molecular lipid species plot
            </h4>

            <p>
              In this tab, you can review the molecular lipid species of a selected lipid class within a given sample. The quantitative data displayed are based on the intensities of the carboxylate anion fragmentation, isotopic corrections (types 1 and 2), and the subsequent elimination of fragmentation bias by LipidXte2. Unique to shotgun lipidomics data, the sn1 and sn2 positions of the fatty acids are specified.
            </p>

            <p>
              <img src="~@/assets/fig-3.png" width="500px"/>
              <figcaption>Figure 3: Molecular lipid species plot for Ultimate SPLASH One #2.</figcaption>
            </p>

            <h4>
              Validation plots
            </h4>

            <p>
              These two plots allow you to review the results of LipidXte2.
            </p>

            <ul class="margin">
              <li class="margin">
                Quantification Plot (left): Displays the median quantity from FT MS (x-axis) compared to the corrected quantities from HCD FT MS/MS for all selected normalized collision energies (y-axis, blue dots). A linear fit (blue line) illustrates the correlation between the data. Red dots represent the results without correction for the highest selected collision energy.
              </li>
              <li class="margin">
                Error Plot (right): Shows the differences in quantification between MS1 and MS2. The error indicates the difference between MS2 and MS1, with data categorized by the number of double bonds in the analytes (0-1, 2-3, and more than 4). This plot demonstrates how the applied corrections reduce discrepancies between the two acquisition methods.
              </li>
            </ul>


            <p>
              <img src="~@/assets/fig-4.png" width="500px"/>
              <figcaption>Figure 4: Quantification plot for PG from Ultimate SPLASH One #2.</figcaption>
            </p>
            <p>
              <img src="~@/assets/fig-5.png" width="500px"/>
              <figcaption>Figure 5: Error plot for PG from Ultimate SPLASH One #2.</figcaption>
            </p>

          </div>
        </div>

        <div class="panel panel-default">
          <div class="panel-heading title">MS2 Calculator</div>
          <div class="panel-body" ref="doc5">

            <p>
              The MS2 Calculator allows access to the calibration functions underlying LipidXte2. It enables exploration of the impact of different collision energies on MS2 results and helps in selecting appropriate standards for targeted MS2 experiments.
            </p>
            <p>
              Calibrations for multiple lipid classes, including PC, PCO (both [M+HCO2]-), and PE, PEO, PG, PA, PI, and PS (all [M-H]-), are stored here. Each class can be combined with a set of approximately 50 fatty acids based on the user's needs.
            </p>

            <h4>Analyte selection</h4>

            <p>
              In the field below, users can select the analytes of interest one by one. After selecting the class and fatty acids/alcohols, click ‘Add & Show’ to display the data. Use the ‘Clear’ button to reset the page.
            </p>

            <p>
              <img src="~@/assets/fig-6.png"/>
              <figcaption>Figure 6: Analyte selection field.</figcaption>
            </p>

            <h4>General plot functionalities</h4>

            <p>
              See above.
            </p>

            <h4>Fragment intensity plot</h4>

            <p>
              The fragment intensity plot displays the intensities of fragments stored for the corresponding lipid class in the database. The data points are either experimental (annotated ‘-exp’, cross-shaped) or derived from polynomial calibration (dot-shaped). Users can select specific fragments and data types of interest by clicking on the legend. Fatty acid fragments and CO2 NL data are available for all lipid classes. Additional data for precursor and fragment signals are available for PCO.
            </p>

            <p>
              The cursor below the plot allows you to select the normalized collision energy of the combined FTMS2 spectra by adjusting its position.
            </p>

            <p>
              <img src="~@/assets/fig-7.png"/>
              <figcaption>Figure 7: Fragment intensity plot showing the fragment intensities for FA 16:0 (experimental and calibration data) and FA 22:6 (4z) (calibration data only). Faded elements could be selected by the used based on the needs.</figcaption>
            </p>

            <h4>Combined MS2 spectra</h4>

            <p>
              The combined MS2 spectra display an overlaid MS2 spectrum or a cross-section of the fragment intensity plot data. The plot content depends on the selected analytes and the chosen collision energy. This feature allows users to estimate the fragmentation differences or bias between the selected analytes.
            </p>

            <p>
              <img src="~@/assets/fig-8.png"/>
              <figcaption>Figure 8: Combined MS2 spectra for PCO-16:0/16:0 and PCO-16:0/22:6 (4z) at a normalized collision energy of 30%. The fragment intensity of FA 22:6 (4z) at m/z 327 is approximately 4 times lower than that of FA 16:0 at m/z 255, given the same precursor intensity. Therefore, standards with saturated fatty acids are not ideal for quantifying lipids containing FA 22:6 (4z).</figcaption>
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

.octicon {
  display: inline-block;
  overflow: visible !important;
  vertical-align: text-bottom;
  fill: currentColor;
}

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

main > div { flex-basis: 90%; }

.doc_help {
display: flex;
flex-direction: column;
}

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

.panel-body figcaption {
  font-style: italic;
}

.panel-body b {
  padding: 1px 4px;
  font-size: 90%;
  color: #fff;
  background-color: #444;
  border-radius: 3px;
  -webkit-box-shadow: inset 0 -1px 0 rgba(0, 0, 0, .25);
  box-shadow: inset 0 -1px 0 rgba(0, 0, 0, .25);
}

.doc p {
  color: black;
  margin-bottom: 10px;
}

.doc_help button {
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

.doc_help input {
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

.doc_help select {
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

.doc_help select.timestamp {
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

.doc_help .vue-input-tag-wrapper {
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

.doc_help button.alt {
  color: #42b983;
  background-color: transparent;
}

::selection {
  background:rgba(255, 255, 125, 0.99);
  color:#032764;
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
