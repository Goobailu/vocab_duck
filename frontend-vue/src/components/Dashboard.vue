<template>
  <div class="space-y-6">
    <!-- Loading state -->
    <div v-if="loading" class="flex items-center justify-center h-96">
      <div class="text-center">
        <div class="w-12 h-12 border-4 border-amber-500/30 border-t-amber-500 rounded-full animate-spin mx-auto mb-4"></div>
        <p :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">加载数据中...</p>
      </div>
    </div>

    <template v-else>
      <!-- Welcome header -->
      <div>
        <h1 class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          欢迎回来, {{ user?.username || '同学' }}
        </h1>
        <p class="mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">今天也要加油学习哦</p>
      </div>

      <!-- Quick actions -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <router-link
          to="/learn"
          class="glass-card p-5 flex items-center gap-4 transition-all duration-200 group"
          :class="theme === 'dark' ? 'hover:border-amber-500/40' : 'hover:border-amber-300'"
        >
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center shrink-0 shadow-lg shadow-amber-500/30">
            <Sparkles class="w-6 h-6 text-white" />
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">学习新词</h3>
            <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">开始今日的学习任务</p>
          </div>
          <ChevronRight class="w-5 h-5 transition-colors shrink-0"
            :class="theme === 'dark' ? 'text-slate-500 group-hover:text-amber-400' : 'text-gray-400 group-hover:text-amber-500'" />
        </router-link>

        <router-link
          to="/review"
          class="glass-card p-5 flex items-center gap-4 transition-all duration-200 group cursor-pointer"
          :class="theme === 'dark' ? 'hover:border-orange-500/40' : 'hover:border-orange-300'"
        >
          <div
            class="w-12 h-12 rounded-xl bg-gradient-to-br from-orange-400 to-red-500 flex items-center justify-center shrink-0 shadow-lg shadow-orange-500/30 relative"
          >
            <RotateCcw class="w-6 h-6 text-white" />
            <span
              v-if="stats.wordsDueReview > 0"
              class="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-red-500 text-white text-xs flex items-center justify-center shadow-md"
            >
              {{ stats.wordsDueReview > 9 ? '9+' : stats.wordsDueReview }}
            </span>
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">今日复习</h3>
            <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">
              {{ stats.wordsDueReview > 0 ? `${stats.wordsDueReview} 个单词待复习` : '暂无待复习单词' }}
            </p>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <span
              v-if="stats.wordsDueReview > 0"
              class="text-xs px-2 py-0.5 rounded-full bg-orange-500/15 text-orange-400"
            >
              待复习
            </span>
            <ChevronRight class="w-5 h-5 transition-colors"
              :class="theme === 'dark' ? 'text-slate-500 group-hover:text-orange-400' : 'text-gray-400 group-hover:text-orange-400'" />
          </div>
        </router-link>

        <router-link
          to="/stats"
          class="glass-card p-5 flex items-center gap-4 transition-all duration-200 group"
          :class="theme === 'dark' ? 'hover:border-blue-500/40' : 'hover:border-blue-300'"
        >
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-400 to-cyan-500 flex items-center justify-center shrink-0 shadow-lg shadow-blue-500/30">
            <TrendingUp class="w-6 h-6 text-white" />
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">学习统计</h3>
            <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">查看今日学习进度</p>
          </div>
          <ChevronRight class="w-5 h-5 transition-colors shrink-0"
            :class="theme === 'dark' ? 'text-slate-500 group-hover:text-blue-400' : 'text-gray-400 group-hover:text-blue-400'" />
        </router-link>

        <router-link
          to="/streak"
          class="glass-card p-5 flex items-center gap-4 transition-all duration-200 group"
          :class="theme === 'dark' ? 'hover:border-red-500/40' : 'hover:border-red-300'"
        >
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-red-400 to-orange-500 flex items-center justify-center shrink-0 shadow-lg shadow-red-500/30">
            <Flame class="w-6 h-6 text-white" />
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">连续学习</h3>
            <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ stats.streakDays || 0 }} 天</p>
          </div>
          <ChevronRight class="w-5 h-5 transition-colors shrink-0"
            :class="theme === 'dark' ? 'text-slate-500 group-hover:text-red-400' : 'text-gray-400 group-hover:text-red-400'" />
        </router-link>
      </div>

      <!-- Stats grid -->
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <div class="stat-card">
          <div class="w-10 h-10 rounded-lg bg-amber-500/20 flex items-center justify-center mb-3">
            <BookOpen class="w-5 h-5 text-amber-500" />
          </div>
          <p class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ stats.totalWordsLearned ?? 0 }}</p>
          <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">已学单词</p>
        </div>

        <div class="stat-card">
          <div class="w-10 h-10 rounded-lg bg-emerald-500/20 flex items-center justify-center mb-3">
            <Award class="w-5 h-5 text-emerald-400" />
          </div>
          <p class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ stats.wordsMastered ?? 0 }}</p>
          <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">已掌握</p>
        </div>

        <div class="stat-card">
          <div class="w-10 h-10 rounded-lg bg-orange-500/20 flex items-center justify-center mb-3">
            <RotateCcw class="w-5 h-5 text-orange-400" />
          </div>
          <p class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ stats.wordsDueReview ?? 0 }}</p>
          <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">待复习</p>
        </div>

        <div class="stat-card">
          <div class="w-10 h-10 rounded-lg bg-red-500/20 flex items-center justify-center mb-3">
            <Flame class="w-5 h-5 text-red-400" />
          </div>
          <p class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ stats.streakDays ?? 0 }}</p>
          <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">连续学习</p>
        </div>

        <div class="stat-card">
          <div class="w-10 h-10 rounded-lg bg-cyan-500/20 flex items-center justify-center mb-3">
            <Target class="w-5 h-5 text-cyan-400" />
          </div>
          <p class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            {{ stats.avgRecallRate != null ? `${Math.round(stats.avgRecallRate * 100)}%` : '0%' }}
          </p>
          <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">掌握率</p>
        </div>

        <div class="stat-card">
          <div class="w-10 h-10 rounded-lg bg-purple-500/20 flex items-center justify-center mb-3">
            <Brain class="w-5 h-5 text-purple-400" />
          </div>
          <p class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            {{ stats.memoryStrength ? stats.memoryStrength.toFixed(2) : '0.00' }}
          </p>
          <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">记忆强度</p>
        </div>
      </div>

      <!-- Quick links -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <router-link
          to="/review-plan"
          class="glass-card p-5 flex items-center justify-between transition-all duration-200 group hover:shadow-lg"
          :class="theme === 'dark' ? 'hover:border-purple-500/40' : 'hover:border-purple-300'"
        >
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-400 to-indigo-500 flex items-center justify-center shrink-0">
              <CalendarDays class="w-6 h-6 text-white" />
            </div>
            <div>
              <h3 class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">复习计划</h3>
              <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">查看本周复习安排和紧急单词</p>
            </div>
          </div>
          <ChevronRight class="w-5 h-5 transition-colors"
            :class="theme === 'dark' ? 'text-slate-500 group-hover:text-purple-400' : 'text-gray-400 group-hover:text-purple-400'" />
        </router-link>

        <router-link
          to="/analytics"
          class="glass-card p-5 flex items-center justify-between transition-all duration-200 group hover:shadow-lg"
          :class="theme === 'dark' ? 'hover:border-purple-500/40' : 'hover:border-purple-300'"
        >
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-400 to-pink-500 flex items-center justify-center shrink-0">
              <BarChart3 class="w-6 h-6 text-white" />
            </div>
            <div>
              <h3 class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">数据分析</h3>
              <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">基于记忆诊断的深度分析报告</p>
            </div>
          </div>
          <ChevronRight class="w-5 h-5 transition-colors"
            :class="theme === 'dark' ? 'text-slate-500 group-hover:text-purple-400' : 'text-gray-400 group-hover:text-purple-400'" />
        </router-link>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTheme } from '../composables/useTheme'
import { useUserStore } from '../stores/user'
import {
  BookOpen,
  Brain,
  RotateCcw,
  TrendingUp,
  Flame,
  Target,
  Award,
  ChevronRight,
  Sparkles,
  CalendarDays,
  BarChart3
} from 'lucide-vue-next'
import { api } from '../services/api'

const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  }
})

const router = useRouter()
const { theme } = useTheme()
const userStore = useUserStore()
const loading = ref(true)
const Math = window.Math

const stats = ref({
  totalWordsLearned: 0,
  wordsMastered: 0,
  wordsDueReview: 0,
  streakDays: 0,
  avgRecallRate: 0,
  memoryStrength: 0,
  todayNewWords: 0,
  todayReviews: 0,
  dailyNewWordsGoal: 30
})



async function loadDashboardData() {
  loading.value = true
  try {
    const [statsRes, streakRes] = await Promise.all([
      api.get('/api/dashboard/stats'),
      api.get('/api/dashboard/streak-calculation').catch(() => null)
    ])
    
    stats.value = {
      ...stats.value,
      ...statsRes.data
    }
    
    if (streakRes && streakRes.data.currentStreak !== undefined) {
      stats.value.streakDays = streakRes.data.currentStreak
    }
    
    if (userStore.user && stats.value.streakDays !== undefined) {
      userStore.setUser({
        ...userStore.user,
        streakDays: stats.value.streakDays
      })
    }
  } catch (err) {
    console.error('Failed to load dashboard data:', err)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadDashboardData()
})

const route = useRoute()
watch(() => route.path, () => {
  if (route.path === '/') {
    loadDashboardData()
  }
})

</script>

<style scoped>
@keyframes pulse-slow {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.animate-pulse-slow {
  animation: pulse-slow 2s ease-in-out infinite;
}

.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
}

.dark .glass-card {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(255, 255, 255, 0.05);
}
</style>
