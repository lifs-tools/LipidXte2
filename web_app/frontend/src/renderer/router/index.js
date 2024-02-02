import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'landing-page',
      component: require('@/components/LandingPage')
    },
    {
      path: '/sample/:batch_title*',
      name: 'sample-page',
      component: require('@/components/SamplePage'),
      props: true
    },
    {
      path: '/login',
      name: 'login',
      component: require('@/components/Protected')
    },
    {
      path: '/help',
      name: 'help',
      component: require('@/components/HelpPage')
    },
    {
      path: '/poly',
      name: 'polynomial-page',
      component: require('@/components/PolynomialPage'),
      beforeEnter (to, from, next) {
        if (to.name !== 'login' && !localStorage.getItem('authenticated')) {
          next({
            path: 'login',
            replace: true
          })
        } else {
          next()
        }
      }
    },
    {
      path: '/slens',
      name: 'slens-page',
      component: require('@/components/SlensPage'),
      beforeEnter (to, from, next) {
        if (to.name !== 'login' && !localStorage.getItem('authenticated')) {
          next({
            path: 'login',
            replace: true
          })
        } else {
          next()
        }
      }
    },
    {
      path: '*',
      redirect: '/'
    }
  ]
})
