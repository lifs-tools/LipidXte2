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
      <li role="presentation" v-show="$route.query.timestamp" class="active">
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
      <label>Batch ID:</label>&nbsp;<span>{{timestamp}}</span>
    </div>

    <table>
      <tr>
        <td>
          <div class="panel panel-warning" style="height: 120px">
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
          <div class="panel panel-info" style="height: 120px">
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
          <button type="button" class="btn btn-primary" @click="quantify" :disabled="processing">Quantify</button>
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

    <!-- Tab links -->
    <div class="tab">
      <button class="tablinks" v-bind:class="{ active: isLspecies }" @click="openTab('LSpecies')">Lipid Species</button>
      <button class="tablinks" v-bind:class="{ active: isMspecies }" @click="openTab('MSpecies')">Molecular Species</button>
      <button class="tablinks" v-bind:class="{ active: isValidation }" @click="openTab('Validation')">Validation</button>
    </div>
    <!-- Tab content -->
    <div class="tabcontent" v-bind:style="{ display: isLspecies?'block':'none' }">

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

    <div class="tabcontent" v-bind:style="{ display: isMspecies?'block':'none' }">

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
    <div class="tabcontent" v-bind:style="{ display: isValidation?'block':'none' }">

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
      </table>

      <div ref="validation"></div>
    </div>

    <modal v-if="processing" @close="processing = false">
      <!--
        you can use custom content here to overwrite
        default content
      -->
      <h3 slot="header"><i class="fa fa-gear fa-spin" aria-hidden="true"/> LipidXte Web</h3>
      <h4 slot="body">Quantifying your data ...</h4>
      <p slot="footer">This window will be closed automatically soon.</p>
    </modal>
  </div>
</template>

<script>
  import 'font-awesome/css/font-awesome.css'
  import { serverUrl } from './conf'
  import modal from './ModalWindow.vue'

  let Plotly = require('./box')
  let d3 = require('d3-dsv')
  let _ = require('lodash')

  export default {
    name: 'sample-page',
    components: {modal},
    data: function () {
      return {
        // options: ['RemoveRef', 'SummarizeNCE', 'GroupOnly'],
        options: ['SummarizeNCE'],
        batchlist: [],
        correctedSum: {},
        uncorrectedSum: {},
        quantOption: 'Quantity',
        outputOption: 'All',
        timestamp: this.$route.query.timestamp,
        isLspecies: false,
        isMspecies: false,
        isValidation: false,
        r2: 0,
        slope: 0,
        intercept: 0,
        nceString: '',
        processing: false
      }
    },
    methods: {
      quantify () {
        let vm = this
        vm.processing = true
        console.log(this.quantOption)
        console.log(this.options)
        console.log(this.outputOption)

        // console.log(this.options.indexOf('NoCorrection'))
        // Checking the server response
        vm.isLspecies = true
        vm.isMspecies = true
        vm.isValidation = true
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
          }
          )

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
          }
          )

        let sum = _.reduce(fai, (s, n) => s + n, 0)

        return sum / fai.length
      },
      getMSpecies () {
        let vm = this
        vm.isMspecies = true
        let oReq = new XMLHttpRequest()
        oReq.onload = function (e) {
          if (this.status === 200) {
            let idx = this.responseText.indexOf('\n')
            let nceHeader = this.responseText.slice(0, idx)
            vm.nceString = 'NCE: ' + nceHeader
            // console.log(nceHeader)
            // console.log(this.responseText.slice(idx + 1))

            let tsv = this.responseText.slice(idx + 1)
            let headerline = tsv.slice(0, tsv.indexOf('\n') + 1).split('\t')
            // console.log(headerline)

            let headers = []
            _.filter(headerline, (o) => o.startsWith('PRI_Group'))
              .forEach((c) => {
                headers.push(c)
              })

            // console.log(headers)

            let groupSize = headers.length

            let data = d3.tsvParse(this.responseText.slice(idx + 1), function (d) {
              if (d.Mspecies !== 'Sum') {
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

            let sum = d3.tsvParse(this.responseText.slice(idx + 1), function (d) {
              if (d.Mspecies === 'Sum') {
                return {
                  fai: vm.averageFAI(d),
                  pri: vm.averagePRI(d),
                  species: d.Species
                }
              }
            })

            let nce = 'NCE ' + nceHeader.replace(/\[|\]/g, '') + ' '

            if (vm.options.indexOf('NoCorrection') > -1) {
              // no correction
              vm.uncorrectedSum = { x: _.map(sum, 'pri'), y: _.map(sum, 'fai'), text: _.map(sum, 'species').map(c => c + '<br>' + nce + 'uncorrected') }
            } else {
              // correction
              vm.correctedSum = { x: _.map(sum, 'pri'), y: _.map(sum, 'fai'), text: _.map(sum, 'species').map(c => c + '<br>' + nce + 'corrected') }
            }

            let yTitle = 'Quantity, microM'
            if (vm.quantOption !== 'Quantity') {
              yTitle = 'Quantity, mol %'
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

            let layout = {
              title: newTitle,
              height: 700,
              margin: {b: 180},
              xaxis: {title: 'Molecular species'},
              yaxis: {title: yTitle},
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
              yaxis: {title: 'Quantity, microM'}
            }

            Plotly.newPlot(vm.$refs.lspecies, data, layout)
          } else {
            console.error(this.responseText)
          }
        }
        oReq.open('GET', serverUrl + '/batch')
        oReq.setRequestHeader('timestamp', vm.timestamp)
        oReq.setRequestHeader('quantOption', vm.quantOption)
        oReq.setRequestHeader('options', vm.options.sort().join('_'))
        oReq.setRequestHeader('outputOption', vm.outputOption)
        oReq.send()
      },
      getValidation () {
        let vm = this
        let oReq = new XMLHttpRequest()
        oReq.onload = function (e) {
          if (this.status === 200) {
            let idx = this.responseText.indexOf('\n')
            let nceHeader = this.responseText.slice(0, idx)

            let sum = d3.tsvParse(this.responseText.slice(idx + 1), function (d) {
              if (d.Mspecies === 'Sum') {
                return {
                  fai: vm.averageFAI(d),
                  pri: vm.averagePRI(d),
                  species: d.Species
                }
              }
            })

            let nce = 'NCE ' + nceHeader.replace(/\[|\]/g, '') + ' '

            if (vm.options.indexOf('NoCorrection') > -1) {
              // no correction
              vm.correctedSum = { x: _.map(sum, 'pri'), y: _.map(sum, 'fai'), text: _.map(sum, 'species').map(c => c + '<br>' + nce + 'corrected') }
            } else {
              // correction
              vm.uncorrectedSum = { x: _.map(sum, 'pri'), y: _.map(sum, 'fai'), text: _.map(sum, 'species').map(c => c + '<br>' + 'NCE ' + nce.substr(nce.replace(/\[|\]/g, '').lastIndexOf(',') + 2) + 'uncorrected') }
            }

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

            console.log(nceHeader)

            let nocorrected = {
              x: vm.uncorrectedSum.x,
              y: vm.uncorrectedSum.y,
              mode: 'markers',
              name: 'NCE ' + nce.substr(nce.replace(/\[|\]/g, '').lastIndexOf(',') + 2) + 'uncorrected',
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

            let regression = require('regression')

            let dat = _.zip(vm.correctedSum.x, vm.correctedSum.y)

            let result = regression.linear(dat)

            // console.log('R2 = ' + result.r2)
            vm.r2 = result.r2
            vm.slope = result.equation[0]
            vm.intercept = result.equation[1]
            // console.log(result)

            let fitted = {
              x: vm.correctedSum.x,
              y: _.zipWith(vm.correctedSum.x, function (a) {
                return result.predict(a)[1]
              }),
              name: 'Fitted line for corrected',
              mode: 'lines',
              text: 'Fitted line for corrected',
              hoverinfo: 'x+y+text',
              line: {
                shape: 'spline',
                color: 'rgb(55, 130, 195)',
                width: 3
              }
            }

            let layout = {
              // title: 'Validation Figure NCE:' + nceHeader,
              title: 'Validation Plot',
              xaxis: {title: 'Quantity, microM'},
              yaxis: {title: 'Quantity, microM'}
            }

            Plotly.newPlot(vm.$refs.validation, [corrected, nocorrected, fitted], layout)

          //            console.log(data)
          } else {
            console.error(this.responseText)
          }
          vm.isLspecies = false
          vm.isMspecies = true
          vm.isValidation = false
          vm.processing = false
        }
        oReq.open('GET', serverUrl + '/batch')
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

        vm.$http.get(serverUrl + '/download', { responseType: 'arraybuffer', headers: reqHeaders })
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
      },
      openTab (tab) {
        let vm = this
        console.log(tab)
        switch (tab) {
          case 'LSpecies':
            vm.isLspecies = true
            vm.isMspecies = false
            vm.isValidation = false
            break
          case 'MSpecies':
            vm.isLspecies = false
            vm.isMspecies = true
            vm.isValidation = false
            break
          case 'Validation':
            vm.isLspecies = false
            vm.isMspecies = false
            vm.isValidation = true
            break
        }
      }
    },
    props: ['batch_title'],
    mounted: function () {
      let vm = this
      // console.log(vm.batch_title)
      let oReq = new XMLHttpRequest()
      oReq.onload = function (e) {
        if (this.status === 200) {
          // console.info(this.responseText)
          let list = JSON.parse(this.responseText)
          vm.batchlist = []

          Object.keys(list).map(function (key, index) {
            if (key === vm.timestamp) {
              vm.batchlist.push({text: `${list[key]}`, value: key})
              vm.quantify()
            }
          })

          // console.log(vm.batchlist)

          // let found = _.find(vm.batchlist, { 'value': vm.timestamp })
          // if (found) {
          //   vm.quantify()
          // }

          // let found = _.find(vm.batchlist, { 'text': vm.batch_title })
          // if (found) {
          //   console.log(found)
          //   vm.timestamp = found.value
          //   vm.quantify()
          // }
        } else {
          console.error(this.responseText)
        }
      }
      oReq.open('GET', serverUrl + '/list')
      oReq.send()
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
