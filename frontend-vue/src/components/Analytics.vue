<template>
  <div class="p-8 animate-fadeIn">
    <div class="mb-8">
      <h2 class="text-2xl font-bold flex items-center gap-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
        <BarChart3 class="w-6 h-6 text-purple-400" /> 数据分析
      </h2>
      <p class="text-sm mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">基于记忆诊断智能体的深度分析报告</p>
    </div>

    <!-- Key Metrics -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-amber-500/20 flex items-center justify-center mb-3">
          <Brain class="w-5 h-5 text-amber-500" />
        </div>
        <p class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ diagnosis?.memoryStrength?.toFixed(2) || '1.00' }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">记忆强度</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-orange-500/20 flex items-center justify-center mb-3">
          <Activity class="w-5 h-5 text-orange-400" />
        </div>
        <p class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ diagnosis?.forgettingRate?.toFixed(3) || '0.333' }}</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">遗忘衰减率</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-purple-500/20 flex items-center justify-center mb-3">
          <Calendar class="w-5 h-5 text-purple-400" />
        </div>
        <p class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ diagnosis?.halfLifeDays?.toFixed(1) || '2.1' }} 天</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">记忆半衰期</p>
      </div>
      <div class="stat-card">
        <div class="w-10 h-10 rounded-xl bg-emerald-500/20 flex items-center justify-center mb-3">
          <Zap class="w-5 h-5 text-emerald-400" />
        </div>
        <p class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ diagnosis?.optimalDailyWords || 20 }} 词</p>
        <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">推荐日学量</p>
      </div>
    </div>

    <!-- Charts Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <!-- Forgetting Curve -->
      <div class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <TrendingUp class="w-5 h-5 text-amber-500" /> 遗忘曲线（SRS指数衰减模型）
        </h3>
        <div v-if="curveChartData.labels.length > 0" class="h-[250px]">
          <Line :data="curveChartData" :options="chartOptions" />
        </div>
        <p v-else class="text-center py-16" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">学习数据不足，需要更多复习记录</p>
      </div>

      <!-- Daily Progress -->
      <div class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <Calendar class="w-5 h-5 text-emerald-400" /> 每日学习量（近30天）
        </h3>
        <div v-if="progressChartData.labels.length > 0" class="h-[250px]">
          <Bar :data="progressChartData" :options="chartOptions" />
        </div>
        <p v-else class="text-center py-16" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">暂无学习记录</p>
      </div>

      <!-- Mastery by Difficulty -->
      <div class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <Target class="w-5 h-5 text-purple-400" /> 掌握度分布（按难度）
        </h3>
        <div v-if="masteryChartData.labels.length > 0" class="h-[250px]">
          <Bar :data="masteryChartData" :options="horizontalOptions" />
        </div>
        <p v-else class="text-center py-16" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">暂无数据</p>
      </div>

      <!-- Mastery Band Doughnut -->
      <div class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <Brain class="w-5 h-5 text-cyan-400" /> 掌握度分段分布
        </h3>
        <div v-if="bandChartData.labels.length > 0" class="h-[250px] flex items-center justify-center">
          <Doughnut :data="bandChartData" :options="doughnutOptions" />
        </div>
        <p v-else class="text-center py-16" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">暂无数据</p>
      </div>
    </div>

    <!-- Enhanced Diagnosis Features -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <!-- Predictive Analysis -->
      <div v-if="diagnosis?.predictiveAnalysis" class="glass-card p-6">
        <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
            :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
          <TrendingUp class="w-5 h-5 text-purple-400" /> 预测分析
        </h3>
        <div class="grid grid-cols-2 gap-3 mb-4">
          <div class="p-3 rounded-xl" :class="theme === 'dark' ? 'bg-slate-800/50' : 'bg-gray-50'">
            <p class="text-lg font-bold" :class="getRetentionColor(diagnosis.predictiveAnalysis.expectedRetention)">
              {{ (diagnosis.predictiveAnalysis.expectedRetention * 100).toFixed(0) }}%
            </p>
            <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">7日预测留存率</p>
          </div>
          <div class="p-3 rounded-xl" :class="theme === 'dark' ? 'bg-slate-800/50' : 'bg-gray-50'">
            <p class="text-lg font-bold" :class="theme === 'dark' ? 'text-orange-400' : 'text-orange-600'">
              {{ diagnosis.predictiveAnalysis.riskWordsCount || 0 }}
            </p>
            <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">高遗忘风险单词</p>
          </div>
        </div>
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">预计全部掌握天数</span>
            <span class="text-lg font-semibold" :class="theme === 'dark' ? 'text-emerald-400' : 'text-emerald-600'">
              {{ diagnosis.predictiveAnalysis.estimatedMasteryDays || '-' }} 天
            </span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">当前记忆强度</span>
            <span class="text-lg font-semibold" :class="theme === 'dark' ? 'text-amber-400' : 'text-amber-600'">
              {{ diagnosis.predictiveAnalysis.memoryStrength || '-' }}
            </span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">遗忘衰减率</span>
            <span class="text-lg font-semibold" :class="theme === 'dark' ? 'text-red-400' : 'text-red-600'">
              {{ diagnosis.predictiveAnalysis.forgettingRate ? (diagnosis.predictiveAnalysis.forgettingRate * 100).toFixed(2) : '-' }}%/天
            </span>
          </div>
          <div v-if="diagnosis.predictiveAnalysis.totalNonMasteredWords" class="flex items-center justify-between">
            <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">未掌握单词</span>
            <span class="text-lg font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
              {{ diagnosis.predictiveAnalysis.totalNonMasteredWords }}
            </span>
          </div>
        </div>
        <div v-if="diagnosis.predictiveAnalysis.reviewNeedByPeriod" class="mt-4 pt-4 border-t" :class="theme === 'dark' ? 'border-slate-700' : 'border-gray-200'">
          <p class="text-xs font-medium mb-2" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">复习需求分布</p>
          <div class="space-y-2">
            <div class="flex items-center gap-3">
              <div class="flex-1 h-2 rounded-full bg-red-500/20 overflow-hidden">
                <div class="h-full rounded-full bg-red-500" :style="{ width: `${diagnosis.predictiveAnalysis.reviewNeedByPeriod.urgent_0_3_days ? Math.min(diagnosis.predictiveAnalysis.reviewNeedByPeriod.urgent_0_3_days * 5, 100) : 0}%` }"></div>
              </div>
              <span class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">0-3天: {{ diagnosis.predictiveAnalysis.reviewNeedByPeriod.urgent_0_3_days || 0 }}</span>
            </div>
            <div class="flex items-center gap-3">
              <div class="flex-1 h-2 rounded-full bg-amber-500/20 overflow-hidden">
                <div class="h-full rounded-full bg-amber-500" :style="{ width: `${diagnosis.predictiveAnalysis.reviewNeedByPeriod.soon_3_7_days ? Math.min(diagnosis.predictiveAnalysis.reviewNeedByPeriod.soon_3_7_days * 5, 100) : 0}%` }"></div>
              </div>
              <span class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">3-7天: {{ diagnosis.predictiveAnalysis.reviewNeedByPeriod.soon_3_7_days || 0 }}</span>
            </div>
            <div class="flex items-center gap-3">
              <div class="flex-1 h-2 rounded-full bg-blue-500/20 overflow-hidden">
                <div class="h-full rounded-full bg-blue-500" :style="{ width: `${diagnosis.predictiveAnalysis.reviewNeedByPeriod.upcoming_7_14_days ? Math.min(diagnosis.predictiveAnalysis.reviewNeedByPeriod.upcoming_7_14_days * 5, 100) : 0}%` }"></div>
              </div>
              <span class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">7-14天: {{ diagnosis.predictiveAnalysis.reviewNeedByPeriod.upcoming_7_14_days || 0 }}</span>
            </div>
          </div>
        </div>
        <div v-if="diagnosis.predictiveAnalysis.suggestion" class="mt-4 p-3 rounded-xl"
             :class="theme === 'dark' ? 'bg-purple-500/10 border border-purple-500/30' : 'bg-purple-50 border border-purple-200'">
          <p class="text-sm" :class="theme === 'dark' ? 'text-purple-100' : 'text-purple-700'">{{ diagnosis.predictiveAnalysis.suggestion }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useTheme } from '../composables/useTheme'
import { BarChart3, TrendingUp, Brain, Target, Activity, Calendar, Zap, Clock, Check } from 'lucide-vue-next'
import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement,
  BarElement, ArcElement, Title, Tooltip, Legend
} from 'chart.js'
import { Line, Bar, Doughnut } from 'vue-chartjs'
import { api } from '../services/api'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Title, Tooltip, Legend)

const { theme } = useTheme()
const diagnosis = ref(null)
const progress = ref(null)
const mastery = ref(null)

const isDark = computed(() => theme.value === 'dark')

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  scales: {
    x: {
      grid: { color: isDark.value ? '#334155' : '#e2e8f0' },
      ticks: { color: isDark.value ? '#64748b' : '#94a3b8', font: { size: 11 } }
    },
    y: {
      grid: { color: isDark.value ? '#334155' : '#e2e8f0' },
      ticks: { color: isDark.value ? '#64748b' : '#94a3b8', font: { size: 11 } }
    },
  },
  plugins: {
    tooltip: {
      backgroundColor: isDark.value ? '#1e293b' : '#ffffff',
      borderColor: isDark.value ? '#334155' : '#e2e8f0',
      borderWidth: 1,
      titleColor: isDark.value ? '#f1f5f9' : '#0f172a',
      bodyColor: isDark.value ? '#cbd5e1' : '#475569'
    },
    legend: { labels: { color: isDark.value ? '#94a3b8' : '#64748b' } },
  },
}))

const horizontalOptions = computed(() => ({
  ...chartOptions.value,
  indexAxis: 'y',
}))

const doughnutOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    tooltip: {
      backgroundColor: isDark.value ? '#1e293b' : '#ffffff',
      borderColor: isDark.value ? '#334155' : '#e2e8f0',
      borderWidth: 1,
      titleColor: isDark.value ? '#f1f5f9' : '#0f172a',
      bodyColor: isDark.value ? '#cbd5e1' : '#475569'
    },
    legend: { position: 'bottom', labels: { color: isDark.value ? '#94a3b8' : '#64748b', padding: 12, font: { size: 11 } } },
  },
}))

const curveChartData = computed(() => {
  const points = diagnosis.value?.curvePoints || []
  return {
    labels: points.map(p => `${p.daysSinceReview}d`),
    datasets: [
      {
        label: '预测保持率 (R=e^(-d*t))',
        data: points.map(p => (p.predictedRetention * 100).toFixed(1)),
        borderColor: '#f59e0b',
        backgroundColor: 'rgba(245,158,11,0.05)',
        borderWidth: 2,
        pointRadius: 5,
        pointBackgroundColor: '#f59e0b',
        fill: true,
        tension: 0.4,
      },
      {
        label: '实际保持率',
        data: points.map(p => p.actualRetention ? (p.actualRetention * 100).toFixed(1) : null),
        borderColor: '#22c55e',
        backgroundColor: '#22c55e',
        borderWidth: 2,
        borderDash: [5, 5],
        pointRadius: 6,
        pointBackgroundColor: '#22c55e',
        fill: false,
        spanGaps: false,
        tension: 0.3,
      },
    ],
  }
})

const progressChartData = computed(() => {
  const daily = progress.value?.dailyProgress || []
  return {
    labels: daily.map(d => d.date?.slice(5) || ''),
    datasets: [
      { label: '新词', data: daily.map(d => d.newWords), backgroundColor: '#f59e0b' },
      { label: '复习', data: daily.map(d => d.reviews), backgroundColor: '#22c55e' },
    ],
  }
})

const masteryChartData = computed(() => {
  const dist = mastery.value?.distribution || []
  return {
    labels: dist.map(d => `难度${d.difficulty}`),
    datasets: [
      { 
        label: '总数', 
        data: dist.map(d => d.total), 
        backgroundColor: isDark.value ? '#475569' : '#cbd5e1',
        borderRadius: 4,
      },
      { 
        label: '已掌握', 
        data: dist.map(d => d.mastered), 
        backgroundColor: '#22c55e',
        borderRadius: 4,
      },
    ],
  }
})

const COLORS = ['#f59e0b', '#22c55e', '#3b82f6', '#ef4444', '#8b5cf6']
const bandChartData = computed(() => {
  const band = diagnosis.value?.masteryDistribution?.masteryBands
  if (!band) return { labels: [], datasets: [] }
  const entries = Object.entries(band)
  return {
    labels: entries.map(([name]) => name.split(' ')[0]),
    datasets: [{
      data: entries.map(([, v]) => v),
      backgroundColor: entries.map((_, i) => COLORS[i % COLORS.length]),
      borderWidth: 0,
    }],
  }
})

onMounted(async () => {
  const [diagRes, progRes, mastRes] = await Promise.all([
    api.get('/api/dashboard/diagnosis').catch(() => null),
    api.get('/api/dashboard/progress?days=30').catch(() => null),
    api.get('/api/dashboard/mastery-distribution').catch(() => null),
  ])
  if (diagRes) diagnosis.value = diagRes.data
  if (progRes) progress.value = progRes.data
  if (mastRes) mastery.value = mastRes.data
})

function getEfficiencyColor(grade) {
  const colors = {
    'A+': 'text-emerald-400',
    'A': 'text-emerald-400',
    'B': 'text-amber-400',
    'C': 'text-orange-400',
    'D': 'text-red-400',
  }
  return colors[grade] || 'text-slate-400'
}

function getRetentionColor(retention) {
  if (retention >= 0.8) return 'text-emerald-400'
  if (retention >= 0.6) return 'text-amber-400'
  return 'text-orange-400'
}
</script>
