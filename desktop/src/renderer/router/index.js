import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

export default new Router({
  base: __dirname,
  routes: [
    {
      path: '/',
      name: 'landing-page',
      component: require('@/components/LandingPage').default
    },
    {
      path: '/sample/:batch_title*',
      name: 'sample-page',
      component: require('@/components/SamplePage').default,
      props: true
    },
    {
      path: '/login',
      name: 'login',
      component: require('@/components/Protected').default
    },
    {
      path: '/help',
      name: 'help',
      component: require('@/components/HelpPage').default
    },
    {
      path: '/poly',
      name: 'polynomial-page',
      component: require('@/components/PolynomialPage').default,
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
      component: require('@/components/SlensPage').default,
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
