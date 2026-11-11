<template>
  <div class="p-8 animate-fadeIn">
    <div class="mb-8">
      <h2 class="text-2xl font-bold flex items-center gap-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
        <TrendingUp class="w-6 h-6 text-amber-500" /> 学习统计
      </h2>
      <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">查看你的学习进度和统计数据</p>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-blue-500/20 flex items-center justify-center mb-3">
          <BookOpen class="w-5 h-5 text-blue-400" />
        </div>
        <p class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ stats.totalWordsLearned || 0 }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">已学单词</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-green-500/20 flex items-center justify-center mb-3">
          <CheckCircle2 class="w-5 h-5 text-green-400" />
        </div>
        <p class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ stats.wordsMastered || 0 }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">已掌握</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-orange-500/20 flex items-center justify-center mb-3">
          <RotateCcw class="w-5 h-5 text-orange-400" />
        </div>
        <p class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ stats.wordsDueReview || 0 }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">待复习</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-purple-500/20 flex items-center justify-center mb-3">
          <Brain class="w-5 h-5 text-purple-400" />
        </div>
        <p class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ (stats.memoryStrength || 0).toFixed(2) }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">记忆强度</p>
      </div>
    </div>

    <!-- Learning Progress -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <div class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <Target class="w-5 h-5 text-amber-400" /> 今日学习进度
        </h3>
        
        <div class="space-y-4">
          <div>
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-medium" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-600'">新词学习</span>
              <span class="text-sm font-bold text-amber-500">{{ stats.todayNewWords || 0 }} / {{ dailyPlan.newWordTarget || 30 }}</span>
            </div>
            <div class="h-3 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'">
              <div
                class="h-full bg-gradient-to-r from-amber-400 to-orange-500 rounded-full transition-all duration-1000 ease-out"
                :style="{ width: `${Math.min(100, ((stats.todayNewWords || 0) / (dailyPlan.newWordTarget || 30)) * 100)}%` }"
              ></div>
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-medium" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-600'">单词复习</span>
              <span class="text-sm font-bold text-orange-500">{{ stats.todayReviews || 0 }} 个</span>
            </div>
            <div class="h-3 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'">
              <div
                class="h-full bg-gradient-to-r from-orange-400 to-red-500 rounded-full transition-all duration-1000 ease-out"
                :style="{ width: `${Math.min(100, ((stats.todayReviews || 0) / Math.max(stats.wordsDueReview || 30, 30)) * 100)}%` }"
              ></div>
            </div>
          </div>
        </div>

        <div class="flex items-center justify-around mt-6 pt-6 border-t" :class="theme === 'dark' ? 'border-slate-700' : 'border-gray-200'">
          <div class="text-center">
            <div class="text-2xl font-bold text-amber-500">{{ stats.todayNewWords || 0 }}</div>
            <div class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">今日新词</div>
          </div>
          <div class="w-px h-10" :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'"></div>
          <div class="text-center">
            <div class="text-2xl font-bold text-orange-500">{{ stats.todayReviews || 0 }}</div>
            <div class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">今日复习</div>
          </div>
          <div class="w-px h-10" :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'"></div>
          <div class="text-center">
            <div class="text-2xl font-bold text-green-500">{{ stats.wordsMastered || 0 }}</div>
            <div class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">已掌握</div>
          </div>
        </div>
      </div>

      <!-- Recent Reviews -->
      <div class="glass-card p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-base font-semibold flex items-center gap-2"
              :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            <History class="w-5 h-5 text-purple-400" /> 最近复习
          </h3>
          <router-link to="/review" class="text-sm text-amber-500 hover:text-amber-400 flex items-center gap-1">
            查看全部 <ChevronRight class="w-4 h-4" />
          </router-link>
        </div>
        
        <div class="space-y-3 max-h-[300px] overflow-y-auto">
          <div
            v-for="item in recentReviews"
            :key="item.id"
            class="flex items-center justify-between p-3 rounded-lg transition-colors duration-200 hover:bg-gray-50/50"
            :class="theme === 'dark' ? 'hover:bg-slate-700/30' : ''"
          >
            <div class="flex items-center gap-3">
              <div
                class="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-medium"
                :class="getMasteryLevel(item.masteryLevel).color"
              >
                {{ getMasteryLevel(item.masteryLevel).icon }}
              </div>
              <div>
                <div class="text-sm font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ item.word }}</div>
                <div class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">{{ item.meaning }}</div>
              </div>
            </div>
            <span
              class="px-2 py-1 rounded-full text-xs font-medium"
              :class="item.status === 'mastered' ? 'bg-green-500/20 text-green-400' : 'bg-amber-500/20 text-amber-400'"
            >
              {{ item.status === 'mastered' ? '轻松' : '学习中' }}
            </span>
          </div>
          
          <div v-if="recentReviews.length === 0" class="text-center py-8">
            <History class="w-10 h-10 mx-auto mb-3 opacity-50" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'" />
            <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">暂无复习记录</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Review Stats -->
    <div class="glass-card p-6">
      <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
          :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
        <BarChart3 class="w-5 h-5 text-blue-400" /> 近期复习统计
      </h3>
      
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div class="text-center">
          <div class="text-3xl font-bold text-blue-500">{{ reviewStats.totalReviews || 0 }}</div>
          <div class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">复习总数</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-bold text-green-500">{{ reviewStats.accuracyRate ? (reviewStats.accuracyRate * 100).toFixed(1) : '0.0' }}%</div>
          <div class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">正确率</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-bold text-purple-500">{{ reviewStats.masteryRate ? (reviewStats.masteryRate * 100).toFixed(1) : '0.0' }}%</div>
          <div class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">掌握率</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-bold text-orange-500">{{ reviewStats.averageScore || 0 }}</div>
          <div class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">平均分</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTheme } from '../composables/useTheme'
import {
  TrendingUp,
  BookOpen,
  CheckCircle2,
  RotateCcw,
  Brain,
  Target,
  History,
  ChevronRight,
  BarChart3
} from 'lucide-vue-next'
import { api } from '../services/api'

const router = useRouter()
const { theme } = useTheme()
const loading = ref(true)

const stats = ref({})
const dailyPlan = ref({})
const reviewStats = ref({})
const recentReviews = ref([])

function getMasteryLevel(mastery) {
  if (!mastery) return { color: 'bg-gray-100 text-gray-500', icon: '📚' }
  if (mastery >= 0.9) return { color: 'bg-green-100 text-green-600', icon: '✅' }
  if (mastery >= 0.7) return { color: 'bg-emerald-100 text-emerald-600', icon: '💪' }
  if (mastery >= 0.5) return { color: 'bg-amber-100 text-amber-600', icon: '📖' }
  if (mastery >= 0.3) return { color: 'bg-orange-100 text-orange-600', icon: '🔄' }
  return { color: 'bg-red-100 text-red-600', icon: '📝' }
}

onMounted(async () => {
  try {
    const [statsRes, dailyPlanRes, reviewStatsRes, recentReviewsRes] = await Promise.all([
      api.get('/api/dashboard/stats'),
      api.get('/api/dashboard/daily-plan').catch(() => null),
      api.get('/api/learning/review-stats', { params: { days: 7 } }),
      api.get('/api/learning/review-history', { params: { days: 30, limit: 10 } })
    ])
    
    stats.value = statsRes.data
    if (dailyPlanRes) dailyPlan.value = dailyPlanRes.data
    reviewStats.value = reviewStatsRes.data
    recentReviews.value = recentReviewsRes.data.history || []
  } catch (err) {
    console.error('Failed to load learning stats:', err)
  } finally {
    loading.value = false
  }
})
</script>