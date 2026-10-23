<template>
  <div class="dropzone vue-dropzone"></div>
</template>

<script>
import Dropzone from 'dropzone'
import { toRaw } from 'vue'

// Dropzone scans the page for .dropzone elements on load; this component attaches it explicitly.
Dropzone.autoDiscover = false

// Thin wrapper around Dropzone.js (the library the old Vue 2 dropzone component wrapped).
// The parent passes the Dropzone options object and talks to the instance through `dropzone`.
export default {
  name: 'DropzoneArea',
  props: {
    options: { type: Object, required: true }
  },
  mounted () {
    // Keep Dropzone away from Vue's reactive proxies: it mutates the options and file objects itself.
    this.dropzone = new Dropzone(this.$el, toRaw(this.options))
  },
  beforeUnmount () {
    if (this.dropzone) this.dropzone.destroy()
  },
  methods: {
    getQueuedFiles () {
      return this.dropzone.getQueuedFiles()
    },
    processQueue () {
      this.dropzone.processQueue()
    },
    setHeader (name, value) {
      this.dropzone.options.headers[name] = value
    }
  }
}
</script>
