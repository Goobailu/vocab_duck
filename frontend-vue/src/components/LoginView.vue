<template>
  <div class="min-h-screen flex items-center justify-center relative overflow-hidden px-4"
       :class="theme === 'dark' ? 'bg-slate-900' : 'bg-gradient-to-br from-amber-50 via-white to-orange-50'">
    <!-- Decorative blur circles -->
    <div class="absolute top-[-120px] left-[-80px] w-72 h-72 rounded-full blur-3xl pointer-events-none"
         :class="theme === 'dark' ? 'bg-amber-600/20' : 'bg-amber-300/30'"></div>
    <div class="absolute bottom-[-100px] right-[-60px] w-96 h-96 rounded-full blur-3xl pointer-events-none"
         :class="theme === 'dark' ? 'bg-orange-600/15' : 'bg-orange-200/30'"></div>
    <div class="absolute top-1/2 left-1/3 w-64 h-64 rounded-full blur-3xl pointer-events-none"
         :class="theme === 'dark' ? 'bg-yellow-500/10' : 'bg-yellow-200/20'"></div>

    <div class="w-full max-w-md relative z-10">
      <!-- Logo area -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-gradient-to-br from-amber-400 to-orange-500 mb-4 shadow-lg shadow-amber-500/25">
          <span class="text-4xl">🦆</span>
        </div>
        <h1 class="text-3xl font-bold mb-1" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">背词鸭</h1>
        <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">Vocab Duck</p>
      </div>

      <!-- Card -->
      <div class="rounded-2xl p-8 backdrop-blur-xl transition-colors duration-300"
           :class="theme === 'dark'
             ? 'bg-slate-800/60 border border-slate-700/50'
             : 'bg-white/80 border border-gray-200 shadow-xl shadow-gray-200/50'">
        <!-- Toggle tabs -->
        <div class="flex mb-6 rounded-xl p-1 transition-colors duration-300"
             :class="theme === 'dark' ? 'bg-slate-800/60' : 'bg-gray-100'">
          <button
            class="flex-1 py-2.5 rounded-lg text-sm font-medium transition-all duration-200"
            :class="isLogin
              ? 'bg-amber-500 text-white shadow-md shadow-amber-500/30'
              : (theme === 'dark' ? 'text-slate-400 hover:text-slate-200' : 'text-gray-500 hover:text-gray-700')"
            @click="isLogin = true"
          >
            登录
          </button>
          <button
            class="flex-1 py-2.5 rounded-lg text-sm font-medium transition-all duration-200"
            :class="!isLogin
              ? 'bg-amber-500 text-white shadow-md shadow-amber-500/30'
              : (theme === 'dark' ? 'text-slate-400 hover:text-slate-200' : 'text-gray-500 hover:text-gray-700')"
            @click="isLogin = false"
          >
            注册
          </button>
        </div>

        <!-- Success display (after registration) -->
        <div
          v-if="registerSuccess"
          class="mb-4 px-4 py-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-sm flex items-start gap-2"
        >
          <span class="mt-0.5 shrink-0">&#10003;</span>
          <span>{{ successMsg }}</span>
        </div>

        <!-- Error display -->
        <div
          v-if="errorMsg"
          class="mb-4 px-4 py-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-sm flex items-start gap-2"
        >
          <span class="mt-0.5 shrink-0">&#9888;</span>
          <span>{{ errorMsg }}</span>
        </div>

        <!-- Login form -->
        <form v-if="isLogin" @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-sm font-medium mb-1.5" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">用户名</label>
            <input
              v-model="loginForm.username"
              type="text"
              required
              placeholder="请输入用户名"
              class="input-field w-full"
            />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1.5" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">密码</label>
            <input
              v-model="loginForm.password"
              type="password"
              required
              placeholder="请输入密码"
              class="input-field w-full"
            />
          </div>
          <button
            type="submit"
            :disabled="loading"
            class="btn-primary w-full py-3 rounded-xl font-medium text-white transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="loading" class="inline-flex items-center gap-2">
              <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/></svg>
              登录中...
            </span>
            <span v-else>登 录</span>
          </button>
        </form>

        <!-- Register form -->
        <form v-else @submit.prevent="handleRegister" class="space-y-4">
          <div>
            <label class="block text-sm font-medium mb-1.5" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">用户名</label>
            <input
              v-model="registerForm.username"
              type="text"
              required
              placeholder="请输入用户名"
              class="input-field w-full"
            />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1.5" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">邮箱</label>
            <input
              v-model="registerForm.email"
              type="email"
              required
              placeholder="请输入邮箱地址"
              class="input-field w-full"
            />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1.5" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">密码</label>
            <input
              v-model="registerForm.password"
              type="password"
              required
              placeholder="请设置密码（至少6位）"
              minlength="6"
              class="input-field w-full"
            />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1.5" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">目标考试</label>
            <select v-model="registerForm.targetExam" required class="input-field w-full">
              <option value="" disabled>请选择目标考试</option>
              <option value="cet4">大学英语四级 (CET-4)</option>
              <option value="cet6">大学英语六级 (CET-6)</option>
              <option value="kaoyan">考研英语</option>
              <option value="ielts">雅思 (IELTS)</option>
              <option value="toefl">托福 (TOEFL)</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium mb-1.5" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">每日新词数量</label>
            <input
              v-model.number="registerForm.dailyNewWords"
              type="number"
              required
              min="5"
              max="100"
              placeholder="建议 20-50 个"
              class="input-field w-full"
            />
          </div>
          <button
            type="submit"
            :disabled="loading"
            class="btn-primary w-full py-3 rounded-xl font-medium text-white transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="loading" class="inline-flex items-center gap-2">
              <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/></svg>
              注册中...
            </span>
            <span v-else>注 册</span>
          </button>
        </form>

        <!-- Footer hint -->
        <p class="mt-6 text-center text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">
          <BookOpen class="inline w-3.5 h-3.5 mr-1 -mt-0.5" />
          基于多智能体架构的智能词汇学习平台
        </p>
      </div>

      <!-- Exam badges -->
      <div class="flex justify-center gap-3 mt-6">
        <span class="px-3 py-1 rounded-full text-xs transition-colors duration-300"
              :class="theme === 'dark' ? 'bg-slate-800/60 text-slate-400 border border-slate-700/50' : 'bg-white text-gray-500 border border-gray-200 shadow-sm'">
          <GraduationCap class="inline w-3.5 h-3.5 mr-1 -mt-0.5" />CET-4/6
        </span>
        <span class="px-3 py-1 rounded-full text-xs transition-colors duration-300"
              :class="theme === 'dark' ? 'bg-slate-800/60 text-slate-400 border border-slate-700/50' : 'bg-white text-gray-500 border border-gray-200 shadow-sm'">考研</span>
        <span class="px-3 py-1 rounded-full text-xs transition-colors duration-300"
              :class="theme === 'dark' ? 'bg-slate-800/60 text-slate-400 border border-slate-700/50' : 'bg-white text-gray-500 border border-gray-200 shadow-sm'">雅思</span>
        <span class="px-3 py-1 rounded-full text-xs transition-colors duration-300"
              :class="theme === 'dark' ? 'bg-slate-800/60 text-slate-400 border border-slate-700/50' : 'bg-white text-gray-500 border border-gray-200 shadow-sm'">托福</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import axios from 'axios'
import { BookOpen, GraduationCap } from 'lucide-vue-next'
import { useTheme } from '../composables/useTheme'

const emit = defineEmits(['login-success'])
const { theme } = useTheme()

const isLogin = ref(true)
const loading = ref(false)
const errorMsg = ref('')
const registerSuccess = ref(false)
const successMsg = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  targetExam: '',
  dailyNewWords: 30
})

function clearError() {
  errorMsg.value = ''
  registerSuccess.value = false
}

async function handleLogin() {
  clearError()
  if (!loginForm.username || !loginForm.password) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  try {
    const res = await axios.post('/api/auth/login', {
      username: loginForm.username,
      password: loginForm.password
    })
    const { accessToken, tokenType, user } = res.data
    emit('login-success', accessToken, user)
  } catch (err) {
    errorMsg.value = err.response?.data?.detail || err.response?.data?.message || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  clearError()
  if (!registerForm.username || !registerForm.email || !registerForm.password) {
    errorMsg.value = '请填写所有必填字段'
    return
  }
  if (!registerForm.targetExam) {
    errorMsg.value = '请选择目标考试'
    return
  }
  if (registerForm.password.length < 6) {
    errorMsg.value = '密码长度至少为6位'
    return
  }
  loading.value = true
  try {
    await axios.post('/api/auth/register', {
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      targetExam: registerForm.targetExam,
      dailyNewWords: registerForm.dailyNewWords
    })
    // 注册成功后不自动登录，显示提示信息并切换到登录页
    successMsg.value = '注册成功！请使用你的账号登录'
    registerSuccess.value = true
    loginForm.username = registerForm.username
    isLogin.value = true
  } catch (err) {
    errorMsg.value = err.response?.data?.detail || err.response?.data?.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>
