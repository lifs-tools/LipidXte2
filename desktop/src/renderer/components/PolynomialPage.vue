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
      <li role="presentation" class="active">
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

      <label for="class-id">Class:</label>

      <select id="class-id" class="lipid-class" v-model="lipidClass">
        <option disabled value="">Please select one in the lipid classes</option>
        <option v-for="lipidC in lipidClassList" v-bind:value="lipidC">
          {{ lipidC }}
        </option>
      </select>

      <label for="sn1-id">SN1:</label>

      <select id="sn1-id" class="lipid-class" v-model="lipidSn1">
        <option disabled value="">Please select one in the fractions</option>
        <option v-for="fraction in (lipidClass.startsWith('PCO') || lipidClass.startsWith('PEO')) ? oFractionList : fractionList" v-bind:value="fraction">
          {{ fraction.name }}
        </option>
      </select>

      <label for="sn2-id">SN2:</label>

      <select id="sn2-id" class="lipid-class" v-model="lipidSn2">
        <option disabled value="">Please select one in the fractions</option>
        <option v-for="fraction in fractionList" v-bind:value="fraction">
          {{ fraction.name }}
        </option>
      </select>
      &nbsp;&nbsp;
      <button type="button" class="btn-lipid" @click="showPlots">Add & show</button>
      <button type="button" class="btn-lipid" @click="clear">Clear</button>
      <button type="button" class="btn-lipid" @click="download">Download</button>
      <a ref="tsv"></a>
    </div>

    <div>
      <label>Selected analytes:</label>&nbsp;<span>{{analytes.join(', ')}}</span>
    </div>

    <table>
      <tr>
        <td>

        </td>
        <td>

        </td>
        <!--<td>-->
        <!--<button type="button" class="btn btn-danger" @click="quantify">Test</button>-->
        <!--</td>-->
        <a ref="tsv"></a>
      </tr>
      <tr>
        <td></td>
        <td></td>
      </tr>
    </table>

    <br/>

    <table>
      <tr>
        <td>
          <div ref="plots" style="width: 680px;"></div>
        </td>
        <td>
          <div ref="mzplot" style="width: 680px;"></div>
        </td>
      </tr>
      <tr>
        <td>
          <div id="nceSliderContainer">
            <input v-show="plotData.length > 0" type="range" min="10" max="70" value="10" class="slider" v-model="nceValue" style="width: 450px;" id="nceSlider">
          </div>
        </td>
        <td>
          &nbsp;
        </td>
      </tr>
    </table>
  </div>
</template>

<script>
import _ from 'lodash'
import {serverUrl} from './conf'
import Plotly from './box'

export default {
  name: 'PolynomialPage',
  data: function () {
    return {
      lipidClassList: [],
      lipidClassPCOExtList: ['PCO-FANL', 'PCO-M-60', 'PCO-PR'],
      lipidClass: '',
      fractionList: [],
      oFractionList: [{id: 7, mz: 255.23, name: '16:0'},
        {id: 8, mz: 253.22, name: '16:1'}, {id: 13, mz: 281.25, name: '18:1'}],
      lipidSn1: '',
      lipidSn2: '',
      plotData: [],
      analytes: [],
      fragmentsSn1: [],
      fragmentsSn2: [],
      experiments: [],
      nceValue: 10,
      mzPlot: undefined
    }
  },
  computed: {
  },
  watch: {
    lipidClass (newVal, oldVal) {
      const vm = this
      if (newVal.startsWith('PCO') || newVal.startsWith('PEO')) {
        vm.lipidSn1 = vm.oFractionList[0]
      }
    },
    nceValue (newVal) {
      const vm = this
      vm.updateNCEPlot(newVal)
    }
  },
  methods: {
    getPlotData (nce) {
      const vm = this
      let data = []

      vm.plotData.forEach(d => {
        data.push({
          x: [d.mz, d.mz],
          y: [0, d.y[nce - 10]],
          mode: 'lines+markers',
          text: d.name,
          name: d.name,
          showlegend: false,
          marker: {symbol: d.name.endsWith('-exp') ? 'x' : 'circle', size: [null, 13]},
          line: {width: 5},
          text_auto: true
        })
      })

      return data
    },
    getMzList () {
      const vm = this
      return vm.plotData.map(c => c.mz)
    },
    async updateNCEPlot (nce) {
      const vm = this
      let mzList = vm.getMzList()

      let x = nce

      let layout = {
        title: `Combined HCD FTMS2- spectra at NCE = ${x} %`,
        height: 700,
        margin: {b: 180},
        uirevision: 'same',
        xaxis: {title: 'm/z', range: [_.min(mzList) - 10, _.max(mzList) + 10]},
        yaxis: {title: 'Relative Intensity, %'}
      }

      const data = vm.getPlotData(nce)

      if (vm.mzPlot) {
        // console.log(vm.mzPlot.data.length)
        if (_.isEqual(data.map(c => c.name), vm.mzPlot.data.map(c => c.name))) {
          const indices = data.map((c, i) => i)
          Plotly.restyle(vm.$refs.mzplot, 'x', data.map(c => c.x), indices)
          Plotly.restyle(vm.$refs.mzplot, 'y', data.map(c => c.y), indices)
          Plotly.update(vm.$refs.mzplot, null, {title: layout.title, xaxis: layout.xaxis, height: layout.height, margin: layout.margin})
        } else {
          vm.mzPlot = await Plotly.newPlot(vm.$refs.mzplot, data, {title: layout.title, xaxis: layout.xaxis, height: layout.height, margin: layout.margin})
        }
      } else {
        vm.mzPlot = await Plotly.newPlot(vm.$refs.mzplot, data, layout)
      }
    },
    async showPlots () {
      let vm = this

      if (vm.lipidClass === '' || vm.lipidSn2 === '') {
        console.error('error')
        return
      }

      // console.log(vm.lipidSn1)
      // console.log(vm.lipidSn2)

      const mzList = []

      if (!(vm.lipidClass.startsWith('PCO') || vm.lipidClass.startsWith('PEO')) && vm.lipidSn1 === vm.lipidSn2) {
        // Symmetric
        const url = `${serverUrl}/sym/${vm.lipidClass}/${vm.lipidSn1.id}`
        await this.$http.get(url, {
          mode: 'cors',
          cache: 'no-cache',
          headers: {
            'Content-Type': 'application/json'
          }
        })
          .then(res => {
            // console.log(res.data)
            // Draw a plot
            vm.plotData.push({
              x: _.map(res.data, 'CE'),
              y: _.map(res.data, 'INT'),
              // fill: 'tonexty',
              mode: 'lines+markers',
              // type: 'area',
              text: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '/' + vm.lipidSn2.name + '</b>',
              name: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '/' + vm.lipidSn2.name + '</b>',
              mz: vm.lipidSn1.mz
            })

            mzList.push(vm.lipidSn1.mz)

            if (res.data[0].CO2INT) {
              vm.plotData.push({
                x: _.map(res.data, 'CE'),
                y: _.map(res.data, 'CO2INT'),
                // fill: 'tonexty',
                mode: 'lines+markers',
                // type: 'area',
                text: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '-CO2/' + vm.lipidSn2.name + '-CO2</b>',
                name: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '-CO2/' + vm.lipidSn2.name + '-CO2</b>',
                mz: vm.lipidSn2.mz - 43.99
              })
            }
          })
          .catch(e => {
            this.error = true
            console.error(e)
          })
      } else {
        if (!(vm.lipidClass.startsWith('PCO') || vm.lipidClass.startsWith('PEO'))) {
          // vm.fragmentsSn1.push(vm.lipidClass + ' ' + vm.lipidSn1.name)

          const url = `${serverUrl}/sn1/${vm.lipidClass}/${vm.lipidSn1.id}`
          await this.$http.get(url, {
            mode: 'cors',
            cache: 'no-cache',
            headers: {
              'Content-Type': 'application/json'
            }
          })
            .then(res => {
              // console.log(res.data)
              // Draw a plot
              vm.plotData.push({
                x: _.map(res.data, 'CE'),
                y: _.map(res.data, 'INT'),
                // fill: 'tonexty',
                mode: 'lines+markers',
                // type: 'area',
                text: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '</b>/' + vm.lipidSn2.name,
                name: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '</b>/' + vm.lipidSn2.name,
                mz: vm.lipidSn1.mz
              })

              mzList.push(vm.lipidSn1.mz)

              if (res.data[0].CO2INT) {
                vm.plotData.push({
                  x: _.map(res.data, 'CE'),
                  y: _.map(res.data, 'CO2INT'),
                  // fill: 'tonexty',
                  mode: 'lines+markers',
                  // type: 'area',
                  text: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '-CO2</b>/' + vm.lipidSn2.name,
                  name: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '-CO2</b>/' + vm.lipidSn2.name,
                  mz: vm.lipidSn2.mz - 43.99
                })
              }
            })
            .catch(e => {
              this.error = true
              console.error(e)
            })
        }

        // if (vm.fragmentsSn2.indexOf(vm.lipidClass + ' ' + vm.lipidSn2.name) < 0)
        {
          // vm.fragmentsSn2.push(vm.lipidClass + ' ' + vm.lipidSn2.name)

          const url = `${serverUrl}/sn2/${vm.lipidClass}/${vm.lipidSn2.id}`
          const res = await this.$http.get(url, {
            mode: 'cors',
            cache: 'no-cache',
            headers: {
              'Content-Type': 'application/json'
            }
          })
          // console.log(res.data)

          // Draw a plot
          vm.plotData.push({
            x: _.map(res.data, 'CE'),
            y: _.map(res.data, 'INT'),
            // fill: 'tonexty',
            mode: 'lines+markers',
            // type: 'area',
            text: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + vm.lipidSn2.name + '</b>',
            name: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + vm.lipidSn2.name + '</b>',
            mz: vm.lipidSn2.mz
          })
          // console.log(vm.lipidSn2.mz)
          mzList.push(vm.lipidSn2.mz)

          if (res.data[0].CO2INT) {
            vm.plotData.push({
              x: _.map(res.data, 'CE'),
              y: _.map(res.data, 'CO2INT'),
              // fill: 'tonexty',
              mode: 'lines+markers',
              // type: 'area',
              text: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + vm.lipidSn2.name + '-CO2</b>',
              name: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + vm.lipidSn2.name + '-CO2</b>',
              mz: vm.lipidSn2.mz - 43.99
            })
          }

          if (vm.lipidClass === 'PCO') {
            const results = await Promise.all(vm.lipidClassPCOExtList.map(c => {
              const url = `${serverUrl}/sn2/${c}/${vm.lipidSn2.id}`
              return this.$http.get(url, {
                mode: 'cors',
                cache: 'no-cache',
                headers: {
                  'Content-Type': 'application/json'
                }
              })
            }))

            results.forEach((res, i) => {
              const c = vm.lipidClassPCOExtList[i]

              let mz = vm.lipidSn2.mz
              if (c === 'PCO-FANL') {
                mz = Number((449.33 + 17).toFixed(2))
              } else if (c === 'PCO-M-60') {
                mz = Number((449.33 + vm.lipidSn2.mz).toFixed(2))
              } else if (c === 'PCO-PR') {
                mz = Number((449.33 + 60.02 + vm.lipidSn2.mz).toFixed(2))
              }
              // console.log(c + ':' + mz)

              mzList.push(mz)

              // Draw a plot
              vm.plotData.push({
                x: _.map(res.data, 'CE'),
                y: _.map(res.data, 'INT'),
                // fill: 'tonexty',
                mode: 'lines+markers',
                // type: 'area',
                text: c === 'PCO-FANL' ? c + ' ' + vm.lipidSn1.name + '/<b>' + vm.lipidSn2.name + '</b>' : c + ' ' + vm.lipidSn1.name + '/' + vm.lipidSn2.name,
                name: c === 'PCO-FANL' ? c + ' ' + vm.lipidSn1.name + '/<b>' + vm.lipidSn2.name + '</b>' : c + ' ' + vm.lipidSn1.name + '/' + vm.lipidSn2.name,
                mz: mz
              })
            })
          }
        }
      }

      if (vm.analytes.indexOf(vm.lipidClass + ' ' + vm.lipidSn1.name + '/' + vm.lipidSn2.name) < 0) {
        vm.analytes.push(vm.lipidClass + ' ' + vm.lipidSn1.name + '/' + vm.lipidSn2.name)

        // Fetch experiment data
        const url = `${serverUrl}/json/${vm.lipidClass}.json`
        await this.$http.get(url, {
          mode: 'cors',
          cache: 'no-cache',
          headers: {
            'Content-Type': 'application/json'
          }
        })
          .then(res => {
            // console.log(res.data)
            // Check DMPE for PE and PEO fragments and PCO commercial and synthesized here
            // const specie = res.data[vm.lipidClass + ' ' + vm.lipidSn1.name + '/' + vm.lipidSn2.name]

            // const species = _.filter()
            let speciesKeys = Object.keys(res.data).filter((key) => key.includes(`${vm.lipidSn1.name.substring(0, 4)}/${vm.lipidSn2.name.substring(0, 4)}`))

            if (vm.lipidClass === 'PCO' && speciesKeys.length > 2) {
              if (vm.lipidSn2.name.indexOf('5z') > -1) {
                speciesKeys.splice(2, 1)
              } else {
                speciesKeys.splice(0, 2)
              }
            }
            // console.log(speciesKeys)

            speciesKeys.forEach(key => {
              const specie = res.data[key]

              if (specie) {
                // console.log(specie)

                if (!(vm.lipidClass.startsWith('PCO') || vm.lipidClass.startsWith('PEO')) && vm.lipidSn1 === vm.lipidSn2) {
                  let exp = specie[vm.lipidSn1.mz]

                  if (exp) {
                    // console.log(exp)

                    // // Draw a plot
                    vm.plotData.push({
                      x: exp.nce,
                      y: exp.int,
                      mode: 'lines+markers',
                      marker: {symbol: 'x'},
                      text: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp</b>',
                      name: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp</b>',
                      mz: vm.lipidSn1.mz
                    })
                  } else {
                    const exp = specie[(vm.lipidSn1.mz + 0.01) + '']

                    if (exp) {
                      // // Draw a plot
                      vm.plotData.push({
                        x: exp.nce,
                        y: exp.int,
                        mode: 'lines+markers',
                        marker: {symbol: 'x'},
                        text: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp</b>',
                        name: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp</b>',
                        mz: vm.lipidSn1.mz
                      })
                    }
                  }
                } else if (!(vm.lipidClass.startsWith('PCO') || vm.lipidClass.startsWith('PEO'))) {
                  let exp = specie[vm.lipidSn1.mz]

                  if (exp) {
                    // console.log(exp)

                    // // Draw a plot
                    vm.plotData.push({
                      x: exp.nce,
                      y: exp.int,
                      mode: 'lines+markers',
                      marker: {symbol: 'x'},
                      text: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '</b>/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp',
                      name: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '</b>/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp',
                      mz: vm.lipidSn1.mz
                    })
                  } else {
                    const exp = specie[(vm.lipidSn1.mz + 0.01) + '']

                    if (exp) {
                      // // Draw a plot
                      vm.plotData.push({
                        x: exp.nce,
                        y: exp.int,
                        mode: 'lines+markers',
                        marker: {symbol: 'x'},
                        text: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '</b>/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp',
                        name: vm.lipidClass + ' <b>' + vm.lipidSn1.name + '</b>/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp',
                        mz: vm.lipidSn1.mz
                      })
                    }
                  }
                }

                if (vm.lipidSn1 !== vm.lipidSn2) {
                  let exp = specie[vm.lipidSn2.mz]

                  if (exp) {
                    // console.log(exp)

                    // // Draw a plot
                    vm.plotData.push({
                      x: exp.nce,
                      y: exp.int,
                      mode: 'lines+markers',
                      marker: {symbol: 'x'},
                      text: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '</b>-exp',
                      name: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '</b>-exp',
                      mz: vm.lipidSn2.mz
                    })
                  } else {
                    const exp = specie[(vm.lipidSn2.mz + 0.01) + '']

                    if (exp) {
                      // // Draw a plot
                      vm.plotData.push({
                        x: exp.nce,
                        y: exp.int,
                        mode: 'lines+markers',
                        marker: {symbol: 'x'},
                        text: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '</b>-exp',
                        name: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '</b>-exp',
                        mz: vm.lipidSn2.mz
                      })
                    }
                  }

                  // CO2loss
                  let mz = vm.lipidSn2.mz - 43.99
                  exp = specie[mz]

                  if (exp) {
                    // console.log(exp)

                    // // Draw a plot
                    vm.plotData.push({
                      x: exp.nce,
                      y: exp.int,
                      mode: 'lines+markers',
                      marker: {symbol: 'x'},
                      text: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-CO2</b>-exp',
                      name: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-CO2</b>-exp',
                      mz: mz
                    })
                  } else {
                    const exp = specie[(mz + 0.01) + '']

                    if (exp) {
                      // // Draw a plot
                      vm.plotData.push({
                        x: exp.nce,
                        y: exp.int,
                        mode: 'lines+markers',
                        marker: {symbol: 'x'},
                        text: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-CO2</b>-exp',
                        name: vm.lipidClass + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-CO2</b>-exp',
                        mz: mz + 0.01
                      })
                    }
                  }

                  // console.log(vm.lipidSn2.mz)
                  // console.log(449.33 + vm.lipidSn2.mz)

                  if (vm.lipidClass === 'PCO') {
                    vm.lipidClassPCOExtList.forEach((c) => {
                      let mz = vm.lipidSn2.mz
                      if (c === 'PCO-FANL') {
                        mz = Number((449.33 + 17).toFixed(2))
                      } else if (c === 'PCO-M-60') {
                        mz = Number((449.33 + vm.lipidSn2.mz).toFixed(2))
                      } else if (c === 'PCO-PR') {
                        mz = Number((449.33 + 60.02 + vm.lipidSn2.mz).toFixed(2))
                      }

                      // console.log(c + ':' + mz)
                      let exp = specie[mz + '']

                      if (exp) {
                        // Draw a plot
                        vm.plotData.push({
                          x: exp.nce,
                          y: exp.int,
                          mode: 'lines+markers',
                          marker: {symbol: 'x'},
                          text: c === 'PCO-FANL' ? c + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '</b>-exp' : c + ' ' + vm.lipidSn1.name + '/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp',
                          name: c === 'PCO-FANL' ? c + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '</b>-exp' : c + ' ' + vm.lipidSn1.name + '/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp',
                          mz: mz
                        })
                      } else {
                        exp = specie[(mz - 0.01) + '']
                        vm.plotData.push({
                          x: exp.nce,
                          y: exp.int,
                          mode: 'lines+markers',
                          marker: {symbol: 'x'},
                          text: c === 'PCO-FANL' ? c + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '</b>-exp' : c + ' ' + vm.lipidSn1.name + '/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp',
                          name: c === 'PCO-FANL' ? c + ' ' + vm.lipidSn1.name + '/<b>' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '</b>-exp' : c + ' ' + vm.lipidSn1.name + '/' + key.replace(vm.lipidClass + ' ' + vm.lipidSn1.name + '/', '') + '-exp',
                          mz: mz
                        })
                      }
                    })
                  }
                }
              }
            })
          })
          .catch(e => {
            this.error = true
            console.error(e)
          })
      }

      let layout = {
        title: 'Fragment intensity curves',
        height: 700,
        margin: {b: 180, r: 80},
        xaxis: {title: 'Normalized Collision Energy, %'},
        yaxis: {title: 'Relative Intensity, %'}
      }

      Plotly.newPlot(vm.$refs.plots, vm.plotData, layout)

      if (vm.mzPlot) {
        const data = vm.getPlotData(vm.nceValue)
        if (!_.isEqual(data.map(c => c.name), vm.mzPlot.data.map(c => c.name))) {
          const mzList = vm.getMzList()

          let layout = {
            title: `Combined HCD FTMS2- spectra at NCE = ${vm.nceValue} %`,
            height: 700,
            margin: {b: 180},
            uirevision: 'same',
            xaxis: {title: 'm/z', range: [_.min(mzList) - 10, _.max(mzList) + 10]},
            yaxis: {title: 'Relative Intensity, %'}
          }

          vm.mzPlot = await Plotly.newPlot(vm.$refs.mzplot, data, layout)
        }
      }

      vm.$refs.plots.on('plotly_click', async function (e) {
        // console.log(e)
        let x = e.points[0].x

        vm.nceValue = x
      })

      vm.$refs.plots.on('plotly_restyle', async function (e) {
        // console.log(e)
        if (vm.mzPlot) {
          const data = vm.getPlotData(10)

          if (!_.isEqual(data.map(c => c.name), vm.mzPlot.data.map(c => c.name))) {
            const mzList = vm.getMzList()

            let layout = {
              title: `Combined HCD FTMS2- spectra at NCE = ${10} %`,
              height: 700,
              margin: {b: 180},
              uirevision: 'same',
              xaxis: {title: 'm/z', range: [_.min(mzList) - 10, _.max(mzList) + 10]},
              yaxis: {title: 'Relative Intensity, %'}
            }

            vm.mzPlot = await Plotly.newPlot(vm.$refs.mzplot, data, layout)
          }

          Plotly.restyle(vm.$refs.mzplot, e[0], e[1])
        }
      })
    },
    clear () {
      const vm = this
      vm.analytes = []
      vm.fragmentsSn1 = []
      vm.fragmentsSn2 = []
      vm.plotData = []
      let layout = {
        title: 'Fragment intensity curves',
        height: 700,
        margin: {b: 180, r: 80},
        xaxis: {title: 'Normalized Collision Energy, %'},
        yaxis: {title: 'Relative Intensity, %'}
      }

      Plotly.newPlot(vm.$refs.plots, vm.plotData, layout)

      let data = []

      layout = {
        title: 'Combined HCD FTMS2- spectra at NCE =',
        height: 700,
        margin: {b: 180},
        xaxis: {title: 'm/z'},
        yaxis: {title: 'Relative Intensity, %'}
      }

      Plotly.newPlot(vm.$refs.mzplot, data, layout)
    },
    download () {
      const vm = this
      // console.log(vm.analytes)
      const output = []
      _.forEach(vm.analytes, analyte => {
        _.range(10, 71).forEach(nce => {
          // console.log(nce)
          const fragNames = []
          const mzList = []
          const intList = []
          const expMzList = []
          const expInt = []
          _.forEach(vm.plotData, item => {
            const name = item.name.replace('<b>', '').replace('</b>', '')
            if (name.indexOf(analyte) > -1) {
              // console.log(name)
              // console.log(item)
              const found = item.name.match(/<b>(.*)<\/b>/gm)
              const fragName = found[0].replace('<b>', '').replace('</b>', '')

              if (name.endsWith('-exp')) {
                fragNames.push(fragName + '-exp')
                expMzList.push(item.mz)
                expInt.push(item.y[nce - 10])
              } else {
                fragNames.push(fragName)
                mzList.push(item.mz)
                intList.push(item.y[nce - 10])
              }
            }
          })

          output.push([analyte, nce, fragNames, mzList, intList, expMzList, expInt])
        })
      })
      // console.log(output)

      let blob = new Blob([JSON.stringify(output)], {type: 'application/json'})

      let link = vm.$refs.tsv
      link.href = window.URL.createObjectURL(blob)
      link.download = 'output.json'
      link.click()
    }
  },
  mounted: function () {
    let vm = this
    // console.log(vm.batch_title)
    let oReq = new XMLHttpRequest()
    oReq.onload = function (e) {
      if (this.status === 200) {
        // console.info(this.responseText)
        let list = JSON.parse(this.responseText)

        vm.lipidClassList = _.map(_.filter(list, i => i.length < 4), c => c)
      } else {
        // console.error(this.responseText)
      }
    }
    oReq.open('GET', serverUrl + '/classes')
    oReq.send()

    oReq = new XMLHttpRequest()
    oReq.onload = function (e) {
      if (this.status === 200) {
        // console.info(this.responseText)
        let list = JSON.parse(this.responseText)

        vm.fractionList = _.map(list, c => c)
        // console.log(vm.fractionList)
      } else {
        // console.error(this.responseText)
      }
    }
    oReq.open('GET', serverUrl + '/fractions')
    oReq.send()
  }
}
</script>

<style scoped>

button {
  color: unset;
  background-color: unset;
}

.btn-lipid:hover {
  background-color: #339999;
  color: white;
  transition: 0.2s;
}

#nceSliderContainer {
  position: relative;
  left: 75px;
  top: -120px;
}
</style>
