import { createRouter, createWebHistory } from 'vue-router'

import LoginView from './views/LoginView.vue'
import AiAssistantView from './views/ai/AiAssistantView.vue'
import AiInsightsView from './views/ai/AiInsightsView.vue'
import AiKnowledgeView from './views/ai/AiKnowledgeView.vue'
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
      path: '/ai/assistant',
      name: 'ai-assistant',
      component: AiAssistantView,
    },
    {
      path: '/ai/insights',
      name: 'ai-insights',
      component: AiInsightsView,
    },
    {
      path: '/ai/knowledge',
      name: 'ai-knowledge',
      component: AiKnowledgeView,
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'workbench',
      component: () => import('./views/WorkbenchView.vue'),
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
