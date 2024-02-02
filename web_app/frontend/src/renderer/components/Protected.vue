<template>
  <div>
    <div class="navbar-header col-md-12 modal-title">
      <router-link to="/">
        <i class="fa fa-home" aria-hidden="true"/> Home
      </router-link>
      / Polynomial Check
    </div>

    <div class="container text-center">

      <div class="row">
        <div class="col-md-6 offset-md-3">
          <h3>Please enter password to access this page.</h3>

          <form v-on:submit.prevent="validateBeforeSubmit">
            <div class="form-group text-left">
              <label class="custom-label control-label">Password</label>
              <input class="form-control password-field" type="password" name="password" v-model.trim="password">
              <span class="error help-block" ></span>
            </div>
            <div class="text-danger" v-if="error"><p>Incorrect password.</p></div>
            <button class="btn btn-primary" type="submit">Submit</button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {serverUrl} from './conf'

export default {
  data () {
    return {
      error: null,
      password: null
    }
  },
  methods: {
    async validateBeforeSubmit () {
      let url = `${serverUrl}/checkPass/${this.password}`

      await this.$http.get(url, {
        mode: 'cors',
        cache: 'no-cache',
        headers: {
          'Content-Type': 'application/json'
        }
      })
          .then(res => {
            // console.log(res.data)
            if (res.data[0]) {
              this.error = false
              localStorage.setItem('authenticated', 'true')
              this.$router.push('/poly')
            }
          })
          .catch(e => {
            this.error = true
            console.error(e)
          })

      // const res = await fetch(url, {
      //   method: 'GET',
      //   mode: 'cors',
      //   cache: 'no-cache',
      //   headers: {
      //     'Content-Type': 'application/json'
      //   }
      // })
      // console.log(res.json())

      // if (this.password === process.env.VUE_APP_PASSWORD) {
      //   this.error = false
      //   localStorage.setItem('user-password', this.password)
      //   this.$router.push('/poly')
      // } else {
      //   this.error = true
      // }
    }
  }
}
</script>
