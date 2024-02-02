<template>
    <button v-on:click="incrementCounter">{{ counter }}</button>
</template>

<script>
  export default {
    name: 'button-counter',
    data: function () {
      return {
        counter: 0
      }
    },
    methods: {
      incrementCounter: function () {
        this.counter += 1
        this.$emit('increment')
      }
    }
  }
</script>

<style scoped>
    * {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
    }
</style>
