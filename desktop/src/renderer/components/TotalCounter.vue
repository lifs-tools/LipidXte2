<template>
    <div>
        <p>{{ total }}</p>
        <button-counter v-on:increment="incrementTotal"></button-counter>
        <button-counter v-on:increment="incrementTotal"></button-counter>
    </div>
</template>

<script>
  import ButtonCounter from './ButtonCounter'

  export default {
    name: 'total-counter',
    components: { ButtonCounter },
    data: function () {
      return {
        total: 0
      }
    },
    methods: {
      incrementTotal: function () {
        this.total += 1
      }
    }
  }
</script>

<style scoped>
    p {
        color: red;
        margin-bottom: 10px;
    }
</style>
