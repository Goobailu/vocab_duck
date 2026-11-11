<template>
  <div class="p-8 animate-fadeIn">
    <div class="mb-8">
      <h2 class="text-2xl font-bold flex items-center gap-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
        <Calendar class="w-6 h-6 text-purple-500" /> 复习计划
      </h2>
      <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">查看和管理你的复习计划</p>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center mb-3">
          <Clock class="w-5 h-5 text-red-400" />
        </div>
        <p class="text-xl font-bold text-red-400">{{ stats.wordsDueReview || 0 }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">待复习</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-orange-500/20 flex items-center justify-center mb-3">
          <AlertCircle class="w-5 h-5 text-orange-400" />
        </div>
        <p class="text-xl font-bold text-orange-400">{{ schedule.overdueCount || 0 }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">已逾期</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-amber-500/20 flex items-center justify-center mb-3">
          <CalendarDays class="w-5 h-5 text-amber-400" />
        </div>
        <p class="text-xl font-bold text-amber-400">{{ weeklyPlan.filter(d => d.hasTasks).length }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">本周任务天数</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-blue-500/20 flex items-center justify-center mb-3">
          <Target class="w-5 h-5 text-blue-400" />
        </div>
        <p class="text-xl font-bold text-blue-400">{{ weeklyProgress }}%</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">本周进度</p>
      </div>
    </div>

    <!-- Action Buttons -->
    <div class="flex flex-wrap gap-4 mb-8">
      <router-link
        to="/review"
        class="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-medium transition-all duration-300 hover:shadow-lg hover:shadow-amber-500/30 hover:scale-105"
      >
        <RotateCcw class="w-5 h-5" />
        开始复习
      </router-link>
      <button
        @click="loadData"
        class="inline-flex items-center gap-2 px-6 py-3 rounded-xl border transition-all duration-300"
        :class="theme === 'dark'
          ? 'border-slate-600 text-slate-300 hover:bg-slate-700/50'
          : 'border-gray-200 text-gray-600 hover:bg-gray-50'"
      >
        <RefreshCw class="w-5 h-5" />
        刷新计划
      </button>
    </div>

    <!-- Weekly Review Plan -->
    <div class="glass-card p-6 mb-8">
      <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
          :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
        <CalendarDays class="w-5 h-5 text-amber-400" /> 本周复习计划
      </h3>
      
      <div class="grid grid-cols-7 gap-2">
        <div
          v-for="(day, index) in weeklyPlan"
          :key="index"
          class="flex flex-col items-center p-3 rounded-xl transition-all duration-200 cursor-pointer"
          :class="[
            isToday(day.date)
              ? (theme === 'dark' ? 'bg-amber-500/20 border border-amber-500/50' : 'bg-amber-50 border border-amber-300')
              : (theme === 'dark' ? 'hover:bg-slate-700/30' : 'hover:bg-gray-50'),
            day.hasTasks ? 'shadow-md' : ''
          ]"
          @click="day.hasTasks && router.push('/review')"
        >
          <span class="text-xs font-medium" :class="day.hasTasks ? (theme === 'dark' ? 'text-amber-400' : 'text-amber-600') : (theme === 'dark' ? 'text-slate-500' : 'text-gray-500')">
            {{ day.weekday }}
          </span>
          <span class="text-lg font-bold mt-1" :class="day.hasTasks ? (theme === 'dark' ? 'text-white' : 'text-gray-900') : (theme === 'dark' ? 'text-slate-600' : 'text-gray-400')">
            {{ day.day }}
          </span>
          
          <div v-if="day.hasTasks" class="mt-2 w-full">
            <div class="relative h-16 w-full rounded-lg overflow-hidden" :class="theme === 'dark' ? 'bg-slate-700/50' : 'bg-gray-100'">
              <div
                class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-amber-400 to-orange-500 rounded-lg transition-all duration-500"
                :style="{ height: `${Math.min(100, (day.dueCount / maxDailyCount * 100))}%` }"
              ></div>
              <div class="absolute inset-0 flex items-end justify-center pb-1">
                <span class="text-xs font-bold text-white drop-shadow-md">
                  {{ day.dueCount }}
                </span>
              </div>
            </div>
          </div>
          
          <div v-else class="mt-2 w-full h-16 flex items-center justify-center">
            <span class="text-xs" :class="theme === 'dark' ? 'text-slate-600' : 'text-gray-300'">—</span>
          </div>
          
          <span v-if="isToday(day.date)" class="mt-1 text-xs px-2 py-0.5 rounded-full bg-amber-500 text-white">
            今天
          </span>
        </div>
      </div>
      

    </div>

      <!-- Today's Plan -->
      <div class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <Clock class="w-5 h-5 text-blue-400" /> 今日计划
        </h3>
        
        <div v-if="todayPlan.length > 0" class="space-y-3">
          <div
            v-for="item in todayPlan"
            :key="item.id"
            class="flex items-center gap-4 p-4 rounded-xl transition-all duration-200 cursor-pointer"
            :class="item.completed ? (theme === 'dark' ? 'bg-green-500/10' : 'bg-green-50') : (theme === 'dark' ? 'hover:bg-slate-700/30' : 'hover:bg-gray-50')"
            @click="handleTaskClick(item)"
          >
            <div
              class="w-6 h-6 rounded-full flex items-center justify-center shrink-0 border-2"
              :class="item.completed ? 'bg-green-500 border-green-500' : (theme === 'dark' ? 'border-slate-500' : 'border-gray-300')"
            >
              <CheckCircle2 v-if="item.completed" class="w-4 h-4 text-white" />
            </div>
            
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <span
                  v-if="item.priority"
                  class="text-xs px-1.5 py-0.5 rounded"
                  :class="{
                    'bg-red-500/20 text-red-400': item.priority === 'high',
                    'bg-amber-500/20 text-amber-400': item.priority === 'medium',
                    'bg-blue-500/20 text-blue-400': item.priority === 'low'
                  }"
                >
                  {{ item.priority === 'high' ? '优先' : item.priority === 'medium' ? '普通' : '次要' }}
                </span>
                <div class="text-sm font-medium" :class="item.completed ? (theme === 'dark' ? 'text-green-400' : 'text-green-600') : (theme === 'dark' ? 'text-white' : 'text-gray-900')">
                  {{ item.title }}
                </div>
              </div>
              <div class="flex items-center gap-3 mt-1">
                <span class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">
                  {{ item.description }}
                </span>
                <span v-if="item.wordCount" class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">
                  · {{ item.wordCount }} 个单词
                </span>
                <span v-if="item.durationMinutes" class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">
                  · {{ item.durationMinutes }} 分钟
                </span>
              </div>
              <div v-if="item.targetCount && !item.completed" class="mt-2 h-1.5 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'">
                <div
                  class="h-full rounded-full"
                  :class="item.type === 'new' ? 'bg-blue-400' : 'bg-orange-400'"
                  :style="{ width: `${item.progress || 0}%` }"
                ></div>
              </div>
            </div>
            
            <span
              class="text-xs px-2 py-1 rounded-full"
              :class="item.type === 'new' ? 'bg-blue-500/20 text-blue-400' : 'bg-orange-500/20 text-orange-400'"
            >
              {{ item.type === 'new' ? '新词' : '复习' }}
            </span>
            
            <ChevronRight class="w-4 h-4" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'" />
          </div>
        </div>
        
        <div v-else class="text-center py-12">
          <Calendar class="w-16 h-16 mx-auto mb-4 opacity-50" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'" />
          <p class="text-base font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">暂无学习计划</p>
          <p class="text-sm mt-2" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">开始学习，创建你的今日计划吧！</p>
        </div>
      </div>

    <!-- Urgent Words -->
    <div class="glass-card p-6">
      <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
          :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
        <AlertCircle class="w-5 h-5 text-red-400" /> 需要紧急复习的单词
      </h3>
      
      <div v-if="urgentWords.length > 0" class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr :class="theme === 'dark' ? 'bg-slate-700/50' : 'bg-gray-50'">
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">单词</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">释义</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">记忆强度</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">上次复习</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y" :class="theme === 'dark' ? 'divide-slate-700' : 'divide-gray-200'">
            <tr
              v-for="word in urgentWords"
              :key="word.id"
              class="transition-colors duration-200 hover:bg-gray-50/50"
              :class="theme === 'dark' ? 'hover:bg-slate-700/30' : ''"
            >
              <td class="px-4 py-3">
                <span class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ word.word }}</span>
              </td>
              <td class="px-4 py-3">
                <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-600'">{{ word.meaning }}</span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="inline-flex items-center gap-2">
                  <div class="w-16 h-2 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'">
                    <div
                      class="h-full rounded-full"
                      :class="word.retrievability < 0.3 ? 'bg-red-500' : (word.retrievability < 0.6 ? 'bg-amber-500' : 'bg-green-500')"
                      :style="{ width: `${word.retrievability * 100}%` }"
                    ></div>
                  </div>
                  <span class="text-xs font-medium" :class="word.retrievability < 0.3 ? 'text-red-400' : (word.retrievability < 0.6 ? 'text-amber-400' : 'text-green-400')">
                    {{ (word.retrievability * 100).toFixed(0) }}%
                  </span>
                </div>
              </td>
              <td class="px-4 py-3 text-center">
                <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ formatDate(word.lastReviewed) }}</span>
              </td>
              <td class="px-4 py-3 text-center">
                <router-link
                  to="/review"
                  class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium transition-all duration-200"
                  :class="theme === 'dark' ? 'bg-amber-500/20 text-amber-400 hover:bg-amber-500/30' : 'bg-amber-100 text-amber-600 hover:bg-amber-200'"
                >
                  <RotateCcw class="w-3.5 h-3.5" />
                  复习
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div v-else class="text-center py-8">
        <CheckCircle2 class="w-10 h-10 mx-auto mb-3 text-green-400" />
        <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">太棒了！没有需要紧急复习的单词</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTheme } from '../composables/useTheme'
import {
  Calendar,
  CalendarDays,
  Clock,
  Target,
  RefreshCw,
  ChevronRight,
  CheckCircle2,
  AlertCircle,
  RotateCcw
} from 'lucide-vue-next'
import { api } from '../services/api'

const router = useRouter()
const { theme } = useTheme()

const stats = ref({})
const schedule = ref({})
const urgentWords = ref([])
const todayPlan = ref([])

const weeklyPlan = computed(() => {
  const plan = []
  const today = new Date()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  
  for (let i = 0; i < 7; i++) {
    const date = new Date(today)
    date.setDate(date.getDate() + i)
    const dateStr = formatDateStr(date)
    
    const scheduleList = schedule.value.schedule || []
    const daySchedule = scheduleList.find(s => s.date === dateStr)
    
    plan.push({
      date: dateStr,
      day: date.getDate(),
      weekday: weekdays[date.getDay()],
      hasTasks: daySchedule && daySchedule.dueCount > 0,
      dueCount: daySchedule?.dueCount || 0,
      completedCount: daySchedule?.completedCount || 0,
      isToday: daySchedule?.isToday || (i === 0),
      cycleCounts: daySchedule?.cycleCounts || {}
    })
  }
  
  return plan
})

const completedToday = computed(() => {
  const todayStr = formatDateStr(new Date())
  const scheduleList = schedule.value.schedule || []
  const todaySchedule = scheduleList.find(s => s.date === todayStr)
  return todaySchedule?.completedCount || 0
})

const weeklyProgress = computed(() => {
  const totalDue = weeklyPlan.value.reduce((acc, day) => acc + day.dueCount, 0)
  const completed = weeklyPlan.value.reduce((acc, day) => acc + day.completedCount, 0)
  return totalDue > 0 ? Math.round((completed / totalDue) * 100) : 0
})

const maxDailyCount = computed(() => {
  const counts = weeklyPlan.value.map(day => day.dueCount)
  return counts.length > 0 ? Math.max(...counts, 1) : 1
})



function formatDateStr(date) {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function getCycleLabel(cycle) {
  const cycleNum = parseInt(cycle)
  if (cycleNum === 1) return '第1次'
  if (cycleNum === 2) return '第2次'
  if (cycleNum === 3) return '第3次'
  if (cycleNum >= 4) return `${cycleNum}周后`
  return `第${cycleNum}次`
}

function isToday(dateStr) {
  return dateStr === formatDateStr(new Date())
}

function formatDate(date) {
  if (!date) return '-'
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function handleTaskClick(item) {
  if (item.type === 'new') {
    router.push('/learning')
  } else {
    router.push('/review')
  }
}

async function loadData() {
  try {
    const [statsRes, scheduleRes, urgentWordsRes, dailyPlanRes] = await Promise.all([
      api.get('/api/dashboard/stats'),
      api.get('/api/dashboard/schedule'),
      api.get('/api/dashboard/urgent-words').catch(() => null),
      api.get('/api/dashboard/daily-plan').catch(() => null)
    ])
    
    stats.value = statsRes.data
    schedule.value = scheduleRes.data
    
    // Fix: backend returns urgentWords array, not words
    if (urgentWordsRes && urgentWordsRes.data) {
      const urgentData = urgentWordsRes.data
      urgentWords.value = (urgentData.urgentWords || urgentData.words || []).map(w => ({
        id: w.userWordId || w.wordId,
        word: w.word || '',
        meaning: w.definitionCn || '',
        retrievability: w.masteryLevel || 0,
        lastReviewed: w.lastReviewed || w.nextReview || null,
        urgencyLevel: w.urgencyLevel || 'medium',
        urgencyScore: w.urgencyScore || 0
      }))
    } else {
      urgentWords.value = []
    }
    
    // Fix: properly map daily plan tasks with meaningful display
    if (dailyPlanRes && dailyPlanRes.data) {
      const planData = dailyPlanRes.data
      const tasks = planData.tasks || []
      todayPlan.value = tasks.map((task, index) => {
        const taskType = task.type || 'review'
        let title = task.taskName || ''
        let description = task.description || ''
        
        // Generate meaningful titles if missing
        if (!title) {
          switch (taskType) {
            case 'review': title = '复习已过期单词'; break
            case 'urgent_review': title = '紧急单词强化'; break
            case 'new_words': title = '学习新单词'; break
            case 'weak_review': title = '薄弱单词巩固'; break
            default: title = '复习单词'
          }
        }
        
        if (!description && task.durationMinutes) {
          description = `预计耗时 ${task.durationMinutes} 分钟`
          if (task.targetCount) description += `，目标 ${task.targetCount} 个单词`
        }
        
        // Determine if task is completed based on today's progress
        let isCompleted = false
        let currentProgress = 0
        let target = 0
        
        if (taskType === 'new_words') {
          target = task.wordCount || task.targetCount || 0
          currentProgress = stats.value.todayNewWords || 0
          isCompleted = currentProgress >= target && target > 0
        } else if (taskType === 'weak_review') {
          target = task.targetCount || 0
          currentProgress = stats.value.todayReviews || 0
          isCompleted = currentProgress >= target && target > 0
        } else if (taskType === 'review' || taskType === 'urgent_review') {
          target = task.targetCount || 0
          currentProgress = stats.value.todayReviews || 0
          isCompleted = currentProgress >= target && target > 0
        }
        
        const progress = target ? Math.min(100, Math.round((currentProgress / target) * 100)) : 0
        
        return {
          id: index,
          title,
          description,
          type: taskType === 'new_words' ? 'new' : 'review',
          completed: isCompleted,
          durationMinutes: task.durationMinutes || 0,
          priority: task.priority || 'medium',
          wordCount: task.wordCount || 0,
          targetCount: task.targetCount || 0,
          progress
        }
      })
    } else {
      todayPlan.value = []
    }
  } catch (err) {
    console.error('Failed to load review plan:', err)
  }
}

onMounted(() => {
  loadData()
})
</script>