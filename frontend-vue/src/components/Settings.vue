<template>
  <div class="min-h-screen p-6" :class="theme === 'dark' ? 'bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900' : 'bg-gradient-to-br from-amber-50 via-white to-orange-50'">
    <div class="max-w-3xl mx-auto">
      <h1 class="text-2xl font-bold mb-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">用户设置</h1>
      <p class="mb-8" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">自定义你的学习偏好和目标</p>

      <div v-if="loading" class="text-center py-12">
        <p :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">加载中...</p>
      </div>

      <div v-else class="space-y-6">
        <!-- 外观设置 -->
        <div class="backdrop-blur rounded-2xl p-6 transition-colors duration-300"
             :class="theme === 'dark' ? 'bg-slate-800/60 border border-slate-700/50' : 'bg-white/80 border border-gray-200 shadow-lg shadow-gray-100/50'">
          <h2 class="text-lg font-semibold mb-5 flex items-center gap-2"
              :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            <Palette class="w-5 h-5 text-amber-400" />
            外观设置
          </h2>

          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">主题模式</p>
                <p class="text-xs mt-0.5" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">选择暗黑或光亮主题</p>
              </div>
              <button
                @click="toggleTheme"
                class="relative w-16 h-8 rounded-full transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-amber-500/50"
                :class="theme === 'dark' ? 'bg-amber-500' : 'bg-gray-300'"
              >
                <div
                  class="absolute top-1 w-6 h-6 rounded-full bg-white shadow-md transition-all duration-300 flex items-center justify-center"
                  :class="theme === 'dark' ? 'left-9' : 'left-1'"
                >
                  <Moon v-if="theme === 'dark'" class="w-3.5 h-3.5 text-amber-500" />
                  <Sun v-else class="w-3.5 h-3.5 text-amber-500" />
                </div>
              </button>
            </div>
            <div class="flex items-center gap-3 pt-2">
              <button
                @click="setTheme('dark')"
                class="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 border"
                :class="theme === 'dark'
                  ? 'bg-amber-500/20 text-amber-400 border-amber-500/40'
                  : 'bg-gray-100 text-gray-500 border-gray-200 hover:border-gray-300'"
              >
                <Moon class="w-4 h-4" />
                暗黑模式
              </button>
              <button
                @click="setTheme('light')"
                class="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 border"
                :class="theme === 'light'
                  ? 'bg-amber-500/20 text-amber-600 border-amber-500/40'
                  : 'bg-gray-100 text-gray-500 border-gray-200 hover:border-gray-300'"
              >
                <Sun class="w-4 h-4" />
                光亮模式
              </button>
            </div>
          </div>
        </div>

        <!-- 学习目标 -->
        <div class="backdrop-blur rounded-2xl p-6 transition-colors duration-300"
             :class="theme === 'dark' ? 'bg-slate-800/60 border border-slate-700/50' : 'bg-white/80 border border-gray-200 shadow-lg shadow-gray-100/50'">
          <h2 class="text-lg font-semibold mb-5 flex items-center gap-2"
              :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            <Target class="w-5 h-5 text-amber-400" />
            学习目标
          </h2>

          <div class="space-y-5">
            <!-- 目标考试 -->
            <div>
              <label class="block text-sm font-medium mb-2"
                     :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">目标考试</label>
              <select
                v-model="form.targetExam"
                class="w-full px-4 py-2.5 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/50 transition-all appearance-none"
                :class="theme === 'dark'
                  ? 'bg-slate-700/60 border border-slate-600/50 text-white'
                  : 'bg-gray-50 border border-gray-200 text-gray-900'"
              >
                <option value="cet4">CET-4 (大学英语四级)</option>
                <option value="cet6">CET-6 (大学英语六级)</option>
                <option value="kaoyan">考研 (Postgraduate)</option>
                <option value="ielts">IELTS (雅思)</option>
                <option value="toefl">TOEFL (托福)</option>
                <option value="gre">GRE</option>
              </select>
            </div>

            <!-- 目标词汇量 -->
            <div>
              <label class="block text-sm font-medium mb-2"
                     :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">目标词汇量</label>
              <input
                v-model.number="form.targetVocabSize"
                type="range"
                min="1000"
                max="15000"
                step="500"
                class="w-full accent-amber-500"
              />
              <div class="flex justify-between text-xs mt-1">
                <span :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">1,000</span>
                <span class="text-amber-500 font-semibold">{{ form.targetVocabSize.toLocaleString() }} 词</span>
                <span :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">15,000</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 学习强度 -->
        <div class="backdrop-blur rounded-2xl p-6 transition-colors duration-300"
             :class="theme === 'dark' ? 'bg-slate-800/60 border border-slate-700/50' : 'bg-white/80 border border-gray-200 shadow-lg shadow-gray-100/50'">
          <h2 class="text-lg font-semibold mb-5 flex items-center gap-2"
              :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            <Zap class="w-5 h-5 text-amber-400" />
            学习强度
          </h2>

          <div class="space-y-5">
            <!-- 每日新词数 -->
            <div>
              <label class="block text-sm font-medium mb-2"
                     :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">每日新词数量</label>
              <input
                v-model.number="form.dailyNewWords"
                type="range"
                min="5"
                max="50"
                step="1"
                class="w-full accent-amber-500"
              />
              <div class="flex justify-between text-xs mt-1">
                <span :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">5</span>
                <span class="text-amber-500 font-semibold">{{ form.dailyNewWords }} 个/天</span>
                <span :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">50</span>
              </div>
            </div>

            <!-- 难度等级 -->
            <div>
              <label class="block text-sm font-medium mb-2"
                     :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">难度等级</label>
              <div class="flex gap-2">
                <button
                  v-for="level in 5"
                  :key="level"
                  @click="form.difficultyLevel = level"
                  class="flex-1 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200"
                  :class="form.difficultyLevel === level
                    ? (theme === 'dark'
                        ? 'bg-amber-600/30 text-amber-400 border border-amber-500/40 shadow-lg shadow-amber-500/10'
                        : 'bg-amber-100 text-amber-700 border border-amber-300 shadow-lg shadow-amber-200/30')
                    : (theme === 'dark'
                        ? 'bg-slate-700/40 text-slate-400 border border-slate-600/30 hover:border-slate-500/50'
                        : 'bg-gray-100 text-gray-500 border border-gray-200 hover:border-gray-300')"
                >
                  {{ level === 1 ? '入门' : level === 2 ? '基础' : level === 3 ? '中级' : level === 4 ? '高级' : '专家' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 账户信息 -->
        <div class="backdrop-blur rounded-2xl p-6 transition-colors duration-300"
             :class="theme === 'dark' ? 'bg-slate-800/60 border border-slate-700/50' : 'bg-white/80 border border-gray-200 shadow-lg shadow-gray-100/50'">
          <h2 class="text-lg font-semibold mb-5 flex items-center gap-2"
              :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            <User class="w-5 h-5 text-emerald-400" />
            账户信息
          </h2>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-xs mb-1" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">用户名</label>
              <p class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ user?.username }}</p>
            </div>
            <div>
              <label class="block text-xs mb-1" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">邮箱</label>
              <p class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ user?.email }}</p>
            </div>
            <div>
              <label class="block text-xs mb-1" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">记忆强度</label>
              <p class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ (user?.memoryStrength || 1.0).toFixed(1) }}</p>
            </div>
            <div>
              <label class="block text-xs mb-1" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">平均回忆率</label>
              <p class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ ((user?.avgRecallRate || 0) * 100).toFixed(0) }}%</p>
            </div>
          </div>
        </div>

        <!-- 危险操作 -->
        <div class="backdrop-blur rounded-2xl p-6 transition-colors duration-300"
             :class="theme === 'dark' ? 'bg-red-900/20 border border-red-800/30' : 'bg-red-50/80 border border-red-200'">
          <h2 class="text-lg font-semibold mb-5 flex items-center gap-2"
              :class="theme === 'dark' ? 'text-red-400' : 'text-red-600'">
            <AlertTriangle class="w-5 h-5" />
            危险操作
          </h2>

          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">重置学习数据并重新选择词库</p>
              <p class="text-xs mt-0.5" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-500'">清空所有学习记录、复习进度和连续学习天数，然后可以选择新的词库</p>
            </div>
            <button
              @click="confirmResetProgress"
              :disabled="saving"
              class="px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 text-white bg-red-500 hover:bg-red-600 disabled:opacity-50"
            >
              {{ resetting ? '重置中...' : '重置学习数据' }}
            </button>
          </div>
        </div>

        <!-- 保存按钮 -->
        <div class="flex justify-end gap-3 pt-2">
          <button
            @click="resetForm"
            class="px-6 py-2.5 rounded-xl text-sm font-medium transition-all duration-200"
            :class="theme === 'dark'
              ? 'text-slate-400 hover:text-white border border-slate-600/50 hover:border-slate-500/50'
              : 'text-gray-500 hover:text-gray-800 border border-gray-300 hover:border-gray-400'"
            :disabled="saving"
          >
            重置
          </button>
          <button
            @click="saveSettings"
            class="px-6 py-2.5 rounded-xl text-sm font-medium text-white bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 shadow-lg shadow-amber-500/25 transition-all duration-200 disabled:opacity-50"
            :disabled="saving"
          >
            <span v-if="saving">保存中...</span>
            <span v-else>保存设置</span>
          </button>
        </div>

        <!-- Toast message -->
        <div
          v-if="message"
          class="fixed top-6 right-6 px-4 py-3 rounded-xl text-sm font-medium shadow-xl transition-all duration-300 z-50"
          :class="messageType === 'success'
            ? 'bg-emerald-600/90 text-white border border-emerald-500/30'
            : 'bg-red-600/90 text-white border border-red-500/30'"
        >
          {{ message }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useTheme } from '../composables/useTheme'
import api from '../services/api'
import { Target, Zap, User, Palette, Moon, Sun, AlertTriangle } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const { theme, toggleTheme, setTheme } = useTheme()

const loading = ref(true)
const saving = ref(false)
const resetting = ref(false)
const message = ref('')
const messageType = ref('success')

const user = computed(() => userStore.user)

const form = ref({
  targetExam: 'cet6',
  targetVocabSize: 6000,
  dailyNewWords: 20,
  difficultyLevel: 3
})

function resetForm() {
  if (user.value) {
    form.value.targetExam = user.value.targetExam || 'cet6'
    form.value.targetVocabSize = user.value.targetVocabSize || 6000
    form.value.dailyNewWords = user.value.dailyNewWords || 20
    form.value.difficultyLevel = user.value.difficultyLevel || 3
  }
}

function showToast(msg, type = 'success') {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

async function confirmResetProgress() {
  if (!confirm('确定要清空所有学习数据吗？\n这将删除所有学习记录、复习进度、连续学习天数，并让你重新选择词库。\n此操作不可撤销！')) return

  resetting.value = true
  try {
    const res = await api.post('/api/learning/reset-progress', null, {
      params: { fullReset: true, includeMastered: true }
    })

    if (userStore.user) {
      userStore.setUser({
        ...userStore.user,
        selectedLibrary: null,
        streakDays: 0,
        avgRecallRate: 0
      })
    }

    showToast(res.data.message || '学习数据已清空')
    // 跳转到词库选择页面
    setTimeout(() => {
      router.push('/library')
    }, 1500)
  } catch (err) {
    console.error('Failed to reset progress:', err)
    showToast('重置失败，请重试', 'error')
  } finally {
    resetting.value = false
  }
}

async function saveSettings() {
  saving.value = true
  try {
    const res = await api.put('/api/auth/me/settings', {
      targetExam: form.value.targetExam,
      targetVocabSize: form.value.targetVocabSize,
      dailyNewWords: form.value.dailyNewWords,
      difficultyLevel: form.value.difficultyLevel
    })
    userStore.setUser(res.data)
    showToast('设置已保存')
  } catch (err) {
    console.error('Failed to save settings:', err)
    showToast('保存失败，请重试', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (user.value) {
    resetForm()
  }
  loading.value = false
})
</script>
