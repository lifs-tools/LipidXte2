import { createRouter, createWebHashHistory } from 'vue-router'

import LandingPage from '@/components/LandingPage.vue'
import SamplePage from '@/components/SamplePage.vue'
import HelpPage from '@/components/HelpPage.vue'
import PolynomialPage from '@/components/PolynomialPage.vue'
import SlensPage from '@/components/SlensPage.vue'

export default createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'landing-page',
      component: LandingPage
    },
    {
      path: '/sample/:batch_title*',
      name: 'sample-page',
      component: SamplePage,
      props: true
    },
    {
      path: '/help',
      name: 'help',
      component: HelpPage
    },
    {
      path: '/poly',
      name: 'polynomial-page',
      component: PolynomialPage
    },
    {
      path: '/slens',
      name: 'slens-page',
      component: SlensPage
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
})
