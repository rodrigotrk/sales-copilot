import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'
import { landingPath, loadMe } from '@/features/auth/session'
import LoginView from '@/features/auth/LoginView.vue'
import LinkView from '@/features/auth/LinkView.vue'
import LandingView from '@/features/business/LandingView.vue'
import BusinessListView from '@/features/business/BusinessListView.vue'
import PlaybookView from '@/features/playbook/PlaybookView.vue'

export function createAppRouter(history: RouterHistory = createWebHistory(import.meta.env.BASE_URL)) {
  const router = createRouter({
    history,
    routes: [
      { path: '/entrar', component: LoginView, meta: { public: true } },
      { path: '/entrar/link', component: LinkView, props: { kind: 'login' }, meta: { public: true } },
      { path: '/entrar/vincular', component: LinkView, props: { kind: 'identity' }, meta: { public: true } },
      { path: '/', name: 'landing', component: LandingView },
      { path: '/negocios', component: BusinessListView },
      { path: '/n/:businessId/playbook', component: PlaybookView, props: true },
      { path: '/:rest(.*)*', redirect: '/' },
    ],
  })

  router.beforeEach(async (to) => {
    if (to.meta.public) return true
    const current = await loadMe()
    if (!current) return '/entrar'
    if (to.name === 'landing') return landingPath(current) ?? true
    return true
  })

  return router
}
