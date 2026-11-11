<template>
  <div class="space-y-6">
    <!-- Library Completed Banner -->
    <div v-if="libraryCompleted" class="flex flex-col items-center justify-center py-16 text-center">
      <div class="w-24 h-24 rounded-2xl flex items-center justify-center mb-6 bg-gradient-to-br from-amber-400 to-orange-500 shadow-lg shadow-amber-500/20">
        <span class="text-5xl">🎉</span>
      </div>
      <h2 class="text-2xl font-bold mb-3" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
        已经完成全部单词背诵，祝你考试顺利！
      </h2>
      <p class="mb-6 text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">
        恭喜你完成了「{{ libraryName }}」词库的全部单词学习
      </p>
      <div class="flex flex-wrap gap-3 justify-center">
        <button @click="goToLibrary" class="btn-secondary flex items-center gap-2">
          <BookOpen class="w-4 h-4" />
          更换词库
        </button>
        <button @click="$router.push('/review')" class="btn-primary flex items-center gap-2">
          <Sparkles class="w-4 h-4" />
          去复习
        </button>
      </div>
    </div>

    <!-- Library Progress Bar -->
    <div v-else-if="libraryProgress && !loading" class="glass-card p-3">
      <div class="flex items-center justify-between mb-1.5">
        <span class="text-xs font-medium" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">
          📘 {{ libraryName }} 词库进度
        </span>
        <span class="text-xs" :class="theme === 'dark' ? 'text-amber-400' : 'text-amber-600'">
          {{ libraryProgress.learnedWords }} / {{ libraryProgress.totalWords }}
          ({{ Math.round(libraryProgress.progress * 100) }}%)
        </span>
      </div>
      <div class="h-2 rounded-full overflow-hidden"
           :class="theme === 'dark' ? 'bg-slate-700' : 'bg-gray-200'">
        <div class="h-full rounded-full bg-gradient-to-r from-amber-500 to-orange-500 transition-all duration-500"
             :style="{ width: `${libraryProgress.progress * 100}%` }"></div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="!libraryCompleted && loading" class="flex items-center justify-center h-96">
      <div class="text-center">
        <div class="w-12 h-12 border-4 border-amber-500/30 border-t-amber-500 rounded-full animate-spin mx-auto mb-4"></div>
        <p :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">加载单词中...</p>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!libraryCompleted && words.length === 0" class="flex flex-col items-center justify-center h-96 text-center">
      <div class="w-20 h-20 rounded-2xl flex items-center justify-center mb-6"
           :class="theme === 'dark' ? 'bg-gradient-to-br from-slate-700 to-slate-800' : 'bg-gradient-to-br from-gray-200 to-gray-300'">
        <BookMarked class="w-10 h-10" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'" />
      </div>
      <h2 class="text-xl font-bold mb-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">暂无新词可学</h2>
      <p class="mb-6" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">你已学完当前词库，或暂无新内容</p>
      <button @click="loadNewWords" class="btn-primary">
        <Sparkles class="w-4 h-4" />
        换一批
      </button>
    </div>

    <!-- Learning Card View -->
    <template v-else-if="!libraryCompleted">
      <!-- Header & Progress -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">学习新词</h1>
          <p class="mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">点击卡片翻转查看详细内容</p>
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="loadNewWords(true)"
            :disabled="loading"
            class="btn-secondary flex items-center gap-2"
          >
            <RefreshCw class="w-4 h-4" />
            换一批
          </button>
        </div>
      </div>

      <!-- Progress Bar -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">学习进度</span>
          <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ currentIndex + 1 }} / {{ words.length }}</span>
        </div>
        <div class="progress-bar">
          <div
            class="progress-bar-fill"
            :style="{ width: `${((currentIndex + 1) / words.length) * 100}%` }"
          ></div>
        </div>
      </div>

      <!-- Card Loading -->
      <div v-if="cardLoading" class="flex items-center justify-center h-80">
        <div class="w-10 h-10 border-4 border-amber-500/30 border-t-amber-500 rounded-full animate-spin"></div>
      </div>

      <!-- Flashcard -->
      <div
        v-else-if="cardContent"
        class="word-card cursor-pointer select-none glass-card"
        @click="flipped = !flipped"
      >
        <!-- Front Side -->
        <div v-if="!flipped" class="flex flex-col items-center justify-center h-80 text-center px-6">
          <h2 class="text-4xl font-bold mb-3" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ cardContent.word }}</h2>
          <p v-if="cardContent.phonetic" class="text-lg italic mb-4" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ cardContent.phonetic }}</p>
          <div class="flex items-center gap-1">
            <Star
              v-for="i in 5"
              :key="i"
              class="w-5 h-5"
              :class="i <= difficultyLevel ? 'text-amber-400 fill-amber-400' : (theme === 'dark' ? 'text-slate-600' : 'text-gray-300')"
            />
          </div>
          <p class="text-xs mt-6" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">点击翻转查看详情</p>
        </div>

        <!-- Back Side -->
        <div v-else class="h-80 overflow-y-auto px-6 py-4 space-y-4 custom-scrollbar">
          <!-- Definitions -->
          <div v-if="cardContent.definitions && cardContent.definitions.length > 0">
            <h3 class="text-xs font-semibold uppercase tracking-wide mb-2 flex items-center gap-1.5"
                :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">
              <BookOpen class="w-3.5 h-3.5" />
              释义
            </h3>
            <div class="space-y-1.5">
              <div
                v-for="(def, dIdx) in cardContent.definitions"
                :key="dIdx"
                class="text-sm flex gap-2"
                :class="theme === 'dark' ? 'text-slate-200' : 'text-gray-700'"
              >
                <span v-if="def.pos" class="text-amber-500 font-medium flex-shrink-0">{{ def.pos }}.</span>
                <span>{{ def.meaning || def }}</span>
              </div>
            </div>
          </div>

          <!-- Mnemonic -->
          <div
            v-if="cardContent.mnemonic"
            class="p-3 rounded-lg"
            :class="theme === 'dark' ? 'bg-amber-500/10 border border-amber-500/30' : 'bg-amber-50 border border-amber-200'"
          >
            <div class="flex items-center gap-1.5 text-amber-500 font-medium text-xs mb-1.5">
              <Lightbulb class="w-3.5 h-3.5" />
              记忆技巧
            </div>
            <p class="text-sm" :class="theme === 'dark' ? 'text-amber-100' : 'text-amber-800'">{{ cardContent.mnemonic }}</p>
          </div>

          <!-- Etymology -->
          <div
            v-if="cardContent.etymology"
            class="p-3 rounded-lg"
            :class="theme === 'dark' ? 'bg-blue-500/10 border border-blue-500/30' : 'bg-blue-50 border border-blue-200'"
          >
            <div class="flex items-center gap-1.5 text-blue-400 font-medium text-xs mb-1.5">
              <BookMarked class="w-3.5 h-3.5" />
              词源解析
            </div>
            <p class="text-sm" :class="theme === 'dark' ? 'text-blue-100' : 'text-blue-800'">{{ cardContent.etymology }}</p>
          </div>

          <!-- Examples -->
          <div v-if="cardContent.examples && cardContent.examples.length > 0">
            <h3 class="text-xs font-semibold uppercase tracking-wide mb-2"
                :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">例句</h3>
            <div class="space-y-2">
              <div
                v-for="(ex, eIdx) in cardContent.examples"
                :key="eIdx"
                class="text-sm pl-3"
                :class="theme === 'dark' ? 'text-slate-300 border-l-2 border-slate-600' : 'text-gray-600 border-l-2 border-gray-300'"
              >
                <div>{{ ex.en || ex.sentence || ex }}</div>
                <div v-if="ex.zh || ex.translation" class="text-xs mt-0.5" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">
                  {{ ex.zh || ex.translation }}
                </div>
              </div>
            </div>
          </div>

          <!-- Collocations -->
          <div v-if="cardContent.collocations && cardContent.collocations.length > 0">
            <h3 class="text-xs font-semibold uppercase tracking-wide mb-2"
                :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">常见搭配</h3>
            <div class="flex flex-wrap gap-2">
              <span
                v-for="(col, cIdx) in cardContent.collocations"
                :key="cIdx"
                class="px-2.5 py-1 rounded-full text-xs"
                :class="theme === 'dark' ? 'bg-slate-700/60 border border-slate-600/50 text-slate-300' : 'bg-gray-100 border border-gray-200 text-gray-600'"
              >
                {{ typeof col === 'string' ? col : col.phrase || col.text }}
              </span>
            </div>
          </div>

          <!-- Mini Story -->
          <div
            v-if="cardContent.mini_story"
            class="p-3 rounded-lg"
            :class="theme === 'dark' ? 'bg-emerald-500/10 border border-emerald-500/30' : 'bg-emerald-50 border border-emerald-200'"
          >
            <div class="flex items-center gap-1.5 text-emerald-400 font-medium text-xs mb-1.5">
              <Sparkles class="w-3.5 h-3.5" />
              迷你故事
            </div>
            <p class="text-sm leading-relaxed" :class="theme === 'dark' ? 'text-emerald-100' : 'text-emerald-800'">{{ cardContent.mini_story }}</p>
          </div>

          <!-- Exam Tips -->
          <div
            v-if="cardContent.exam_tips"
            class="p-3 rounded-lg"
            :class="theme === 'dark' ? 'bg-purple-500/10 border border-purple-500/30' : 'bg-purple-50 border border-purple-200'"
          >
            <div class="flex items-center gap-1.5 text-purple-400 font-medium text-xs mb-1.5">
              <BookMarked class="w-3.5 h-3.5" />
              考试提示
            </div>
            <p class="text-sm" :class="theme === 'dark' ? 'text-purple-100' : 'text-purple-800'">{{ cardContent.exam_tips }}</p>
          </div>

          <!-- Image Prompt -->
          <div
            v-if="cardContent.image_prompt"
            class="p-4 rounded-xl border"
            :class="theme === 'dark' ? 'bg-gradient-to-br from-purple-500/20 via-pink-500/20 to-orange-500/20 border-purple-500/30' : 'bg-gradient-to-br from-purple-50 via-pink-50 to-orange-50 border-purple-200'"
          >
            <div class="flex items-center gap-1.5 font-medium text-xs mb-1.5"
                 :class="theme === 'dark' ? 'text-purple-300' : 'text-purple-600'">
              <Image class="w-3.5 h-3.5" />
              联想画面
            </div>
            <p class="text-sm italic leading-relaxed" :class="theme === 'dark' ? 'text-slate-200' : 'text-gray-700'">{{ cardContent.image_prompt }}</p>
          </div>

          <p class="text-xs text-center pt-2" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">点击翻转回正面</p>
        </div>
      </div>

      <!-- Navigation -->
      <div class="flex items-center justify-between gap-4">
        <button
          @click="prevCard"
          :disabled="currentIndex === 0"
          class="btn-secondary flex items-center gap-2"
        >
          <ChevronLeft class="w-4 h-4" />
          上一个
        </button>

        <div class="flex items-center gap-2">
          <button
            @click="generateWordImage"
            :disabled="cardLoading || !cardContent"
            class="btn-outline flex items-center gap-2"
            :class="theme === 'dark' ? 'text-slate-400 hover:text-slate-200 border-slate-600 hover:border-slate-500' : 'text-gray-500 hover:text-gray-700 border-gray-300 hover:border-gray-400'"
            title="AI生成配图"
          >
            <Camera class="w-4 h-4" />
            配图
          </button>
        </div>

        <button
          @click="skipWord"
          :disabled="words.length === 0"
          class="btn-outline flex items-center gap-2"
          :class="theme === 'dark' ? 'text-slate-400 hover:text-slate-200 border-slate-600 hover:border-slate-500' : 'text-gray-500 hover:text-gray-700 border-gray-300 hover:border-gray-400'"
        >
          <SkipForward class="w-4 h-4" />
          跳过
        </button>

        <button
          @click="markLearned"
          :disabled="markingLearned"
          class="btn-accent flex items-center gap-2"
        >
          <BookMarked class="w-4 h-4" />
          {{ markingLearned ? '标记中...' : '标记已学' }}
        </button>

        <button
          @click="nextCard"
          :disabled="currentIndex === words.length - 1"
          class="btn-primary flex items-center gap-2"
        >
          下一个
          <ChevronRight class="w-4 h-4" />
        </button>
      </div>
    </template>
  </div>

  <!-- Image Generation Modal -->
  <Teleport to="body">
    <div
      v-if="showImageModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
      @click.self="closeImageModal"
    >
      <div class="glass-card w-full max-w-2xl max-h-[80vh] overflow-y-auto">
        <div class="flex items-center justify-between p-4 border-b" :class="theme === 'dark' ? 'border-slate-700' : 'border-gray-200'">
          <h3 class="text-lg font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
            <Camera class="w-5 h-5 inline mr-2 text-purple-400" />
            AI 生成配图
          </h3>
          <button @click="closeImageModal" class="p-1 rounded-lg hover:bg-slate-700/50 transition-colors">
            <span class="text-xl" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">&times;</span>
          </button>
        </div>
        <div class="p-6">
          <div v-if="imageLoading" class="flex flex-col items-center justify-center py-12">
            <div class="w-12 h-12 border-4 border-amber-500/30 border-t-amber-500 rounded-full animate-spin mb-4"></div>
            <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">正在生成图像...</p>
          </div>
          <div v-else-if="generatedImage">
            <div v-if="generatedImage.status === 'success' && generatedImage.imageUrl" class="space-y-4">
              <div class="relative rounded-xl overflow-hidden" :class="theme === 'dark' ? 'bg-slate-800' : 'bg-gray-100'">
                <img :src="generatedImage.imageUrl" alt="Generated word image" class="w-full h-auto max-h-[50vh] object-contain" />
              </div>
              <div v-if="generatedImage.prompt" class="p-3 rounded-lg" :class="theme === 'dark' ? 'bg-slate-800/50' : 'bg-gray-50'">
                <p class="text-xs font-medium mb-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">图像生成提示词:</p>
                <p class="text-sm italic" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">{{ generatedImage.prompt }}</p>
              </div>
            </div>
            <div v-else class="text-center py-12">
              <Image class="w-16 h-16 mx-auto mb-4 opacity-50" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'" />
              <p class="text-sm mb-2" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ generatedImage.message || '无法生成图像' }}</p>
              <p v-if="generatedImage.message && generatedImage.message.includes('API')" class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">请在系统设置中配置图像生成API密钥</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>

  
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTheme } from '../composables/useTheme'
import { useUserStore } from '../stores/user'
import {
  BookOpen,
  ChevronRight,
  ChevronLeft,
  Star,
  Lightbulb,
  BookMarked,
  Image,
  Sparkles,
  SkipForward,
  Camera,
  RefreshCw
} from 'lucide-vue-next'
import { api } from '../services/api'

const { theme } = useTheme()
const router = useRouter()
const userStore = useUserStore()

const words = ref([])
const currentIndex = ref(0)
const cardContent = ref(null)
const flipped = ref(false)
const loading = ref(true)
const cardLoading = ref(false)
const markingLearned = ref(false)
const libraryProgress = ref(null)
const libraryCompleted = ref(false)
const skippedWords = ref([])

const difficultyLevel = ref(3)
const generatedImage = ref(null)
const imageLoading = ref(false)
const showImageModal = ref(false)

const LIBRARY_NAMES = {
  cet4: '四级核心单词',
  cet6: '六级核心单词',
  kaoyan: '考研单词',
  ielts: '雅思单词',
  toefl: '托福单词'
}

const libraryName = computed(() => {
  const lib = userStore.user?.selectedLibrary
  return lib ? (LIBRARY_NAMES[lib] || lib) : ''
})

async function loadLibraryProgress() {
  try {
    const res = await api.get('/api/learning/library-progress')
    libraryProgress.value = res.data
    if (res.data.completed && res.data.totalWords > 0) {
      libraryCompleted.value = true
    }
  } catch (err) {
    console.error('Failed to load library progress:', err)
  }
}

function goToLibrary() {
  router.push('/library')
}

async function loadNewWords(random = false) {
  loading.value = true
  flipped.value = false
  currentIndex.value = 0
  cardContent.value = null

  try {
    const res = await api.get('/api/learning/new-words', { params: { count: 10, random } })
    const data = res.data
    words.value = Array.isArray(data) ? data : (data.words || [])

    if (words.value.length > 0) {
      await loadCard(words.value[0].wordId || words.value[0].id || words.value[0])
    }
  } catch (err) {
    console.error('Failed to load new words:', err)
    words.value = []
  } finally {
    loading.value = false
  }
}

async function loadCard(wordId) {
  cardLoading.value = true
  cardContent.value = null
  flipped.value = false

  try {
    const id = typeof wordId === 'object' ? (wordId.wordId || wordId.id || wordId.word_id) : wordId
    const res = await api.get(`/api/learning/word-card/${id}`)
    const data = res.data
    cardContent.value = data.content || data
    difficultyLevel.value = cardContent.value.difficulty || cardContent.value.difficulty_level || 3
  } catch (err) {
    console.error('Failed to load word card:', err)
    cardContent.value = { word: '加载失败', definitions: [{ meaning: '无法获取单词详情，请稍后重试' }] }
  } finally {
    cardLoading.value = false
  }
}

function prevCard() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    const word = words.value[currentIndex.value]
    loadCard(word.wordId || word.id || word)
  }
}

function nextCard() {
  if (currentIndex.value < words.value.length - 1) {
    currentIndex.value++
    const word = words.value[currentIndex.value]
    loadCard(word.wordId || word.id || word)
  }
}

function skipWord() {
  const word = words.value[currentIndex.value]
  if (word) {
    const wordId = word.wordId || word.id || word
    if (!skippedWords.value.includes(wordId)) {
      skippedWords.value.push(wordId)
    }
  }

  if (currentIndex.value < words.value.length - 1) {
    currentIndex.value++
    const nextWord = words.value[currentIndex.value]
    loadCard(nextWord.wordId || nextWord.id || nextWord)
  } else {
    loadNewWords(true)
  }
}

async function markLearned() {
  if (markingLearned.value) return
  const word = words.value[currentIndex.value]
  if (!word) return

  markingLearned.value = true
  try {
    const id = word.wordId || word.id || word
    await api.post(`/api/learning/learn-word/${id}`)

    // Refresh library progress
    await loadLibraryProgress()

    // Update streak days
    try {
      const streakRes = await api.get('/api/dashboard/streak-calculation')
      if (streakRes.data.currentStreak !== undefined) {
        userStore.updateStreak(streakRes.data.currentStreak)
      }
    } catch (e) {
      console.log('Failed to update streak:', e)
    }

    if (libraryCompleted.value) {
      loading.value = false
      return
    }

    if (currentIndex.value < words.value.length - 1) {
      nextCard()
    } else {
      await loadNewWords()
    }
  } catch (err) {
    console.error('Failed to mark word as learned:', err)
  } finally {
    markingLearned.value = false
  }
}

async function generateWordImage() {
  const word = words.value[currentIndex.value]
  if (!word) return

  imageLoading.value = true
  generatedImage.value = null
  showImageModal.value = true

  try {
    const id = word.wordId || word.id || word
    const res = await api.get(`/api/learning/word-image/${id}`)
    generatedImage.value = res.data.result
  } catch (err) {
    console.error('Failed to generate image:', err)
    generatedImage.value = { status: 'error', message: '图像生成失败，请稍后重试' }
  } finally {
    imageLoading.value = false
  }
}

function closeImageModal() {
  showImageModal.value = false
  generatedImage.value = null
}

onMounted(async () => {
  // 如果没有选择词库，跳转到词库选择页面
  if (!userStore.user?.selectedLibrary) {
    router.push('/library')
    return
  }
  await loadLibraryProgress()
  if (!libraryCompleted.value) {
    loadNewWords()
  } else {
    loading.value = false
  }
})
</script>

<style scoped>
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(75, 85, 99, 0.5) transparent;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(75, 85, 99, 0.5);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
</style>
