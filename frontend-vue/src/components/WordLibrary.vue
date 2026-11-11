<template>
  <div class="min-h-screen flex items-center justify-center p-6 transition-colors duration-300"
       :class="theme === 'dark'
         ? 'bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900'
         : 'bg-gradient-to-br from-amber-50 via-white to-orange-50'">
    <div class="max-w-5xl w-full">
      <!-- Header -->
      <div class="text-center mb-10">
        <div class="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-gradient-to-br from-amber-400 to-orange-500 mb-5 shadow-lg shadow-amber-500/20">
          <span class="text-4xl">🦆</span>
        </div>
        <h1 class="text-3xl font-bold mb-3" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          选择你的背诵词库
        </h1>
        <p class="text-base" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">
          请选择一个词库开始你的单词学习之旅，完成全部背诵后会有惊喜哦！
        </p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="w-10 h-10 border-4 border-amber-500/30 border-t-amber-500 rounded-full animate-spin"></div>
      </div>

      <!-- Library Cards -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        <div
          v-for="lib in libraries"
          :key="lib.id"
          @click="selectLibrary(lib)"
          class="group relative rounded-2xl p-6 cursor-pointer transition-all duration-300 border backdrop-blur-sm"
          :class="[
            theme === 'dark'
              ? 'bg-slate-800/60 border-slate-700/50 hover:border-amber-500/50 hover:bg-slate-800/80'
              : 'bg-white/80 border-gray-200 hover:border-amber-400 hover:shadow-xl hover:shadow-amber-100/50',
            selectedId === lib.id ? (theme === 'dark' ? 'border-amber-500 ring-2 ring-amber-500/30' : 'border-amber-500 ring-2 ring-amber-500/20') : ''
          ]"
        >
          <!-- Selected Badge -->
          <div v-if="selectedId === lib.id"
               class="absolute top-4 right-4 w-7 h-7 rounded-full bg-amber-500 flex items-center justify-center">
            <svg class="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>

          <!-- Icon -->
          <div class="text-4xl mb-4">{{ lib.icon }}</div>

          <!-- Name -->
          <h3 class="text-xl font-bold mb-1" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            {{ lib.name }}
          </h3>
          <p class="text-xs mb-3" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">
            {{ lib.nameEn }}
          </p>

          <!-- Description -->
          <p class="text-sm mb-5 leading-relaxed" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">
            {{ lib.description }}
          </p>

          <!-- Stats -->
          <div class="flex items-center gap-4 text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">
            <div class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
              <span>{{ lib.totalWords || 0 }} 词</span>
            </div>
            <div class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
              </svg>
              <span>难度 {{ '★'.repeat(lib.maxDifficulty) }}{{ '☆'.repeat(5 - lib.maxDifficulty) }}</span>
            </div>
          </div>

          <!-- Progress (if learned some words) -->
          <div v-if="lib.learnedWords > 0" class="mt-4">
            <div class="flex items-center justify-between text-xs mb-1.5">
              <span :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">
                已学 {{ lib.learnedWords }} / {{ lib.totalWords }}
              </span>
              <span class="text-amber-500 font-medium">
                {{ Math.round((lib.learnedWords / lib.totalWords) * 100) }}%
              </span>
            </div>
            <div class="h-1.5 rounded-full overflow-hidden"
                 :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'">
              <div class="h-full rounded-full bg-gradient-to-r from-amber-500 to-orange-500 transition-all duration-500"
                   :style="{ width: `${(lib.learnedWords / lib.totalWords) * 100}%` }"></div>
            </div>
          </div>

          <!-- Hover Hint -->
          <div class="mt-4 pt-4 border-t transition-opacity duration-300 opacity-0 group-hover:opacity-100"
               :class="theme === 'dark' ? 'border-slate-700/50' : 'border-gray-100'">
            <p class="text-xs text-center" :class="theme === 'dark' ? 'text-amber-400' : 'text-amber-600'">
              {{ selectedId === lib.id ? '当前词库' : '点击选择此词库' }} →
            </p>
          </div>
        </div>
      </div>

      <!-- Current selection hint -->
      <div v-if="selectedId && !selecting" class="mt-8 text-center">
        <div class="inline-flex items-center gap-3 px-6 py-3 rounded-full backdrop-blur-sm"
             :class="theme === 'dark' ? 'bg-slate-800/60 border border-slate-700/50' : 'bg-white/80 border border-gray-200'">
          <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">
            当前词库:
          </span>
          <span class="text-sm font-semibold text-amber-500">
            {{ currentLibraryName }}
          </span>
          <button @click="goToLearn"
                  class="ml-2 px-4 py-1.5 rounded-full text-xs font-medium text-white bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 transition-all shadow-md shadow-amber-500/20">
            开始背诵 →
          </button>
        </div>
      </div>

      <!-- Selecting Toast -->
      <div v-if="selecting" class="fixed bottom-8 left-1/2 -translate-x-1/2 px-6 py-3 rounded-full bg-amber-500 text-white text-sm font-medium shadow-lg shadow-amber-500/30 flex items-center gap-2">
        <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
        正在切换词库...
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTheme } from '../composables/useTheme'
import { useUserStore } from '../stores/user'
import { api } from '../services/api'

const { theme } = useTheme()
const router = useRouter()
const userStore = useUserStore()

const libraries = ref([])
const loading = ref(true)
const selecting = ref(false)

const selectedId = computed(() => userStore.user?.selectedLibrary || '')
const currentLibraryName = computed(() => {
  const lib = libraries.value.find(l => l.id === selectedId.value)
  return lib ? lib.name : ''
})

async function loadLibraries() {
  loading.value = true
  try {
    const res = await api.get('/api/learning/libraries')
    libraries.value = res.data
  } catch (err) {
    console.error('Failed to load libraries:', err)
  } finally {
    loading.value = false
  }
}

async function selectLibrary(lib) {
  if (selecting.value || lib.id === selectedId.value) return
  selecting.value = true
  try {
    await api.post('/api/learning/select-library', { libraryId: lib.id })
    // Update user store
    if (userStore.user) {
      userStore.setUser({ ...userStore.user, selectedLibrary: lib.id })
    }
    // Navigate to learn page after a brief delay
    setTimeout(() => {
      router.push('/learn')
    }, 500)
  } catch (err) {
    console.error('Failed to select library:', err)
  } finally {
    selecting.value = false
  }
}

function goToLearn() {
  router.push('/learn')
}

onMounted(() => {
  loadLibraries()
})
</script>
