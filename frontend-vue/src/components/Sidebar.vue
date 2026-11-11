<template>
  <aside
    class="fixed left-0 top-0 h-screen w-64 flex flex-col z-40 backdrop-blur-xl transition-colors duration-300"
    :class="[
      theme === 'dark'
        ? 'bg-slate-800/80 border-r border-slate-700/50'
        : 'bg-white/80 border-r border-gray-200'
    ]"
  >
    <!-- Logo area -->
    <div class="px-5 py-6 border-b transition-colors duration-300"
         :class="theme === 'dark' ? 'border-slate-700/50' : 'border-gray-200'">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center shadow-lg shadow-amber-500/20 shrink-0">
          <span class="text-xl">🦆</span>
        </div>
        <div class="min-w-0">
          <h1 class="text-base font-bold truncate" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">背词鸭</h1>
          <p class="text-xs truncate" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">Vocab Duck</p>
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 px-3 py-4 space-y-4 overflow-y-auto">
      <!-- Main Navigation -->
      <div>
        <p class="px-3 mb-2 text-xs font-semibold uppercase tracking-wider"
           :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">学习</p>
        <div class="space-y-1">
          <router-link
            v-for="item in mainNavItems"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 group"
            :class="isActive(item.path)
              ? (theme === 'dark'
                  ? 'bg-amber-600/20 text-amber-400 border border-amber-500/30 shadow-lg shadow-amber-500/10'
                  : 'bg-amber-50 text-amber-700 border border-amber-200 shadow-sm')
              : (theme === 'dark'
                  ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/40 border border-transparent'
                  : 'text-gray-500 hover:text-gray-800 hover:bg-gray-100 border border-transparent')"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0 transition-all duration-200"
              :class="isActive(item.path)
                ? 'bg-gradient-to-br from-amber-400 to-orange-500 text-white'
                : (theme === 'dark' ? 'bg-slate-700/50 text-slate-500 group-hover:text-slate-300' : 'bg-gray-100 text-gray-400 group-hover:text-gray-600')"
            >
              <component :is="item.icon" class="w-4 h-4" />
            </div>
            <span class="truncate">{{ item.label }}</span>
            <span
              v-if="item.badge"
              class="ml-auto px-2 py-0.5 text-xs rounded-full bg-red-500 text-white font-bold"
            >
              {{ item.badge }}
            </span>
          </router-link>
        </div>
      </div>

      <!-- Review Navigation -->
      <div>
        <p class="px-3 mb-2 text-xs font-semibold uppercase tracking-wider"
           :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">复习</p>
        <div class="space-y-1">
          <router-link
            v-for="item in reviewNavItems"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 group"
            :class="isActive(item.path)
              ? (theme === 'dark'
                  ? 'bg-purple-600/20 text-purple-400 border border-purple-500/30 shadow-lg shadow-purple-500/10'
                  : 'bg-purple-50 text-purple-700 border border-purple-200 shadow-sm')
              : (theme === 'dark'
                  ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/40 border border-transparent'
                  : 'text-gray-500 hover:text-gray-800 hover:bg-gray-100 border border-transparent')"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0 transition-all duration-200"
              :class="isActive(item.path)
                ? 'bg-gradient-to-br from-purple-400 to-indigo-500 text-white'
                : (theme === 'dark' ? 'bg-slate-700/50 text-slate-500 group-hover:text-slate-300' : 'bg-gray-100 text-gray-400 group-hover:text-gray-600')"
            >
              <component :is="item.icon" class="w-4 h-4" />
            </div>
            <span class="truncate">{{ item.label }}</span>
          </router-link>
        </div>
      </div>

      <!-- Stats Navigation -->
      <div>
        <p class="px-3 mb-2 text-xs font-semibold uppercase tracking-wider"
           :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">统计</p>
        <div class="space-y-1">
          <router-link
            v-for="item in statsNavItems"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 group"
            :class="isActive(item.path)
              ? (theme === 'dark'
                  ? 'bg-blue-600/20 text-blue-400 border border-blue-500/30 shadow-lg shadow-blue-500/10'
                  : 'bg-blue-50 text-blue-700 border border-blue-200 shadow-sm')
              : (theme === 'dark'
                  ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/40 border border-transparent'
                  : 'text-gray-500 hover:text-gray-800 hover:bg-gray-100 border border-transparent')"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0 transition-all duration-200"
              :class="isActive(item.path)
                ? 'bg-gradient-to-br from-blue-400 to-cyan-500 text-white'
                : (theme === 'dark' ? 'bg-slate-700/50 text-slate-500 group-hover:text-slate-300' : 'bg-gray-100 text-gray-400 group-hover:text-gray-600')"
            >
              <component :is="item.icon" class="w-4 h-4" />
            </div>
            <span class="truncate">{{ item.label }}</span>
          </router-link>
        </div>
      </div>

      <!-- Tools Navigation -->
      <div>
        <p class="px-3 mb-2 text-xs font-semibold uppercase tracking-wider"
           :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">工具</p>
        <div class="space-y-1">
          <router-link
            v-for="item in toolsNavItems"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 group"
            :class="isActive(item.path)
              ? (theme === 'dark'
                  ? 'bg-emerald-600/20 text-emerald-400 border border-emerald-500/30 shadow-lg shadow-emerald-500/10'
                  : 'bg-emerald-50 text-emerald-700 border border-emerald-200 shadow-sm')
              : (theme === 'dark'
                  ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/40 border border-transparent'
                  : 'text-gray-500 hover:text-gray-800 hover:bg-gray-100 border border-transparent')"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0 transition-all duration-200"
              :class="isActive(item.path)
                ? 'bg-gradient-to-br from-emerald-400 to-teal-500 text-white'
                : (theme === 'dark' ? 'bg-slate-700/50 text-slate-500 group-hover:text-slate-300' : 'bg-gray-100 text-gray-400 group-hover:text-gray-600')"
            >
              <component :is="item.icon" class="w-4 h-4" />
            </div>
            <span class="truncate">{{ item.label }}</span>
            <span v-if="item.badge" class="ml-auto px-2 py-0.5 text-xs rounded-full bg-emerald-500/20 text-emerald-300">{{ item.badge }}</span>
          </router-link>
        </div>
      </div>
    </nav>

    <!-- User info at bottom -->
    <div class="px-4 py-4 border-t transition-colors duration-300"
         :class="theme === 'dark' ? 'border-slate-700/50' : 'border-gray-200'">
      <div class="flex items-center gap-3 mb-3">
        <div class="w-9 h-9 rounded-full bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center text-white text-sm font-bold shrink-0">
          {{ userInitial }}
        </div>
        <div class="min-w-0 flex-1">
          <p class="text-sm font-medium truncate" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ user?.username || '未登录' }}</p>
          <div class="flex items-center gap-2 mt-1">
            <div class="flex items-center gap-1">
              <span class="text-orange-400 text-sm">🔥</span>
              <span class="text-xs font-semibold text-orange-400">{{ userStore.streakDays || (user?.streakDays ?? 0) }}</span>
            </div>
            <div class="flex-1 h-1.5 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'">
              <div
                class="h-full bg-gradient-to-r from-orange-400 to-red-500 rounded-full transition-all duration-500"
                :style="{ width: `${Math.min(100, Math.max(0, (userStore.streakDays || (user?.streakDays ?? 0)) / 7 * 100))}%` }"
              ></div>
            </div>
            <span class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">天</span>
          </div>
        </div>
      </div>
      <router-link
        to="/settings"
        class="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm transition-all duration-200 mb-2"
        :class="theme === 'dark'
          ? 'text-slate-400 hover:text-amber-400 hover:bg-amber-500/10 border border-slate-700/50 hover:border-amber-500/30'
          : 'text-gray-500 hover:text-amber-600 hover:bg-amber-50 border border-gray-200 hover:border-amber-300'"
      >
        <Settings class="w-4 h-4" />
        用户设置
      </router-link>
      <router-link
        to="/library"
        class="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm transition-all duration-200 mb-2"
        :class="[
          isActive('/library')
            ? (theme === 'dark'
                ? 'bg-amber-600/20 text-amber-400 border border-amber-500/30'
                : 'bg-amber-50 text-amber-700 border border-amber-200')
            : (theme === 'dark'
                ? 'text-slate-400 hover:text-amber-400 hover:bg-amber-500/10 border border-slate-700/50 hover:border-amber-500/30'
                : 'text-gray-500 hover:text-amber-600 hover:bg-amber-50 border border-gray-200 hover:border-amber-300')
        ]"
      >
        <Library class="w-4 h-4" />
        {{ user?.selectedLibrary ? '切换词库' : '选择词库' }}
      </router-link>
      <button
        @click="emit('logout')"
        class="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm transition-all duration-200"
        :class="theme === 'dark'
          ? 'text-slate-400 hover:text-red-400 hover:bg-red-500/10 border border-slate-700/50 hover:border-red-500/30'
          : 'text-gray-500 hover:text-red-500 hover:bg-red-50 border border-gray-200 hover:border-red-300'"
      >
        <LogOut class="w-4 h-4" />
        退出登录
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from '../composables/useTheme'
import { useUserStore } from '../stores/user'
import {
  LayoutDashboard,
  MessageCircle,
  BookOpen,
  RotateCcw,
  BarChart3,
  Settings,
  LogOut,
  Library,
  TrendingUp,
  CalendarDays,
  Flame,
  Target,
  Sparkles
} from 'lucide-vue-next'
import { api } from '../services/api'

const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['logout'])
const route = useRoute()
const { theme } = useTheme()
const userStore = useUserStore()

async function fetchStreak() {
  try {
    const res = await api.get('/api/dashboard/streak-calculation')
    if (res.data.currentStreak !== undefined) {
      userStore.updateStreak(res.data.currentStreak)
    }
  } catch (err) {
    console.error('Failed to fetch streak:', err)
  }
}

onMounted(() => {
  fetchStreak()
})

watch(() => props.user, () => {
  fetchStreak()
}, { deep: true })

const mainNavItems = [
  { path: '/', label: '主页', icon: LayoutDashboard },
  { path: '/learn', label: '学习新词', icon: BookOpen }
]

const reviewNavItems = [
  { path: '/review', label: '复习中心', icon: RotateCcw },
  { path: '/review-plan', label: '复习计划', icon: CalendarDays }
]

const statsNavItems = [
  { path: '/stats', label: '学习统计', icon: TrendingUp },
  { path: '/streak', label: '连续学习', icon: Flame },
  { path: '/analytics', label: '数据分析', icon: BarChart3 }
]

const toolsNavItems = [
  { path: '/chat', label: '智能对话', icon: MessageCircle, badge: 'AI' }
]

function isActive(path) {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path === path || route.path.startsWith(path + '/')
}

const userInitial = computed(() => {
  const name = props.user?.username
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
})
</script>
