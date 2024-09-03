<template>
    <div>
    <button v-on:click="createChart">{{ url }}</button>
        <div ref="test">Test</div>
    </div>
</template>

<script>
  const {BrowserWindow} = require('electron').remote

  export default {
    name: 'chart-window',
    props: ['url'],
    methods: {
      createChart: function () {
        // remote.getCurrentWindow().setFullScreen(true);
        let win = new BrowserWindow({parent: this.$electron.mainWindow, width: 800, height: 600})
        win.on('closed', () => {
          win = null
        })

        // Load a remote URL
        win.loadURL(this.url)
        // win.loadURL('data: text/html; charset=utf-8,' + encodeURI(this.$refs.test.innerHTML))
      }
    }
  }
</script>

<style scoped>
    html, body {
        width: 100%;
        height: 100%;
        margin: 0px;
    }

    #chartdiv {
        width: 100%;
        height: 100%;
    }
</style>
