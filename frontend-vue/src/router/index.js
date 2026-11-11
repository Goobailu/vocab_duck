import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'dashboard', component: () => import('../components/Dashboard.vue') },
  { path: '/chat', name: 'chat', component: () => import('../components/Chat.vue') },
  { path: '/learn', name: 'learn', component: () => import('../components/Learning.vue') },
  { path: '/review', name: 'review', component: () => import('../components/Review.vue') },
  { path: '/analytics', name: 'analytics', component: () => import('../components/Analytics.vue') },
  { path: '/settings', name: 'settings', component: () => import('../components/Settings.vue') },
  { path: '/library', name: 'library', component: () => import('../components/WordLibrary.vue') },
  { path: '/stats', name: 'stats', component: () => import('../components/LearningStats.vue') },
  { path: '/review-plan', name: 'reviewPlan', component: () => import('../components/ReviewPlan.vue') },
  { path: '/streak', name: 'streak', component: () => import('../components/Streak.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
