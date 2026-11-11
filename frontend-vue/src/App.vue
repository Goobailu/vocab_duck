<template>
  <div v-if="loading" class="min-h-screen flex items-center justify-center bg-[var(--bg-main)]">
    <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-amber-500"></div>
  </div>
  <LoginView v-else-if="!userStore.user" @login-success="onLogin" />
  <div v-else class="min-h-screen flex bg-[var(--bg-main)]">
    <Sidebar :user="userStore.user" @logout="onLogout" />
    <main class="flex-1 overflow-y-auto ml-64 p-6">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useUserStore } from './stores/user'
import { api, setAuthToken } from './services/api'
import { useTheme } from './composables/useTheme'
import router from './router'
import LoginView from './components/LoginView.vue'
import Sidebar from './components/Sidebar.vue'

const userStore = useUserStore()
const loading = ref(true)
const { theme } = useTheme()

// Navigation guard: redirect to library selection if user hasn't chosen a library
router.beforeEach((to) => {
  if (to.path === '/learn' && userStore.user && !userStore.user.selectedLibrary) {
    return { path: '/library' }
  }
})

onMounted(async () => {
  const token = localStorage.getItem('vocab_token')
  if (token) {
    setAuthToken(token)
    try {
      const [userRes, streakRes] = await Promise.all([
        api.get('/api/auth/me'),
        api.get('/api/dashboard/streak-calculation').catch(() => null)
      ])
      const userData = userRes.data
      userStore.setUser(userData)
      if (streakRes && streakRes.data.currentStreak !== undefined) {
        userStore.updateStreak(streakRes.data.currentStreak)
      }
    } catch {
      localStorage.removeItem('vocab_token')
    }
  }
  loading.value = false
})

async function onLogin(token, userData) {
  localStorage.setItem('vocab_token', token)
  setAuthToken(token)
  userStore.setUser(userData)
  try {
    const streakRes = await api.get('/api/dashboard/streak-calculation')
    if (streakRes.data.currentStreak !== undefined) {
      userStore.updateStreak(streakRes.data.currentStreak)
    }
  } catch (e) {
    console.log('Failed to fetch streak data:', e)
  }
}

function onLogout() {
  localStorage.removeItem('vocab_token')
  setAuthToken(null)
  userStore.clearUser()
}
</script>
