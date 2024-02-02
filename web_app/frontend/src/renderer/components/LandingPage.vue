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
      <li role="presentation" class="active">
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
        <router-link to="/sample/ILIS experiment">
          Sample
        </router-link>
      </li>
      <li role="presentation">
        <router-link to="/poly">
          MS2 spectra calculator
        </router-link>
      </li>
      <li role="presentation">
        <router-link to="/help">
          Help
        </router-link>
      </li>
    </ul>

    <br/>

    <main>
      <div class="left-side">
        <div class="doc">
          <div class="title"><i class="fa fa-check" aria-hidden="true"/>Getting Started</div>
          <p>
            Please, upload your RAW files or mzXML files and the standard reference file in csv format, the file name should be <code>standard_list.csv</code>. Please, check the <A href="https://lipidxte.mpi-cbg.de/standard_list.csv">sample file</A>.<br/>
            Then, click the upload button after adding all the necessary files by doing drag-and-drop in <i class="fa fa-paste" aria-hidden="true"/> the dropzone.
          </p>
        </div>
        <div class="doc">
          <div class="doc">
            <!--<button @click="test">Test</button>-->
            <button @click="upload">Upload</button><br/>
            <label for="class-name">Class:</label>
            <select id="class-name" v-model="className">
              <option disabled value="">Please select one</option>
              <option selected value="PC">PC</option>
              <option value="PE">PE</option>
            </select>
          </div>
          <label for="title">Title:</label>&nbsp;
          <input id="title" type="text" v-model="title" placeholder="enter a title"/><br/>
          <!--<label for="input-tag">Tags:</label>-->
          <!--<input-tag id="input-tag" :on-change="callbackTags" :tags="sampleTags" placeholder="enter tags"></input-tag>-->
          <br/>
          <label for="dropzone"><i class="fa fa-paste" aria-hidden="true"/> Group-1: RAW files & <code>standard_list.csv</code>:</label><br/>
          <vue-dropzone id="dropzone" ref="myVueDropzone" :options="dropzoneOptions">
          </vue-dropzone>
          <label for="dropzone2"><i class="fa fa-paste" aria-hidden="true"/> Group-2: RAW files :</label><br/>
          <vue-dropzone id="dropzone2" ref="myVueDropzone2" :options="dropzoneGroup2Options">
          </vue-dropzone>
          <label for="dropzone3"><i class="fa fa-paste" aria-hidden="true"/> Group-3: RAW files :</label><br/>
          <vue-dropzone id="dropzone3" ref="myVueDropzone3" :options="dropzoneGroup3Options">
          </vue-dropzone>
        </div>
      </div>

      <div class="right-side">
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

    <!--<button id="show-modal" @click="showModal = true">Show Modal</button>-->
    <modal v-if="showModal" @close="showModal = false">
      <!--
        you can use custom content here to overwrite
        default content
      -->
      <h3 slot="header"><i class="fa fa-gear fa-spin" aria-hidden="true"/> LipidXte Web</h3>
      <h4 slot="body">Processing your data ...</h4>
      <p slot="footer">This window will be closed automatically soon.</p>
    </modal>

    <modal v-if="alert" @close="alert = false">
      <!--
        you can use custom content here to overwrite
        default content
      -->
      <h3 slot="header" style="color: #e73c57"><i class="fa fa-exclamation-triangle" aria-hidden="true"/> Error</h3>
      <h4 slot="body">{{alertMessage}}</h4>
    </modal>
  </div>
</template>

<script>
  import 'font-awesome/css/font-awesome.css'
  import modal from './ModalWindow'
  import vue2Dropzone from 'vue2-dropzone'
  import InputTag from 'vue-input-tag'
  import { serverUrl } from './conf'

  export default {
    name: 'landing-page',
    components: { vueDropzone: vue2Dropzone, inputTag: InputTag, modal },
    methods: {
      open (link) {
        this.$electron.shell.openExternal(link)
      },
      upload () {
        this.uploadClicked = true

        // Check the empty fields
        if (this.title === '') {
          this.alertMessage = 'Title cannot be empty. Please, enter your experiment title.'
          this.alert = true
          this.uploadClicked = false
          console.error(this.alertMessage)
          return
        }

        if (this.$refs.myVueDropzone.getQueuedFiles().length === 0 &&
          this.$refs.myVueDropzone2.getQueuedFiles().length === 0 &&
          this.$refs.myVueDropzone3.getQueuedFiles().length === 0) {
          this.alertMessage = 'Uploading files are necessary. Please, drag and drop the RAW files.'
          this.alert = true
          this.uploadClicked = false
          console.error(this.alertMessage)
          return
        }

        // Adding uploading file size check
        let totalSize = 0
        if (this.$refs.myVueDropzone.getQueuedFiles().length > 0) {
          console.log(this.$refs.myVueDropzone.getQueuedFiles())
          this.$refs.myVueDropzone.getQueuedFiles().forEach(c => {
            totalSize += c.size
          })
        }

        if (this.$refs.myVueDropzone2.getQueuedFiles().length > 0) {
          console.log(this.$refs.myVueDropzone2.getQueuedFiles())
          this.$refs.myVueDropzone2.getQueuedFiles().forEach(c => {
            totalSize += c.size
          })
        }

        if (this.$refs.myVueDropzone3.getQueuedFiles().length > 0) {
          console.log(this.$refs.myVueDropzone3.getQueuedFiles())
          this.$refs.myVueDropzone3.getQueuedFiles().forEach(c => {
            totalSize += c.size
          })
        }

        if (totalSize > 100000000) {
          this.alertMessage = 'Uploading files are too big. Please, remove some files.'
          this.alert = true
          return
        }

        this.timestamp = new Date().toISOString()

        this.$refs.myVueDropzone3.options.headers.timestamp = this.timestamp
        let files = this.$refs.myVueDropzone3.getQueuedFiles()
        files.forEach(c => {
          if (!c.name.endsWith('.csv')) {
            this.group3.push(c.name.replace(/\.raw/ig, '.mzXML'))
          }
        })
        this.$refs.myVueDropzone3.options.headers.files = files
        // this.$refs.myVueDropzone3.processQueue()

        this.$refs.myVueDropzone2.options.headers.timestamp = this.timestamp
        files = this.$refs.myVueDropzone2.getQueuedFiles()
        files.forEach(c => {
          if (!c.name.endsWith('.csv')) {
            this.group2.push(c.name.replace(/\.raw/ig, '.mzXML'))
          }
        })
        this.$refs.myVueDropzone2.options.headers.files = files

        this.$refs.myVueDropzone.options.headers.timestamp = this.timestamp
        files = this.$refs.myVueDropzone.getQueuedFiles()
        files.forEach(c => {
          if (!c.name.endsWith('.csv')) {
            this.group1.push(c.name.replace(/\.raw/ig, '.mzXML'))
          }
        })
        this.$refs.myVueDropzone.options.headers.files = files

        if (this.$refs.myVueDropzone3.options.headers.files.length === 0) {
          if (this.$refs.myVueDropzone2.options.headers.files.length === 0) {
            this.$refs.myVueDropzone.processQueue()
          } else {
            this.$refs.myVueDropzone2.processQueue()
          }
        } else {
          this.$refs.myVueDropzone3.processQueue()
        }
      },
      gotoSamplePage () {
        if (this.timestamp === '') {
          this.alertMessage = 'The batch is not selected. Please, choose one in the batch list.'
          this.alert = true
          return
        }

        let params = {
          name: 'sample-page',
          query: {
            timestamp: this.timestamp
          }
        }

        this.$router.push(params)
      },
      callbackTags (item) {
        console.log(item)
      },
      test () {
        this.timestamp = new Date().toISOString()

        this.$refs.myVueDropzone3.options.headers.timestamp = this.timestamp
        let files = this.$refs.myVueDropzone3.getQueuedFiles()
        files.forEach(c => {
          if (!c.name.endsWith('.csv')) {
            this.group3.push(c.name)
          }
        })
        this.$refs.myVueDropzone3.options.headers.files = files

        this.$refs.myVueDropzone2.options.headers.timestamp = this.timestamp
        files = this.$refs.myVueDropzone2.getQueuedFiles()
        files.forEach(c => {
          if (!c.name.endsWith('.csv')) {
            this.group2.push(c.name)
          }
        })
        this.$refs.myVueDropzone2.options.headers.files = files

        this.$refs.myVueDropzone.options.headers.timestamp = this.timestamp
        files = this.$refs.myVueDropzone.getQueuedFiles()
        files.forEach(c => {
          if (!c.name.endsWith('.csv')) {
            this.group1.push(c.name)
          }
        })
        this.$refs.myVueDropzone.options.headers.files = files
        console.info(files)
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
      },
      dbTest () {
        let vm = this
        vm.timestamp = new Date().toISOString()
        var oReq = new XMLHttpRequest()
        oReq.onload = function (e) {
          if (this.status === 200) {
            console.info(this.responseText)
          } else {
            console.error(this.responseText)
          }
        }
        oReq.open('GET', vm.url + '/test')
        oReq.setRequestHeader('timestamp', vm.timestamp)
        oReq.setRequestHeader('tags', vm.sampleTags.join(','))
        oReq.send()
      }
    },
    data: function () {
      let vm = this
      return {
        showModal: false,
        alert: false,
        uploadClicked: false,
        alertMessage: '',
        timestamp: '',
        className: 'PC',
        title: '',
        sampleTags: [],
        options: [],
        group1: [],
        group2: [],
        group3: [],
        dropzoneOptions: {
          init: function () {
            this.on('complete', function (file) {
              this.removeFile(file)
            })
            this.on('queuecomplete', function () {
              if (vm.uploadClicked) {
                console.info('queuecomplete')
                let oReq = new XMLHttpRequest()
                vm.showModal = true
                oReq.onload = function (e) {
                  if (this.status === 200) {
                    console.log(this.responseText)
                    vm.showModal = false
                    vm.gotoSamplePage()
                    vm.uploadClicked = false
                  } else if (this.status === 500) {
                    console.error(this.responseText)
                    vm.showModal = false
                    if (this.responseText) {
                      window.alert(this.responseText)
                    } else {
                      window.alert('There are errors in the input files. Please, contact to authors.')
                    }
                    vm.uploadClicked = false
                  } else {
                    console.error(this.responseText)
                    vm.showModal = false
                    vm.uploadClicked = false
                  }
                }
                oReq.open('GET', serverUrl + '/process')
                oReq.setRequestHeader('timestamp', vm.timestamp)
                oReq.setRequestHeader('title', vm.title)
                oReq.setRequestHeader('tags', vm.sampleTags.join(','))
                oReq.setRequestHeader('className', vm.className)
                oReq.setRequestHeader('group1', vm.group1.join(','))
                oReq.setRequestHeader('group2', vm.group2.join(','))
                oReq.setRequestHeader('group3', vm.group3.join(','))
                oReq.send()
              }
            })
          },
          url: serverUrl + '/upload',
          maxFilesize: 500,
          acceptedFiles: '.csv,.raw',
          createImageThumbnails: false,
          autoProcessQueue: false,
          addRemoveLinks: true,
          paramName: true,
          headers: {'LipidXTe-Header': 'version 1.0',
            'timestamp': new Date().toISOString()}
        },
        dropzoneGroup2Options: {
          init: function () {
            this.on('complete', function (file) {
              this.removeFile(file)
              vm.$refs.myVueDropzone.processQueue()
            })
          },
          url: serverUrl + '/upload',
          maxFilesize: 500,
          acceptedFiles: '.csv,.raw',
          createImageThumbnails: false,
          autoProcessQueue: false,
          addRemoveLinks: true,
          paramName: true,
          headers: {'LipidXTe-Header': 'version 1.0',
            'timestamp': new Date().toISOString()}
        },
        dropzoneGroup3Options: {
          init: function () {
            this.on('complete', function (file) {
              this.removeFile(file)
              vm.$refs.myVueDropzone2.processQueue()
            })
          },
          url: serverUrl + '/upload',
          maxFilesize: 500,
          acceptedFiles: '.csv,.raw',
          createImageThumbnails: false,
          autoProcessQueue: false,
          addRemoveLinks: true,
          paramName: true,
          headers: {'LipidXTe-Header': 'version 1.0',
            'timestamp': new Date().toISOString()}
        }
      }
    },
    mounted: function () {
      let vm = this
      localStorage.removeItem('user-password')

      let oReq = new XMLHttpRequest()
      oReq.onload = function (e) {
        if (this.status === 200) {
          // console.info(this.responseText)
          let list = JSON.parse(this.responseText)
          vm.options = []

          Object.keys(list).map(function (key, index) {
            vm.options.push({text: `${list[key]}`, value: key})
          })
        } else {
          // console.error(this.responseText)
        }
      }
      oReq.open('GET', serverUrl + '/list')
      oReq.send()
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
