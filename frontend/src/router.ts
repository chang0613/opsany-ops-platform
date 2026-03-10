import { createRouter, createWebHistory } from 'vue-router'

import LoginView from './views/LoginView.vue'
import WorkbenchView from './views/WorkbenchView.vue'
import { getToken } from './lib/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'workbench',
      component: WorkbenchView,
    },
  ],
})

router.beforeEach((to) => {
  const token = getToken()

  if (to.path !== '/login' && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.path === '/login' && token) {
    return { path: '/o/workbench/' }
  }

  return true
})

export default router
