<template>
  <div class="p-8 animate-fadeIn">
    <div class="mb-8">
      <h2 class="text-2xl font-bold flex items-center gap-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
        <Flame class="w-6 h-6 text-red-500" /> 连续学习
      </h2>
      <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">坚持学习，成就更好的自己</p>
    </div>

    <!-- Main Streak Card -->
    <div class="glass-card p-8 mb-8 relative overflow-hidden">
      <div class="absolute top-0 right-0 w-48 h-48 bg-gradient-to-br from-orange-500/10 to-red-500/10 rounded-full blur-3xl -mr-24 -mt-24"></div>
      <div class="absolute bottom-0 left-0 w-32 h-32 bg-gradient-to-tr from-amber-500/10 to-yellow-500/10 rounded-full blur-2xl -ml-16 -mb-16"></div>
      
      <div class="relative">
        <div class="flex flex-col md:flex-row items-center justify-between gap-8">
          <div class="flex items-center gap-6">
            <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-orange-400 to-red-500 flex items-center justify-center shadow-xl shadow-orange-500/30 animate-pulse-slow">
              <Flame class="w-10 h-10 text-white" />
            </div>
            <div>
              <h3 class="text-lg font-medium" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-500'">连续学习</h3>
              <div class="text-5xl font-bold bg-gradient-to-r from-orange-400 to-red-500 bg-clip-text text-transparent">
                {{ stats.streakDays || 0 }}
              </div>
              <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ getStreakMessage(stats.streakDays || 0) }}</p>
            </div>
          </div>
          
          <div class="flex items-center gap-8">
            <div class="text-center">
              <div class="w-14 h-14 rounded-xl flex items-center justify-center" :class="theme === 'dark' ? 'bg-slate-700/50' : 'bg-gray-100'">
                <CalendarDays class="w-7 h-7" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'" />
              </div>
              <div class="text-2xl font-bold mt-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">90</div>
              <div class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">统计天数</div>
            </div>
            <div class="text-center">
              <div class="w-14 h-14 rounded-xl flex items-center justify-center bg-emerald-500/20">
                <CheckCircle2 class="w-7 h-7 text-emerald-400" />
              </div>
              <div class="text-2xl font-bold mt-2 text-emerald-500">{{ activeDays }}</div>
              <div class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">学习天数</div>
            </div>
            <div class="text-center">
              <div class="w-14 h-14 rounded-xl flex items-center justify-center bg-amber-500/20">
                <Target class="w-7 h-7 text-amber-400" />
              </div>
              <div class="text-2xl font-bold mt-2 text-amber-500">{{ avgDailyWords }}</div>
              <div class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">日均单词</div>
            </div>
          </div>
        </div>

        <!-- Next Milestone -->
        <div v-if="getNextMilestone()" class="mt-8 p-5 rounded-xl" :class="theme === 'dark' ? 'bg-slate-700/50' : 'bg-gradient-to-r from-amber-50 to-orange-50'">
          <div class="flex items-center justify-between mb-3">
            <span class="text-sm font-medium flex items-center gap-2" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">
              <Sparkles class="w-4 h-4 text-amber-500" />
              下一个里程碑：{{ getNextMilestone().icon }} {{ getNextMilestone().days }}天 ({{ getNextMilestone().title }})
            </span>
            <span class="text-sm font-semibold text-orange-500">
              还差 {{ getNextMilestone().days - (stats.streakDays || 0) }} 天
            </span>
          </div>
          <div class="h-3 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-slate-600/50' : 'bg-gray-200'">
            <div
              class="h-full bg-gradient-to-r from-orange-400 to-red-500 rounded-full transition-all duration-1000 ease-out"
              :style="{ width: `${Math.min(100, ((stats.streakDays || 0) / getNextMilestone().days) * 100)}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <!-- Recent Learning Table -->
      <div class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <Calendar class="w-5 h-5 text-amber-400" /> 近7天学习记录
        </h3>
        
        <div class="overflow-hidden rounded-xl border" :class="theme === 'dark' ? 'border-slate-700' : 'border-gray-200'">
          <table class="w-full">
            <thead>
              <tr :class="theme === 'dark' ? 'bg-slate-700/50' : 'bg-gray-50'">
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">日期</th>
                <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">状态</th>
                <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">新词</th>
                <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">复习</th>
                <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">总计</th>
              </tr>
            </thead>
            <tbody class="divide-y" :class="theme === 'dark' ? 'divide-slate-700' : 'divide-gray-200'">
              <tr
                v-for="(day, index) in last7Days"
                :key="index"
                class="transition-colors duration-200 hover:bg-gray-50/50"
                :class="theme === 'dark' ? 'hover:bg-slate-700/30' : ''"
              >
                <td class="px-4 py-3">
                  <div class="flex items-center gap-3">
                    <div
                      class="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-medium"
                      :class="day.hasStudy 
                        ? 'bg-gradient-to-br from-orange-400 to-red-500 text-white' 
                        : (theme === 'dark' ? 'bg-slate-700 text-slate-400' : 'bg-gray-100 text-gray-500')"
                    >
                      {{ day.dayOfMonth }}
                    </div>
                    <div>
                      <div class="text-sm font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ day.weekDay }}</div>
                      <div class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">{{ day.date }}</div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-3 text-center">
                  <div
                    v-if="day.hasStudy"
                    class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-500/20 text-emerald-400"
                  >
                    <CheckCircle2 class="w-3.5 h-3.5" />
                    已学习
                  </div>
                  <div
                    v-else
                    class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium"
                    :class="theme === 'dark' ? 'bg-slate-700 text-slate-400' : 'bg-gray-100 text-gray-500'"
                  >
                    <XCircle class="w-3.5 h-3.5" />
                    未学习
                  </div>
                </td>
                <td class="px-4 py-3 text-right">
                  <span class="text-sm font-medium" :class="day.newWords > 0 ? 'text-blue-400' : (theme === 'dark' ? 'text-slate-500' : 'text-gray-400')">
                    {{ day.newWords || 0 }}
                  </span>
                </td>
                <td class="px-4 py-3 text-right">
                  <span class="text-sm font-medium" :class="day.reviews > 0 ? 'text-purple-400' : (theme === 'dark' ? 'text-slate-500' : 'text-gray-400')">
                    {{ day.reviews || 0 }}
                  </span>
                </td>
                <td class="px-4 py-3 text-right">
                  <span 
                    class="text-sm font-bold"
                    :class="day.totalWords > 0 
                      ? (day.totalWords >= 15 ? 'text-orange-400' : 'text-emerald-400') 
                      : (theme === 'dark' ? 'text-slate-500' : 'text-gray-400')"
                  >
                    {{ day.totalWords || 0 }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Streak Rewards -->
      <div class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <Award class="w-5 h-5 text-amber-400" /> 连续学习成就
        </h3>
        
        <div class="grid grid-cols-2 gap-3">
          <div
            v-for="milestone in getStreakMilestones()"
            :key="milestone.days"
            class="p-4 rounded-xl transition-all duration-300"
            :class="milestone.unlocked
              ? (theme === 'dark' 
                  ? 'bg-gradient-to-br from-amber-500/20 to-orange-500/20 border border-amber-500/30' 
                  : 'bg-gradient-to-br from-amber-50 to-orange-50 border border-amber-200')
              : (theme === 'dark' ? 'bg-slate-700/30' : 'bg-gray-50')"
          >
            <div class="flex items-center gap-3">
              <span class="text-2xl">{{ milestone.icon }}</span>
              <div>
                <div class="text-sm font-bold" :class="milestone.unlocked ? 'text-amber-400' : (theme === 'dark' ? 'text-slate-500' : 'text-gray-400')">
                  {{ milestone.days }}天
                </div>
                <div class="text-xs" :class="milestone.unlocked ? (theme === 'dark' ? 'text-slate-300' : 'text-gray-700') : (theme === 'dark' ? 'text-slate-600' : 'text-gray-400')">
                  {{ milestone.unlocked ? milestone.title : `还差${milestone.days - (stats.streakDays || 0)}天` }}
                </div>
              </div>
            </div>
            
            <div v-if="milestone.unlocked" class="mt-3 h-1.5 rounded-full overflow-hidden bg-amber-500/30">
              <div class="h-full bg-gradient-to-r from-amber-400 to-orange-500 rounded-full" style="width: 100%"></div>
            </div>
            <div v-else class="mt-3 h-1.5 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-slate-600/50' : 'bg-gray-200'">
              <div
                class="h-full bg-gradient-to-r from-amber-400 to-orange-500 rounded-full"
                :style="{ width: `${Math.min(100, ((stats.streakDays || 0) / milestone.days) * 100)}%` }"
              ></div>
            </div>
          </div>
        </div>

        <!-- Streak Tips -->
        <div class="mt-6 p-4 rounded-xl" :class="theme === 'dark' ? 'bg-blue-500/10' : 'bg-blue-50'">
          <div class="flex items-start gap-3">
            <Lightbulb class="w-5 h-5 text-blue-400 shrink-0 mt-0.5" />
            <div>
              <div class="text-sm font-medium text-blue-600">坚持学习小贴士</div>
              <div class="text-xs mt-1" :class="theme === 'dark' ? 'text-blue-300' : 'text-blue-500'">
                {{ getStreakTip(stats.streakDays || 0) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Encouragement Card -->
    <div class="glass-card p-6 text-center">
      <div class="text-4xl mb-4">{{ getEncouragementEmoji(stats.streakDays || 0) }}</div>
      <h3 class="text-xl font-bold mb-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ getEncouragementTitle(stats.streakDays || 0) }}</h3>
      <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ getEncouragementMessage(stats.streakDays || 0) }}</p>
      
      <router-link
        to="/learn"
        class="inline-flex items-center gap-2 mt-6 px-6 py-3 rounded-xl bg-gradient-to-r from-orange-500 to-red-500 text-white font-medium transition-all duration-300 hover:shadow-lg hover:shadow-orange-500/30 hover:scale-105"
      >
        <Flame class="w-5 h-5" />
        继续学习
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useTheme } from '../composables/useTheme'
import {
  Flame,
  CalendarDays,
  CheckCircle2,
  Target,
  Sparkles,
  Calendar,
  XCircle,
  Award,
  Lightbulb
} from 'lucide-vue-next'
import { api } from '../services/api'

const { theme } = useTheme()

const stats = ref({})
const studyHistory = ref([])

const activeDays = computed(() => {
  return studyHistory.value.filter(d => d.hasStudy).length
})

const avgDailyWords = computed(() => {
  const active = studyHistory.value.filter(d => d.hasStudy)
  const total = active.reduce((acc, d) => acc + (d.totalWords || 0), 0)
  return active.length > 0 ? Math.round(total / active.length) : 0
})

function formatLocalDate(date) {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const last7Days = computed(() => {
  const days = []
  const today = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  
  for (let i = 6; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    const dateStr = formatLocalDate(date)
    
    const historyDay = studyHistory.value.find(h => h.date === dateStr)
    
    days.push({
      date: dateStr,
      dayOfMonth: date.getDate(),
      weekDay: weekDays[date.getDay()],
      hasStudy: historyDay?.hasStudy || false,
      newWords: historyDay?.newWords || 0,
      reviews: historyDay?.reviews || 0,
      totalWords: historyDay?.totalWords || 0
    })
  }
  
  return days
})

function getStreakMessage(days) {
  if (days === 0) return '开始你的学习之旅吧！'
  if (days === 1) return '不错的开始！'
  if (days < 3) return '继续保持！'
  if (days < 7) return '你正在养成好习惯！'
  if (days < 14) return '一周达成！太棒了！'
  if (days < 30) return '两周了！坚持就是胜利！'
  if (days < 50) return '一个月达成！继续加油！'
  if (days < 100) return '百日目标在向你招手！'
  return '太厉害了！你是学习达人！'
}

function getStreakMilestones() {
  const days = stats.value.streakDays || 0
  return [
    { days: 3, icon: '🌱', title: '起步', unlocked: days >= 3 },
    { days: 7, icon: '📅', title: '一周', unlocked: days >= 7 },
    { days: 14, icon: '💪', title: '两周', unlocked: days >= 14 },
    { days: 30, icon: '🔥', title: '一月', unlocked: days >= 30 },
    { days: 50, icon: '⭐', title: '五十天', unlocked: days >= 50 },
    { days: 100, icon: '🏆', title: '百日', unlocked: days >= 100 },
    { days: 365, icon: '🌟', title: '一年', unlocked: days >= 365 }
  ]
}

function getNextMilestone() {
  const milestones = getStreakMilestones()
  return milestones.find(m => !m.unlocked) || null
}

function getStreakTip(days) {
  if (days === 0) return '每天学习15分钟，坚持21天就能养成好习惯！'
  if (days < 7) return '固定一个学习时间，让大脑形成记忆。'
  if (days < 30) return '周末也不要完全停学，可以减少学习量保持节奏。'
  if (days < 100) return '尝试记录学习日记，回顾自己的进步。'
  return '你的坚持已经成为习惯，继续挑战更高目标吧！'
}

function getEncouragementEmoji(days) {
  if (days === 0) return '🎯'
  if (days < 3) return '💪'
  if (days < 7) return '🔥'
  if (days < 14) return '⚡'
  if (days < 30) return '🌟'
  if (days < 50) return '⭐'
  if (days < 100) return '🏅'
  return '👑'
}

function getEncouragementTitle(days) {
  if (days === 0) return '准备开始你的学习之旅？'
  if (days < 3) return '起步阶段'
  if (days < 7) return '初步养成'
  if (days < 14) return '渐入佳境'
  if (days < 30) return '稳步前行'
  if (days < 50) return '小有成就'
  if (days < 100) return '持续精进'
  return '学习大师'
}

function getEncouragementMessage(days) {
  if (days === 0) return '每一段伟大的旅程都从第一步开始，今天就是你改变的起点！'
  if (days < 3) return '万事开头难，但你已经迈出了第一步！继续坚持！'
  if (days < 7) return '好习惯正在养成中，坚持下去你会看到惊人的变化！'
  if (days < 14) return '两周的坚持已经让学习成为你的一部分，继续保持！'
  if (days < 30) return '一个月的坚持值得骄傲！你的努力正在转化为能力！'
  if (days < 50) return '五十天的坚持证明了你对学习的热爱，继续挑战！'
  if (days < 100) return '百日目标近在眼前，你的坚持终将收获硕果！'
  return '一年的坚持让你成为学习大师，你的毅力令人敬佩！'
}

onMounted(async () => {
  try {
    const [statsRes, studyHistoryRes, streakCalcRes] = await Promise.all([
      api.get('/api/dashboard/stats'),
      api.get('/api/dashboard/study-history', { params: { days: 90 } }).catch(() => null),
      api.get('/api/dashboard/streak-calculation').catch(() => null)
    ])
    
    stats.value = statsRes.data
    // Use streak-calculation for authoritative streak data
    if (streakCalcRes && streakCalcRes.data) {
      stats.value = {
        ...stats.value,
        streakDays: streakCalcRes.data.currentStreak,
        longestStreak: streakCalcRes.data.longestStreak,
        totalActiveDays: streakCalcRes.data.totalActiveDays
      }
    }
    if (studyHistoryRes) studyHistory.value = studyHistoryRes.data.history || []
  } catch (err) {
    console.error('Failed to load streak data:', err)
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