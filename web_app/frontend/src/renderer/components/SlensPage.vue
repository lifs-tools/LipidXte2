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
      <li role="presentation" class="active">
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
      <li role="presentation">
        <router-link :to="'/help?timestamp=' + $route.query.timestamp" v-if="$route.query.timestamp">
          Help
        </router-link>
        <router-link to="/help" v-else>
          Help
        </router-link>
      </li>
    </ul>

    <br/>

    <div class="doc">

      <label for="batch-id">Batch:</label>

      <select id="batch-id" class="timestamp" v-model="timestamp">
        <option disabled value="">Please select one in the latest batches</option>
        <option v-for="batch in batchlist" v-bind:value="batch.value"
                :selected="batch.text === batch_title">
          {{ batch.text }}
        </option>
      </select>
    </div>

    <div>
      <label>Batch ID:</label>&nbsp;<span>{{timestamp}}_{{clazz}}</span>
    </div>

    <table>
      <tr>
        <td>
          <div class="panel panel-warning" style="height: 140px">
            <div class="panel-heading">Quantification</div>
            <div class="panel-body option-box">
              <!--<input type="radio" id="intensity" value="Intensity" v-model="quantOption">-->
              <!--<label for="intensity">Intensity</label><br/>-->
              <input type="radio" id="profile" value="Profile" v-model="quantOption">
              <label for="profile">Profile</label><br/>
              <input type="radio" id="quantity" value="Quantity" v-model="quantOption">
              <label for="quantity">Quantity</label>
            </div>
          </div>
        </td>
        <td>
          <div class="panel panel-info" style="height: 140px">
            <div class="panel-heading">Process Options</div>
            <div class='panel-body option-box'>
              <input type="checkbox" id="remove-ref" value="RemoveRef" v-model="options">
              <label for="remove-ref">Remove References</label><br/>
              <!--<input type="checkbox" id="summarize-nce" value="SummarizeNCE" v-model="options">-->
              <!--<label for="summarize-nce">Summarize NCE</label><br/>-->
              <input type="checkbox" id="no-correction" value="NoCorrection" v-model="options">
              <label for="no-correction">No Correction</label><br/>
              <!--<input type="checkbox" id="group-only" value="GroupOnly" v-model="options">-->
              <!--<label for="group-only">Group Only</label>-->
              <input type="checkbox" v-model="thresholded"> <label>Thresholding</label>
            </div>
          </div>
        </td>
        <!--<td>-->
        <!--<div class="panel panel-success" style="height: 160px">-->
        <!--<div class="panel-heading">Output Format</div>-->
        <!--<div class="panel-body option-box">-->
        <!--<input type="radio" id="all" value="All" v-model="outputOption">-->
        <!--<label for="all">All</label><br/>-->
        <!--<input type="radio" id="sum" value="Sum" v-model="outputOption">-->
        <!--<label for="sum">Sum</label><br/>-->
        <!--<input type="radio" id="mspecies" value="Mspecies" v-model="outputOption">-->
        <!--<label for="mspecies">Mspecies</label>-->
        <!--</div>-->
        <!--</div>-->
        <!--</td>-->
      </tr>
      <tr>
        <td>
          <button type="button" class="btn btn-primary" @click="quantify">Quantify</button>
        </td>
        <td>
          <button type="button" class="btn btn-success" @click="download">Download</button>
        </td>
        <!--<td>-->
        <!--<button type="button" class="btn btn-danger" @click="quantify">Test</button>-->
        <!--</td>-->
        <a ref="tsv"></a>
      </tr>
    </table>

    <br/>

    <div class="tab">
      <button v-bind:class="{ active: clazz === 'PE' }" @click="clazz = 'PE';quantify()">PE</button>
      <button v-bind:class="{ active: clazz === 'PC' }" @click="clazz = 'PC';quantify()">PC</button>
      <button v-bind:class="{ active: clazz === 'PG' }" @click="clazz = 'PG';quantify()">PG</button>
      <button v-bind:class="{ active: clazz === 'PI' }" @click="clazz = 'PI';quantify()">PI</button>
      <button v-bind:class="{ active: clazz === 'PS' }" @click="clazz = 'PS';quantify()">PS</button>
    </div>

    <!-- Tab content -->
    <div style="display: flex;flex-direction: row;">
      <div class="tabcontent" v-bind:style="{ display: 'block', width: '50%' }">
        <table border="0px">
          <tr>
            <td></td><td height="5px" width="50px"></td><td></td><td rowspan="3">&nbsp; Max</td>
          </tr>
          <tr>
            <td width="1px" rowspan="7" style="background-color: #2c3e50"></td>
            <td height="1px" style="background-color: #2c3e50"></td>
            <td width="1px" rowspan="7" style="background-color: #2c3e50"></td>
          </tr>
          <tr>
            <td height="5px" style="background-color: #3e9ee7"></td>
          </tr>
          <tr>
            <td height="5px" style="background-color: #3e9ee7"></td>
            <td rowspan="3">&nbsp; Median</td>
          </tr>
          <tr>
            <td height="1px" style="background-color: #2c3e50"></td>
          </tr>
          <tr>
            <td height="5px" style="background-color: #3e9ee7"></td>
          </tr>
          <tr>
            <td height="5px" style="background-color: #3e9ee7"></td>
            <td rowspan="3">&nbsp; Min</td>
          </tr>
          <tr>
            <td height="1px" style="background-color: #2c3e50"></td>
          </tr>
          <tr>
            <td width="1px"></td><td height="5px"></td><td width="1px"></td>
          </tr>
        </table>

        <div ref="lspecies"></div>
      </div>

      <div class="tabcontent" v-bind:style="{ display: 'block', width: '50%' }">

        <table border="0px">
          <tr>
            <td></td><td height="5px" width="50px"></td><td></td><td rowspan="3">&nbsp; Max</td>
          </tr>
          <tr>
            <td width="1px" rowspan="7" style="background-color: #2c3e50"></td>
            <td height="1px" style="background-color: #2c3e50"></td>
            <td width="1px" rowspan="7" style="background-color: #2c3e50"></td>
          </tr>
          <tr>
            <td height="5px" style="background-color: #3e9ee7"></td>
          </tr>
          <tr>
            <td height="5px" style="background-color: #3e9ee7"></td>
            <td rowspan="3">&nbsp; Median</td>
          </tr>
          <tr>
            <td height="1px" style="background-color: #2c3e50"></td>
          </tr>
          <tr>
            <td height="5px" style="background-color: #3e9ee7"></td>
          </tr>
          <tr>
            <td height="5px" style="background-color: #3e9ee7"></td>
            <td rowspan="3">&nbsp; Min</td>
          </tr>
          <tr>
            <td height="1px" style="background-color: #2c3e50"></td>
          </tr>
          <tr>
            <td width="1px"></td><td height="5px"></td><td width="1px"></td>
          </tr>
        </table>

        <span>{{nceString}}</span>

        <div ref="mspecies"></div>
      </div>
    </div>

    <div style="display: flex;flex-direction: row;">
      <div class="tabcontent" v-bind:style="{ display: 'block', width: '50%' }">
        <table>
          <tr><td>
            R<sup>2</sup> = {{r2}}
          </td></tr>
          <tr><td>
            Slope = {{slope}}
          </td></tr>
          <tr><td>
            Intercept = {{intercept}}
          </td></tr>
          <tr>
            <input type="checkbox" v-model="isLog"> Logarithm
          </tr>
        </table>
        <div ref="validation1"></div>
      </div>

      <div class="tabcontent" v-bind:style="{ display: 'block', width: '50%' }">
        <div ref="validation1Error"></div>
      </div>
    </div>

    <div style="display: flex;flex-direction: row;">
      <div v-show="timestamp.endsWith('ultimate')" class="tabcontent additional-validation">
        <table>
          <tr><td>
            R<sup>2</sup> = {{stdR2}}
          </td></tr>
          <tr><td>
            Slope = {{stdSlope}}
          </td></tr>
          <tr><td>
            Intercept = {{stdIntercept}}
          </td></tr>
        </table>
        <div ref="validation2"></div>
      </div>
    </div>


  </div>
</template>

<script>
  import 'font-awesome/css/font-awesome.css'
  import { serverUrl } from './conf'

  let Plotly = require('./box')
  let d3 = require('d3-dsv')
  let _ = require('lodash')

  export default {
    name: 'sample-page',
    data: function () {
      return {
        // options: ['RemoveRef', 'SummarizeNCE', 'GroupOnly'],
        options: ['SummarizeNCE'],
        batchlist: [],
        correctedSum: {},
        uncorrectedSum: {},
        quantOption: 'Quantity',
        outputOption: 'All',
        timestamp: '',
        isLspecies: false,
        preLspecies: false,
        isMspecies: false,
        preMspecies: false,
        isValidation: false,
        preValidation: false,
        r2: 0,
        slope: 0,
        intercept: 0,
        stdR2: 0,
        stdSlope: 0,
        stdIntercept: 0,
        nceString: '',
        clazz: 'PE',
        thresholded: true,
        isLog: false,
        standardList: [
          {
            'class': 'PC',
            'specie': 'PC 31:1:0',
            'mol': 69.1537
          },
          {
            'class': 'PC',
            'specie': 'PC 33:1:0',
            'mol': 133.141
          },
          {
            'class': 'PC',
            'specie': 'PC 35:1:0',
            'mol': 192.521
          },
          {
            'class': 'PC',
            'specie': 'PC 37:3:0',
            'mol': 124.509
          },
          {
            'class': 'PC',
            'specie': 'PC 39:4:0',
            'mol': 60.2996
          },
          {
            'class': 'PE',
            'specie': 'PE 31:1:0',
            'mol': 36.7135
          },
          {
            'class': 'PE',
            'specie': 'PE 33:1:0',
            'mol': 70.5218
          },
          {
            'class': 'PE',
            'specie': 'PE 35:1:0',
            'mol': 101.756
          },
          {
            'class': 'PE',
            'specie': 'PE 37:3:0',
            'mol': 65.6965
          },
          {
            'class': 'PE',
            'specie': 'PE 39:4:0',
            'mol': 31.7617
          },
          {
            'class': 'PG',
            'specie': 'PG 31:1:0',
            'mol': 34.0627
          },
          {
            'class': 'PG',
            'specie': 'PG 33:1:0',
            'mol': 65.6174
          },
          {
            'class': 'PG',
            'specie': 'PG 35:1:0',
            'mol': 94.9311
          },
          {
            'class': 'PG',
            'specie': 'PG 37:3:0',
            'mol': 61.4199
          },
          {
            'class': 'PG',
            'specie': 'PG 39:4:0',
            'mol': 29.6869
          },
          {
            'class': 'PS',
            'specie': 'PS 31:1:0',
            'mol': 33.47
          },
          {
            'class': 'PS',
            'specie': 'PS 33:1:0',
            'mol': 64.5168
          },
          {
            'class': 'PS',
            'specie': 'PS 35:1:0',
            'mol': 93.3945
          },
          {
            'class': 'PS',
            'specie': 'PS 37:3:0',
            'mol': 60.4546
          },
          {
            'class': 'PS',
            'specie': 'PS 39:4:0',
            'mol': 29.2357
          },
          {
            'class': 'PI',
            'specie': 'PI 31:1:0',
            'mol': 30.5979
          },
          {
            'class': 'PI',
            'specie': 'PI 33:1:0',
            'mol': 59.1643
          },
          {
            'class': 'PI',
            'specie': 'PI 35:1:0',
            'mol': 85.8952
          },
          {
            'class': 'PI',
            'specie': 'PI 37:3:0',
            'mol': 55.7303
          },
          {
            'class': 'PI',
            'specie': 'PI 39:4:0',
            'mol': 27.0793
          }
        ]
      }
    },
    watch: {
      isLog: function (newValue) {
        const vm = this

        vm.getValidation()
      }
    },
    methods: {
      quantify () {
        let vm = this

        console.log(this.quantOption)
        console.log(this.options)
        console.log(this.outputOption)

        // console.log(this.options.indexOf('NoCorrection'))
        // Checking the server response
        vm.getMSpecies()
        vm.getValidation()
        // let copy = vm.options.slice()
        // copy.splice(copy.indexOf('NoCorrection'), 1)
        // console.log(copy)
      },
      extractFAI (dat) {
        let val = []
        let names = []

        //         _.filter(Object.keys(dat), (o) => o.startsWith('FAI.') && !o.startsWith('FAI.FC_Group'))
        //           .forEach((c) => {
        //             console.log(c)
        //             if (dat[c] !== '') {
        //               console.log(dat[c])
        //               val = _.concat(val, eval(dat[c].replace(/\.\./g, ',')))
        //               names = _.concat(names, )
        //             }
        //           }
        //         )
        //         console.log(dat)

        _.filter(Object.keys(dat), (o) => o.startsWith('FAI.FC_Group'))
          .forEach((c) => {
            // console.log(c)
            if (dat[c] !== '') {
              val = _.concat(val, eval(dat[c].replace(/\.\./g, ',')))
              // console.log(val)
              names.push(c)
              names.push(c)

              // console.log(names)
              // names = _.concat(_.concat(names, c), c)
            }
          })

        // let min = _.min(val)
        // let max = _.max(val)
        // let avg = _.sum(val) / val.length

        // console.log(names)
        return [val, names]
      },
      averagePRI (dat) {
        let pri = []

        _.filter(Object.keys(dat), (o) => o.startsWith('PRI_') && !o.startsWith('PRI_Group'))
          .forEach((c) => {
            if (dat[c] !== '') {
              pri.push(+dat[c])
            }
          })

        let val = _.reduce(pri, (s, n) => s + n, 0)

        return val / pri.length
      },
      averageFAI (dat) {
        let fai = []

        _.filter(Object.keys(dat), (o) => o.startsWith('FAI.') && !o.startsWith('FAI.FC_Group'))
          .forEach((c) => {
            if (dat[c] !== '') {
              fai = _.concat(fai, eval(dat[c].replace(/\.\./g, ',')))
            }
          })

        let sum = _.reduce(fai, (s, n) => s + n, 0)

        return sum / fai.length
      },
      getMSpecies () {
        let vm = this

        if (vm.timestamp === 'ILIS' || vm.timestamp === 'brain' || vm.timestamp === 'egg' || vm.timestamp === 'heart') {
          vm.clazz = 'PC'
        } else if (vm.timestamp === 'egg_PG') {
          vm.clazz = 'PG'
        } else if (vm.timestamp === 'egg_PE' || vm.timestamp === 'egg_PEO') {
          vm.clazz = 'PE'
        } else if (vm.timestamp === 'brain_PS') {
          vm.clazz = 'PS'
        }
        let oReq = new XMLHttpRequest()
        oReq.responseType = 'json'
        oReq.onload = function (e) {
          if (this.status === 200) {
            let jsonReponse = this.response
            let nceHeader = jsonReponse[0]
            vm.nceString = 'NCE: ' + nceHeader
            // console.log(nceHeader)

            let tsv = jsonReponse[1]
            let headerline = tsv.slice(0, tsv.indexOf('\n') + 1).split('\t')
            // console.log(headerline)

            const isStandardListPresent = jsonReponse[2]

            let headers = []
            _.filter(headerline, (o) => o.startsWith('PRI_Group'))
              .forEach((c) => {
                headers.push(c)
              })

            // console.log(headers)

            let groupSize = headers.length

            let data = d3.tsvParse(tsv, function (d) {
              if (d.Mspecies.startsWith(vm.clazz) && d.Mspecies !== 'Sum') {
                let faiArr = vm.extractFAI(d)

                if (groupSize > 1) {
                  return {
                    type: 'box',
                    y: faiArr[0],
                    x: faiArr[1],
                    // orientation: 'h',
                    name: d.Mspecies
                  }
                } else {
                  return {
                    type: 'box',
                    y: faiArr[0],
                    // orientation: 'h',
                    name: d.Mspecies
                  }
                }
              }
            })

            if (vm.thresholded) {
              if (vm.quantOption === 'Quantity') {
                data = _.filter(data, (i) => i.y[1] > 10)
              } else if (vm.quantOption === 'Profile') {
                data = _.filter(data, (i) => i.y[1] > 3)
              }
            }

            let sum = d3.tsvParse(tsv, function (d) {
              if (d.Species.startsWith(vm.clazz) && d.Mspecies === 'Sum') {
                return {
                  fai: vm.averageFAI(d),
                  pri: vm.averagePRI(d),
                  species: d.Species
                }
              }
            })

            const pspg = (item) => {
              const specie = item.species
              if (vm.timestamp.endsWith('PS') || vm.timestamp.endsWith('PG')) {
                let m = /.* (\d+):(\d+):(\d+)/gm.exec(specie)
                let c = parseInt(m[1])
                return c <= 40 && c % 2 === 0
              } else {
                return true
              }
            }

            sum = sum.filter(c => pspg(c))

            let nce = 'NCE ' + nceHeader.replace(/\[|\]/g, '') + ' '

            if (vm.options.indexOf('NoCorrection') > -1) {
              // no correction
              vm.uncorrectedSum = { x: _.map(sum, 'pri'), y: _.map(sum, 'fai'), text: _.map(sum, 'species').map(c => c + '<br>' + nce + 'uncorrected'), species: _.map(sum, 'species') }
            } else {
              // correction
              vm.correctedSum = { x: _.map(sum, 'pri'), y: _.map(sum, 'fai'), text: _.map(sum, 'species').map(c => c + '<br>' + nce + 'corrected'), species: _.map(sum, 'species') }
            }

            let yTitle = 'Concentration, microM'
            if (vm.quantOption !== 'Quantity') {
              yTitle = 'Concentration, mol %'
            }

            let newTitle = 'Molecular Species'
            if (vm.options.indexOf('NoCorrection') > -1 &&
              vm.options.indexOf('RemoveRef') > -1) {
              newTitle += ' (RemoveRef, No Correction)'
            } else if (vm.options.indexOf('NoCorrection') > -1) {
              newTitle += ' (No Correction)'
            } else if (vm.options.indexOf('RemoveRef') > -1) {
              newTitle += ' (RemoveRef)'
            }

            if (vm.thresholded) {
              if (vm.quantOption === 'Quantity') {
                newTitle += '<br><sup>Species below 10 percent are not shown</sup>'
              } else if (vm.quantOption === 'Profile') {
                newTitle += '<br><sup>Species below 3 percent are not shown</sup>'
              }
            }

            let layout = {
              title: newTitle,
              height: 700,
              margin: {b: 180},
              xaxis: {title: 'Molecular species'},
              yaxis: {title: yTitle, rangemode: 'tozero'},
              boxmode: 'group'
            }

            Plotly.newPlot(vm.$refs.mspecies, data, layout)

            // console.log(data)

            // Draw lspecies
            data = [
              {
                x: _.map(sum, 'species'),
                y: _.map(sum, 'pri'),
                mode: 'lines+markers',
                type: 'bar'
              }
            ]

            layout = {
              title: 'Lipid Species',
              height: 700,
              margin: {b: 180},
              xaxis: {title: 'Lipid species'},
              yaxis: {title: isStandardListPresent ? 'Concentration, microM' : 'Concentration, percent(%)'}
            }

            Plotly.newPlot(vm.$refs.lspecies, data, layout)
          } else {
            console.error(this.response)
          }
        }
        oReq.open('GET', serverUrl + '/ultimate')
        oReq.setRequestHeader('timestamp', vm.timestamp)
        oReq.setRequestHeader('quantOption', vm.quantOption)
        oReq.setRequestHeader('options', vm.options.sort().join('_'))
        oReq.setRequestHeader('outputOption', vm.outputOption)
        oReq.send()
      },
      getValidation () {
        let vm = this
        let oReq = new XMLHttpRequest()
        oReq.responseType = 'json'
        oReq.onload = function (e) {
          if (this.status === 200) {
            let jsonReponse = this.response
            // console.log(jsonReponse)
            let nceHeader = jsonReponse[0]

            let sum = d3.tsvParse(jsonReponse[1], function (d) {
              if (d.Species.startsWith(vm.clazz) && d.Mspecies === 'Sum') {
                return {
                  fai: vm.averageFAI(d),
                  pri: vm.averagePRI(d),
                  species: d.Species
                }
              }
            })

            const pspg = (item) => {
              const specie = item.species
              if (vm.timestamp.endsWith('PS') || vm.timestamp.endsWith('PG')) {
                let m = /.* (\d+):(\d+):(\d+)/gm.exec(specie)
                let c = parseInt(m[1])
                return c <= 40 && c % 2 === 0
              } else {
                return true
              }
            }

            sum = sum.filter(c => pspg(c))

            let nce = 'NCE ' + nceHeader.replace(/\[|\]/g, '') + ' '

            if (vm.options.indexOf('NoCorrection') > -1) {
              // no correction
              vm.correctedSum = { x: _.map(sum, 'pri'), y: _.map(sum, 'fai'), text: _.map(sum, 'species').map(c => c + '<br>' + nce + 'corrected'), species: _.map(sum, 'species') }
            } else {
              // correction
              vm.uncorrectedSum = { x: _.map(sum, 'pri'), y: _.map(sum, 'fai'), text: _.map(sum, 'species').map(c => c + '<br>' + 'NCE ' + nce.substr(nce.replace(/\[|\]/g, '').lastIndexOf(',') + 2) + 'uncorrected'), species: _.map(sum, 'species') }
            }

            // console.log(_.map(sum, 'species').map(c => c))

            const species = sum.map(c => c.species)
            const sortedStandard = vm.standardList.filter(c => c.class === vm.clazz)

            // console.log(species)
            // console.log(sortedStandard)

            let standardDat = sortedStandard.filter(c => species.indexOf(c.specie) > -1).map(c => c.mol)
            // standardDat.sort((a, b) => a - b)
            // vm.correctedSum.y.sort((a, b) => a - b)
            // vm.correctedSum.x.sort((a, b) => a - b)

            let corrected = {
              x: vm.correctedSum.x,
              y: vm.correctedSum.y,
              mode: 'markers',
              name: nce + 'corrected',
              hovertext: vm.correctedSum.text,
              hoverinfo: 'x+y+text',
              marker: {
                color: 'rgb(55, 128, 191)',
                size: 5
              },
              // line: {
              //   shape: 'spline',
              //   color: 'rgb(55, 128, 191)',
              //   width: 3
              // },
              type: 'scatter'
            }

            // console.log(nceHeader.replace(/\[|\]/g, ''))

            let nocorrected = {
              x: vm.uncorrectedSum.x,
              y: vm.uncorrectedSum.y,
              mode: 'markers',
              name: 'NCE ' + nce.replace(/\[|\]/g, '').substr(nce.replace(/\[|\]/g, '').lastIndexOf(',') + 2) + 'uncorrected',
              hovertext: vm.uncorrectedSum.text,
              hoverinfo: 'x+y+text',
              marker: {
                color: 'rgb(219, 64, 82)',
                size: 4
              },
              //              line: {
              //                shape: 'spline',
              //                color: 'rgb(219, 64, 82)',
              //                width: 3
              //              },
              type: 'scatter'
            }

            let targetWithMolecularSpecies = {
              x: standardDat,
              y: vm.correctedSum.y,
              mode: 'markers',
              name: 'MS2/fragment',
              hovertext: vm.standardList.filter(c => c.class === vm.clazz).map(c => c.specie),
              hoverinfo: 'x+y+text',
              marker: {
                color: 'rgb(57,229,10)',
                size: 4
              },
              type: 'scatter'
            }

            let targetWithSpecies = {
              x: standardDat,
              y: vm.correctedSum.x,
              mode: 'markers',
              name: 'FTMS1/precursor',
              hovertext: vm.standardList.filter(c => c.class === vm.clazz).map(c => c.specie),
              hoverinfo: 'x+y+text',
              marker: {
                color: 'rgb(10,98,229)',
                size: 4
              },
              type: 'scatter'
            }

            let standardConcentration = {
              x: standardDat,
              y: standardDat,
              mode: 'markers',
              name: 'Target value',
              hovertext: vm.standardList.filter(c => c.class === vm.clazz).map(c => c.specie),
              hoverinfo: 'x+y+text',
              marker: {
                color: 'rgb(231,38,192)',
                size: 4
              },
              type: 'scatter'
            }

            let regression = require('regression')

            let dat = _.zip(vm.correctedSum.x, vm.correctedSum.y)

            let result = regression.linear(dat)

            // console.log('R2 = ' + result.r2)
            vm.r2 = result.r2
            vm.slope = result.equation[0]
            vm.intercept = result.equation[1]

            let fitModel = [...vm.correctedSum.x]
            fitModel.sort((a, b) => a - b)

            let fitted = {
              x: fitModel,
              y: _.zipWith(fitModel, function (a) {
                return result.predict(a)[1]
              }),
              name: 'Fitted line for corrected species',
              mode: 'lines',
              text: 'Fitted line for corrected species',
              hoverinfo: 'x+y+text',
              line: {
                shape: 'spline',
                color: 'rgb(55, 130, 195)',
                width: 3
              }
            }

            let orientation = {
              x: [0, 500],
              y: [0, 500],
              showlegend: false,
              hoverinfo: 'skip',
              line: {
                shape: 'spline',
                color: 'rgba(55, 55, 55, 0.3)',
                width: 1
              }
            }

            if (vm.isLog) {
              let logLayout = {
                title: 'Quantification via FTMS vs FTMSMS (log)',
                xaxis: {type: 'log', rangemode: 'tozero', zeroline: true, title: 'Specified concentration via FTMS1/precursor, microM'},
                yaxis: {type: 'log', rangemode: 'tozero', zeroline: true, title: 'Specified concentration via FTMS2/fragment, microM'}
              }

              Plotly.newPlot(vm.$refs.validation1, [corrected, nocorrected, fitted, orientation], logLayout)
            } else {
              let layout = {
                // title: 'Validation Figure NCE:' + nceHeader,
                title: 'Quantification via FTMS vs FTMSMS',
                xaxis: {range: [0, 200], rangemode: 'tozero', zeroline: true, title: 'Specified concentration via FTMS1/precursor, microM'},
                yaxis: {range: [0, 200], rangemode: 'tozero', zeroline: true, title: 'Specified concentration via FTMS2/fragment, microM'}
              }

              Plotly.newPlot(vm.$refs.validation1, [corrected, nocorrected, fitted, orientation], layout)
            }

            dat = _.zip(standardDat, vm.correctedSum.x)

            result = regression.linear(dat)

            // console.log('R2 = ' + result.r2)
            vm.stdR2 = result.r2
            vm.stdSlope = result.equation[0]
            vm.stdIntercept = result.equation[1]
            // console.log(result)

            let fitted2 = {
              x: standardDat,
              y: _.zipWith(standardDat, function (a) {
                return result.predict(a)[1]
              }),
              name: 'Fitted line for corrected species',
              mode: 'lines',
              text: 'Fitted line for corrected species',
              hoverinfo: 'x+y+text',
              line: {
                shape: 'spline',
                color: 'rgb(55, 130, 195)',
                width: 3
              }
            }

            let layout2 = {
              // title: 'Validation Figure NCE:' + nceHeader,
              title: 'Target vs specified concentration',
              xaxis: {rangemode: 'tozero', zeroline: true, range: [0, 200], title: 'Target concentration, microM'},
              yaxis: {rangemode: 'tozero', zeroline: true, range: [0, 200], title: 'Specified concentration, microM'}
            }
            Plotly.newPlot(vm.$refs.validation2, [standardConcentration, targetWithSpecies, targetWithMolecularSpecies, fitted2, orientation], layout2)

            if (vm.uncorrectedSum) {
              // console.log(vm.uncorrectedSum)

              let ucdat = _.zip(vm.uncorrectedSum.x, vm.uncorrectedSum.y, vm.uncorrectedSum.species).filter(c => c[0] > 1 && c[1] / c[0] * 100 - 100 <= 100)
              // console.log(ucdat)

              let species = _.map(ucdat, c => c[2])
              // console.log(species)

              let cdat = _.zip(vm.correctedSum.x, vm.correctedSum.y, vm.correctedSum.species).filter(c => species.includes(c[2]))
              // console.log(cdat)

              // console.log(vm.correctedSum)
              // console.log(vm.uncorrectedSum)
              // console.log(vm.uncorrectedSum.species)
              // console.log(noOfDoubleBonds.map(c => c.fragment))
              // console.log(vm.uncorrectedSum.species)

              let x = ucdat.map(c => {
                let m = /.*:(\d+):(\d+)/gm.exec(c[2])
                // console.log(m[1])

                let dbno = parseInt(m[1])

                if (dbno < 2) {
                  return '0-1'
                } else if (dbno < 4) {
                  return '2-3'
                } else {
                  return '4-7'
                }
              })
              // console.log(x)

              // x = ['0-1', '0-1', '0-1', '2-3', '4-7']

              let c1 =
                {
                  type: 'box',
                  y: cdat.map(c => c[1] / c[0] * 100 - 100),
                  x,
                  // orientation: 'h',
                  name: 'corrected'
                }
              let c3 =
                {
                  type: 'box',
                  y: ucdat.map(c => c[1] / c[0] * 100 - 100),
                  x,
                  // orientation: 'h',
                  name: 'uncorrected',
                  marker: {color: '#FF4136'}
                }
              let errorLayout = {
                // title: 'Validation Figure NCE:' + nceHeader,
                title: 'Error over the number of double bonds',
                xaxis: {range: [-0.5, 3], title: 'Number of double bonds'},
                yaxis: {rangemode: 'tozero', zeroline: true, title: 'Error, %'},
                boxmode: 'group'
              }
              Plotly.newPlot(vm.$refs.validation1Error, [c1, c3], errorLayout)
            }
          } else {
            console.error(this.responseText)
          }
        }
        oReq.open('GET', serverUrl + '/ultimate')
        oReq.setRequestHeader('timestamp', vm.timestamp)
        oReq.setRequestHeader('quantOption', vm.quantOption)
        if (vm.options.indexOf('NoCorrection') > -1) {
          // no correction
          let copy = vm.options.slice()
          copy.splice(copy.indexOf('NoCorrection'), 1)
          oReq.setRequestHeader('options', copy.sort().join('_'))
        } else {
          // correction
          let copy = vm.options.slice()
          copy.push('NoCorrection')
          oReq.setRequestHeader('options', copy.sort().join('_'))
        }
        oReq.setRequestHeader('outputOption', vm.outputOption)
        oReq.send()
      },
      download () {
        let vm = this

        let reqHeaders = {}
        reqHeaders['timestamp'] = vm.timestamp
        reqHeaders['quantOption'] = vm.quantOption
        let copy = vm.options.slice()
        reqHeaders['options'] = copy.sort().join('_')
        reqHeaders['outputOption'] = vm.outputOption

        vm.$http.get(serverUrl + '/download-ultimate', { responseType: 'arraybuffer', headers: reqHeaders })
          .then(response => {
          //            console.log(response.headers)
            let blob = new Blob([response.data], {type: response.headers['content-type']})

            let contentDisposition = response.headers['content-disposition'] || ''
            let filename = contentDisposition.split('filename=')[1]
            filename = filename.replace(/"/g, '')

            let link = vm.$refs.tsv
            link.href = window.URL.createObjectURL(blob)
            link.download = filename
            link.click()
          })
      }
    },
    props: ['batch_title'],
    mounted: function () {
      let vm = this
      vm.batchlist = [
        {text: 'Heart PC', value: 'heart'},
        {text: 'Egg PC', value: 'egg'},
        {text: 'Brain PC', value: 'brain'},
        {text: 'Custom PC mix', value: 'ILIS'},
        {text: 'Ultimate SPLASH #1', value: 'ultimate'},
        {text: 'Ultimate SPLASH #2', value: '2nd_ultimate'},
        {text: 'Brain PS', value: 'brain_PS'},
        {text: 'Egg PG', value: 'egg_PG'},
        {text: 'Egg PE', value: 'egg_PE'}
        // {text: 'Egg PEO', value: 'egg_PEO'}
      ]
      vm.timestamp = 'ultimate'

      vm.getMSpecies()
      vm.getValidation()
    }
  }
</script>

<style>
  @import url('~@/assets/bootstrap.css');

  /* Style the tab */
  .tab {
    overflow: hidden;
    border: 1px solid #ccc;
    background-color: #f1f1f1;
  }

  /* Style the buttons that are used to open the tab content */
  .tab button {
    background-color: inherit;
    float: left;
    border: none;
    outline: none;
    cursor: pointer;
    padding: 14px 16px;
    transition: 0.3s;
  }

  /* Change background color of buttons on hover */
  .tab button:hover {
    background-color: #ddd;
  }

  /* Create an active/current tablink class */
  .tab button.active {
    background-color: #ccc;
  }

  /* Style the tab content */
  .tabcontent {
    display: none;
    padding: 6px 12px;
    border: 1px solid #ccc;
    border-top: none;
  }

  .additional-validation {
    display: block;
    width: 100%;
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

</style>
