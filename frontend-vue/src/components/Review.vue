<template>
  <div class="space-y-6">
    <!-- View Switcher -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">复习中心</h1>
        <p class="mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">巩固记忆，强化学习效果</p>
      </div>
      <div class="flex items-center gap-2 p-1 rounded-lg" :class="theme === 'dark' ? 'bg-slate-800/60' : 'bg-gray-100'">
        <button
          @click="currentView = 'review'"
          class="px-4 py-2 rounded-md text-sm font-medium transition-all"
          :class="currentView === 'review'
            ? 'bg-amber-500 text-white'
            : (theme === 'dark' ? 'text-slate-400 hover:text-white' : 'text-gray-600 hover:text-gray-900')"
        >
          <RotateCcw class="w-4 h-4 inline mr-1" />
          开始复习
        </button>
        <button
          @click="currentView = 'list'"
          class="px-4 py-2 rounded-md text-sm font-medium transition-all"
          :class="currentView === 'list'
            ? 'bg-amber-500 text-white'
            : (theme === 'dark' ? 'text-slate-400 hover:text-white' : 'text-gray-600 hover:text-gray-900')"
        >
          <List class="w-4 h-4 inline mr-1" />
          单词列表
        </button>
        <button
          @click="currentView = 'history'"
          class="px-4 py-2 rounded-md text-sm font-medium transition-all"
          :class="currentView === 'history'
            ? 'bg-amber-500 text-white'
            : (theme === 'dark' ? 'text-slate-400 hover:text-white' : 'text-gray-600 hover:text-gray-900')"
        >
          <History class="w-4 h-4 inline mr-1" />
          复习历史
        </button>
      </div>
    </div>

    <!-- Review Stats Card -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="glass-card p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-orange-500/20 flex items-center justify-center">
            <RotateCcw class="w-5 h-5 text-orange-400" />
          </div>
          <div>
            <p class="text-2xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ dueWordsCount }}</p>
            <p class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">待复习单词</p>
          </div>
        </div>
      </div>
      <div class="glass-card p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-emerald-500/20 flex items-center justify-center">
            <CheckCircle2 class="w-5 h-5 text-emerald-400" />
          </div>
          <div>
            <p class="text-2xl font-bold text-emerald-400">{{ reviewStats.correctReviews }}</p>
            <p class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">本周正确</p>
          </div>
        </div>
      </div>
      <div class="glass-card p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-red-500/20 flex items-center justify-center">
            <XCircle class="w-5 h-5 text-red-400" />
          </div>
          <div>
            <p class="text-2xl font-bold text-red-400">{{ reviewStats.totalReviews - reviewStats.correctReviews }}</p>
            <p class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">本周错误</p>
          </div>
        </div>
      </div>
      <div class="glass-card p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-cyan-500/20 flex items-center justify-center">
            <Target class="w-5 h-5 text-cyan-400" />
          </div>
          <div>
            <p class="text-2xl font-bold text-cyan-400">{{ reviewStats.accuracy }}%</p>
            <p class="text-xs" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">本周正确率</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Review View -->
    <template v-if="currentView === 'review'">
      <!-- Loading State -->
      <div v-if="loading" class="flex items-center justify-center h-96">
        <div class="text-center">
          <div class="w-12 h-12 border-4 border-amber-500/30 border-t-amber-500 rounded-full animate-spin mx-auto mb-4"></div>
          <p :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">加载复习单词中...</p>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="dueWords.length === 0 && !finished" class="flex flex-col items-center justify-center h-96 text-center">
        <div class="w-20 h-20 rounded-2xl flex items-center justify-center mb-6"
             :class="theme === 'dark' ? 'bg-gradient-to-br from-emerald-700/40 to-emerald-900/40' : 'bg-gradient-to-br from-emerald-100 to-emerald-200'">
          <CheckCircle2 class="w-10 h-10 text-emerald-400" />
        </div>
        <h2 class="text-xl font-bold mb-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">暂无需要复习的单词</h2>
        <p class="mb-6" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">所有单词都还没到复习时间，继续保持！</p>
        <button @click="fetchDueWords" class="btn-primary">
          <RotateCcw class="w-4 h-4" />
          刷新
        </button>
      </div>

      <!-- Finished / Completion Screen -->
      <div v-else-if="finished" class="flex flex-col items-center justify-center min-h-[480px] text-center space-y-8">
        <div class="w-20 h-20 rounded-2xl flex items-center justify-center"
             :class="theme === 'dark' ? 'bg-gradient-to-br from-emerald-700/40 to-amber-900/40' : 'bg-gradient-to-br from-emerald-100 to-amber-100'">
          <CheckCircle2 class="w-10 h-10 text-emerald-400" />
        </div>

        <div>
          <h2 class="text-2xl font-bold mb-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">复习完成！</h2>
          <p :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">本轮复习已全部完成，以下是你的成绩</p>
        </div>

        <!-- Stats Grid -->
        <div class="grid grid-cols-3 gap-4 w-full max-w-md">
          <div class="glass-card p-4 text-center">
            <p class="text-3xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ stats.total }}</p>
            <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">总复习数</p>
          </div>
          <div class="glass-card p-4 text-center">
            <p class="text-3xl font-bold text-emerald-400">{{ stats.correct }}</p>
            <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">正确</p>
          </div>
          <div class="glass-card p-4 text-center">
            <p class="text-3xl font-bold text-red-400">{{ stats.wrong }}</p>
            <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">错误</p>
          </div>
        </div>

        <!-- Accuracy Bar -->
        <div class="w-full max-w-md">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">正确率</span>
            <span class="text-sm font-semibold" :class="accuracy >= 70 ? 'text-emerald-400' : accuracy >= 40 ? 'text-amber-500' : 'text-red-400'">
              {{ accuracy }}%
            </span>
          </div>
          <div class="progress-bar">
            <div
              class="progress-bar-fill"
              :class="accuracy >= 70 ? '!bg-gradient-to-r !from-emerald-500 !to-emerald-400' : accuracy >= 40 ? '!bg-gradient-to-r !from-amber-500 !to-amber-400' : '!bg-gradient-to-r !from-red-500 !to-red-400'"
              :style="{ width: `${accuracy}%` }"
            ></div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex items-center gap-4">
          <button @click="restartReview" class="btn-primary">
            <RotateCcw class="w-4 h-4" />
            再来一轮
          </button>
          <button @click="currentView = 'history'" class="btn-secondary">
            <History class="w-4 h-4" />
            查看历史
          </button>
        </div>
      </div>

      <!-- Review Active View -->
      <template v-else>
        <!-- Header & Running Stats -->
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">间隔复习</h2>
            <p class="mt-1" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">评估你对每个单词的记忆程度</p>
          </div>
          <div class="flex items-center gap-4">
            <div class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg"
                 :class="theme === 'dark' ? 'bg-emerald-500/10 border border-emerald-500/30' : 'bg-emerald-50 border border-emerald-200'">
              <CheckCircle2 class="w-4 h-4 text-emerald-400" />
              <span class="text-sm font-semibold text-emerald-400">{{ stats.correct }}</span>
            </div>
            <div class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg"
                 :class="theme === 'dark' ? 'bg-red-500/10 border border-red-500/30' : 'bg-red-50 border border-red-200'">
              <XCircle class="w-4 h-4 text-red-400" />
              <span class="text-sm font-semibold text-red-400">{{ stats.wrong }}</span>
            </div>
          </div>
        </div>

        <!-- Progress Bar -->
        <div class="glass-card p-4">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">复习进度</span>
            <span class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ currentIndex + 1 }} / {{ dueWords.length }}</span>
          </div>
          <div class="progress-bar">
            <div
              class="progress-bar-fill"
              :style="{ width: `${((currentIndex + 1) / dueWords.length) * 100}%` }"
            ></div>
          </div>
        </div>

        <!-- Review Card -->
        <div v-if="currentWord" class="glass-card p-6 space-y-5">
          <!-- Word Display -->
          <div class="text-center space-y-2">
            <div class="flex items-center justify-center gap-3">
              <h2 class="text-4xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ currentWord.word }}</h2>
              
            </div>
            <p v-if="currentWord.phonetic" class="text-lg italic" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ currentWord.phonetic }}</p>
          </div>

          <!-- Divider -->
          <div class="border-t" :class="theme === 'dark' ? 'border-slate-700/50' : 'border-gray-200'"></div>

          <!-- Show Answer Button (before reveal) -->
          <div v-if="!showAnswer" class="flex justify-center pt-2">
            <button @click="showAnswer = true" class="btn-primary text-base px-8 py-3">
              显示答案
            </button>
          </div>

          <!-- Answer Details (after reveal) -->
          <div v-else class="space-y-5">
            <!-- Definitions -->
            <div class="space-y-3">
              <div v-if="currentWord.definition_cn" class="p-3 rounded-lg"
                   :class="theme === 'dark' ? 'bg-blue-500/10 border border-blue-500/30' : 'bg-blue-50 border border-blue-200'">
                <p class="text-xs font-medium mb-1" :class="theme === 'dark' ? 'text-blue-400' : 'text-blue-600'">中文释义</p>
                <p class="text-sm" :class="theme === 'dark' ? 'text-slate-200' : 'text-gray-700'">{{ currentWord.definition_cn }}</p>
              </div>
              <div v-if="currentWord.definition_en" class="p-3 rounded-lg"
                   :class="theme === 'dark' ? 'bg-indigo-500/10 border border-indigo-500/30' : 'bg-indigo-50 border border-indigo-200'">
                <p class="text-xs font-medium mb-1" :class="theme === 'dark' ? 'text-indigo-400' : 'text-indigo-600'">英文释义</p>
                <p class="text-sm" :class="theme === 'dark' ? 'text-slate-200' : 'text-gray-700'">{{ currentWord.definition_en }}</p>
              </div>
            </div>

            <!-- Mastery & Interval Info -->
            <div class="flex items-center justify-center gap-6 text-sm">
              <div class="flex items-center gap-2">
                <span :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">掌握程度:</span>
                <div class="flex items-center gap-0.5">
                  <span
                    v-for="i in 5"
                    :key="i"
                    class="w-2.5 h-2.5 rounded-full"
                    :class="i <= Math.round(currentWord.masteryLevel || 0) * 5 ? 'bg-amber-400' : (theme === 'dark' ? 'bg-slate-600' : 'bg-gray-300')"
                  ></span>
                </div>
                <span :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ (currentWord.masteryLevel || 0).toFixed(2) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Clock class="w-4 h-4" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'" />
                <span :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">间隔:</span>
                <span class="font-medium" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ currentWord.intervalDays || 0 }} 天</span>
              </div>
            </div>

            <!-- Quality Rating Buttons -->
            <div>
              <p class="text-center text-sm mb-3" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">你的记忆程度如何？</p>
              <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <button
                  @click="handleQuality(0)"
                  class="flex flex-col items-center gap-1.5 p-3 rounded-xl transition-all cursor-pointer"
                  :class="theme === 'dark'
                    ? 'bg-red-500/10 border border-red-500/30 text-red-400 hover:bg-red-500/20 hover:border-red-500/50'
                    : 'bg-red-50 border border-red-200 text-red-500 hover:bg-red-100 hover:border-red-300'"
                >
                  <XCircle class="w-6 h-6" />
                  <span class="text-xs font-medium">完全不记得</span>
                </button>

                <button
                  @click="handleQuality(2)"
                  class="flex flex-col items-center gap-1.5 p-3 rounded-xl transition-all cursor-pointer"
                  :class="theme === 'dark'
                    ? 'bg-orange-500/10 border border-orange-500/30 text-orange-400 hover:bg-orange-500/20 hover:border-orange-500/50'
                    : 'bg-orange-50 border border-orange-200 text-orange-500 hover:bg-orange-100 hover:border-orange-300'"
                >
                  <ThumbsDown class="w-6 h-6" />
                  <span class="text-xs font-medium">有印象但答错</span>
                </button>

                <button
                  @click="handleQuality(3)"
                  class="flex flex-col items-center gap-1.5 p-3 rounded-xl transition-all cursor-pointer"
                  :class="theme === 'dark'
                    ? 'bg-amber-500/10 border border-amber-500/30 text-amber-400 hover:bg-amber-500/20 hover:border-amber-500/50'
                    : 'bg-amber-50 border border-amber-200 text-amber-600 hover:bg-amber-100 hover:border-amber-300'"
                >
                  <Clock class="w-6 h-6" />
                  <span class="text-xs font-medium">较困难答对</span>
                </button>

                <button
                  @click="handleQuality(5)"
                  class="flex flex-col items-center gap-1.5 p-3 rounded-xl transition-all cursor-pointer"
                  :class="theme === 'dark'
                    ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20 hover:border-emerald-500/50'
                    : 'bg-emerald-50 border border-emerald-200 text-emerald-500 hover:bg-emerald-100 hover:border-emerald-300'"
                >
                  <ThumbsUp class="w-6 h-6" />
                  <span class="text-xs font-medium">轻松记住</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>

    <!-- Word List View -->
    <template v-if="currentView === 'list'">
      <!-- Filters -->
      <div class="glass-card p-4">
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex items-center gap-2">
            <Filter class="w-4 h-4" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'" />
            <span class="text-sm font-medium" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">筛选:</span>
          </div>
          <select
            v-model="filterStatus"
            class="input-field text-sm py-1.5 px-3"
            style="width: auto;"
          >
            <option value="all">全部状态</option>
            <option value="learning">学习中</option>
            <option value="review">复习中</option>
            <option value="new">新学</option>
          </select>
          <select
            v-model="filterDifficulty"
            class="input-field text-sm py-1.5 px-3"
            style="width: auto;"
          >
            <option value="all">全部难度</option>
            <option value="1">简单</option>
            <option value="2">较易</option>
            <option value="3">中等</option>
            <option value="4">较难</option>
            <option value="5">困难</option>
          </select>
          <button
            @click="fetchAllWords"
            class="btn-secondary text-sm"
          >
            <RefreshCw class="w-4 h-4" />
            刷新
          </button>
        </div>
      </div>

      <!-- Word List -->
      <div v-if="allWords.length > 0" class="space-y-2">
        <div
          v-for="word in filteredWords"
          :key="word.userWordId"
          class="glass-card p-4 flex items-center justify-between transition-all hover:shadow-lg"
          :class="isOverdue(word) ? 'ring-1 ring-red-500/30' : ''"
        >
          <div class="flex items-center gap-4">
            <div
              class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0"
              :class="getStatusBgClass(word.status)"
            >
              <BookOpen class="w-5 h-5" :class="getStatusTextClass(word.status)" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h3 class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ word.word }}</h3>
              </div>
              <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ word.definitionCn }}</p>
            </div>
          </div>
          <div class="flex items-center gap-4">
            <div class="text-right">
              <div class="flex items-center gap-2">
                <span class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">掌握度:</span>
                <div class="flex items-center gap-0.5">
                  <span
                    v-for="i in 5"
                    :key="i"
                    class="w-2 h-2 rounded-full"
                    :class="i <= Math.round(word.masteryLevel || 0) * 5 ? 'bg-amber-400' : (theme === 'dark' ? 'bg-slate-600' : 'bg-gray-300')"
                  ></span>
                </div>
              </div>
              <div class="flex items-center gap-2 mt-1">
                <span class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">间隔:</span>
                <span class="text-xs font-medium" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ word.intervalDays || 0 }}天</span>
                <span class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">|</span>
                <span class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">复习:</span>
                <span class="text-xs font-medium" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ word.reviewCount }}次</span>
              </div>
            </div>
            <span
              class="px-2 py-1 rounded-lg text-xs font-medium shrink-0"
              :class="getStatusBadgeClass(word.status)"
            >
              {{ getStatusLabel(word.status) }}
            </span>
          </div>
        </div>
      </div>

      <div v-else class="flex flex-col items-center justify-center h-64 text-center">
        <BookOpen class="w-12 h-12 mb-3 opacity-50" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'" />
        <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">暂无学习中的单词</p>
        <router-link to="/learn" class="mt-3 text-sm text-amber-500 hover:text-amber-400">
          开始学习新词
        </router-link>
      </div>
    </template>

    <!-- History View -->
    <template v-if="currentView === 'history'">
      <!-- Time Range Selector -->
      <div class="glass-card p-4">
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex items-center gap-2">
            <Calendar class="w-4 h-4" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'" />
            <span class="text-sm font-medium" :class="theme === 'dark' ? 'text-slate-300' : 'text-gray-700'">时间范围:</span>
          </div>
          <div class="flex items-center gap-2">
            <button
              v-for="range in timeRanges"
              :key="range.value"
              @click="selectedRange = range.value; fetchHistory()"
              class="px-3 py-1.5 rounded-lg text-sm font-medium transition-all"
              :class="selectedRange === range.value
                ? 'bg-amber-500 text-white'
                : (theme === 'dark' ? 'bg-slate-800 text-slate-300 hover:bg-slate-700' : 'bg-gray-100 text-gray-700 hover:bg-gray-200')"
            >
              {{ range.label }}
            </button>
          </div>
          <button
            @click="fetchHistory()"
            class="btn-secondary text-sm ml-auto"
          >
            <RefreshCw class="w-4 h-4" />
            刷新
          </button>
        </div>
      </div>

      <!-- History Stats -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div class="glass-card p-4">
          <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">复习总数</p>
          <p class="text-3xl font-bold mt-1" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ history.length }}</p>
        </div>
        <div class="glass-card p-4">
          <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">正确率</p>
          <p class="text-3xl font-bold mt-1 text-emerald-400">{{ historyAccuracy }}%</p>
        </div>
        <div class="glass-card p-4">
          <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">平均质量</p>
          <p class="text-3xl font-bold mt-1 text-amber-400">{{ avgQuality }}</p>
        </div>
      </div>

      <!-- History List -->
      <div v-if="history.length > 0" class="space-y-2 max-h-96 overflow-y-auto">
        <div
          v-for="review in history"
          :key="review.id"
          class="glass-card p-4 flex items-center justify-between"
        >
          <div class="flex items-center gap-4">
            <div
              class="w-10 h-10 rounded-full flex items-center justify-center shrink-0"
              :class="review.correct ? 'bg-emerald-500/20' : 'bg-red-500/20'"
            >
              <CheckCircle2 v-if="review.correct" class="w-5 h-5 text-emerald-400" />
              <XCircle v-else class="w-5 h-5 text-red-400" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h3 class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ review.word }}</h3>
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="getQualityClass(review.quality)"
                >
                  {{ getQualityLabel(review.quality) }}
                </span>
              </div>
              <div class="flex items-center gap-3 mt-1">
                <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">{{ review.definitionCn }}</p>
                <span class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">|</span>
                <span class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">{{ formatDateTime(review.reviewedAt) }}</span>
              </div>
            </div>
          </div>
          <div class="text-right">
            <div class="text-xs" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'">掌握度变化</div>
            <div class="flex items-center gap-1 mt-1">
              <span class="text-sm font-medium" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-600'">{{ formatMastery(review.prevMastery) }}</span>
              <ArrowRight class="w-3 h-3" :class="review.newMastery > (review.prevMastery || 0) ? 'text-emerald-400' : 'text-red-400'" />
              <span class="text-sm font-medium" :class="review.newMastery > (review.prevMastery || 0) ? 'text-emerald-400' : 'text-red-400'">{{ formatMastery(review.newMastery) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="flex flex-col items-center justify-center h-64 text-center">
        <History class="w-12 h-12 mb-3 opacity-50" :class="theme === 'dark' ? 'text-slate-500' : 'text-gray-400'" />
        <p class="text-sm" :class="theme === 'dark' ? 'text-slate-400' : 'text-gray-500'">暂无复习历史记录</p>
        <button @click="currentView = 'review'" class="mt-3 btn-primary text-sm">
          开始复习
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useTheme } from '../composables/useTheme'
import {
  RotateCcw,
  CheckCircle2,
  XCircle,
  Clock,
  AlertTriangle,
  ThumbsUp,
  ThumbsDown,
  List,
  History,
  Target,
  Filter,
  RefreshCw,
  BookOpen,
  Calendar,
  ArrowRight
} from 'lucide-vue-next'
import { api } from '../services/api'
import { useUserStore } from '../stores/user'

const { theme } = useTheme()
const userStore = useUserStore()

const currentView = ref('review')
const dueWords = ref([])
const allWords = ref([])
const currentIndex = ref(0)
const showAnswer = ref(false)
const sessionId = ref(null)
const reviews = ref([])
const stats = ref({ correct: 0, wrong: 0, total: 0 })
const finished = ref(false)
const loading = ref(true)

const filterStatus = ref('all')
const filterDifficulty = ref('all')
const selectedRange = ref(7)
const history = ref([])

const reviewStats = ref({
  totalReviews: 0,
  correctReviews: 0,
  accuracy: 0,
  avgQuality: 0,
  dailyStats: []
})

const timeRanges = [
  { label: '7天', value: 7 },
  { label: '30天', value: 30 },
  { label: '90天', value: 90 },
  { label: '全部', value: 365 }
]

const currentWord = computed(() => {
  if (currentIndex.value < dueWords.value.length) {
    return dueWords.value[currentIndex.value]
  }
  return null
})

const accuracy = computed(() => {
  if (stats.value.total === 0) return 0
  return Math.round((stats.value.correct / stats.value.total) * 100)
})

const historyAccuracy = computed(() => {
  const correct = history.value.filter(h => h.correct).length
  if (history.value.length === 0) return 0
  return Math.round((correct / history.value.length) * 100)
})

const avgQuality = computed(() => {
  const sum = history.value.reduce((acc, h) => acc + (h.quality || 0), 0)
  if (history.value.length === 0) return 0
  return (sum / history.value.length).toFixed(1)
})

const filteredWords = computed(() => {
  return allWords.value.filter(word => {
    if (filterStatus.value !== 'all' && word.status !== filterStatus.value) return false
    if (filterDifficulty.value !== 'all' && String(word.difficulty) !== filterDifficulty.value) return false
    return true
  })
})

function isOverdue(word) {
  if (!word.nextReview) return false
  const nextReview = new Date(word.nextReview)
  return nextReview < new Date()
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

function formatMastery(mastery) {
  if (!mastery) return '0.00'
  return mastery.toFixed(2)
}

function getStatusBgClass(status) {
  switch (status) {
    case 'learning': return 'bg-blue-500/20'
    case 'review': return 'bg-orange-500/20'
    case 'new': return 'bg-amber-500/20'
    case 'mastered': return 'bg-emerald-500/20'
    default: return 'bg-slate-500/20'
  }
}

function getStatusTextClass(status) {
  switch (status) {
    case 'learning': return 'text-blue-400'
    case 'review': return 'text-orange-400'
    case 'new': return 'text-amber-400'
    case 'mastered': return 'text-emerald-400'
    default: return theme.value === 'dark' ? 'text-slate-400' : 'text-gray-400'
  }
}

function getStatusBadgeClass(status) {
  switch (status) {
    case 'learning': return theme.value === 'dark' ? 'bg-blue-500/15 text-blue-400' : 'bg-blue-50 text-blue-600'
    case 'review': return theme.value === 'dark' ? 'bg-orange-500/15 text-orange-400' : 'bg-orange-50 text-orange-600'
    case 'new': return theme.value === 'dark' ? 'bg-amber-500/15 text-amber-400' : 'bg-amber-50 text-amber-600'
    case 'mastered': return theme.value === 'dark' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-emerald-50 text-emerald-600'
    default: return theme.value === 'dark' ? 'bg-slate-500/15 text-slate-400' : 'bg-gray-100 text-gray-600'
  }
}

function getStatusLabel(status) {
  switch (status) {
    case 'learning': return '学习中'
    case 'review': return '复习中'
    case 'new': return '新学'
    case 'mastered': return '已掌握'
    default: return status
  }
}

function getQualityClass(quality) {
  if (!quality) return theme.value === 'dark' ? 'bg-slate-500/15 text-slate-400' : 'bg-gray-200 text-gray-500'
  if (quality >= 4) return 'bg-emerald-500/15 text-emerald-400'
  if (quality === 3) return 'bg-amber-500/15 text-amber-400'
  return 'bg-red-500/15 text-red-400'
}

function getQualityLabel(quality) {
  if (!quality) return '-'
  if (quality >= 4) return '轻松'
  if (quality === 3) return '较难'
  if (quality === 2) return '答错'
  return '忘记'
}

const dueWordsCount = ref(0)

async function fetchDueWords() {
  loading.value = true
  try {
    const [dueRes, sessionRes] = await Promise.all([
      api.get('/api/learning/due-words', { params: { limit: 20 } }),
      api.post('/api/learning/start-session', { sessionType: 'review' })
    ])

    const dueData = dueRes.data
    dueWords.value = Array.isArray(dueData) ? dueData : (dueData.words || [])
    dueWordsCount.value = dueData.count || dueWords.value.length

    const sessionData = sessionRes.data
    sessionId.value = sessionData.sessionId || sessionData.session_id || sessionData.id || null
  } catch (err) {
    console.error('Failed to fetch due words or start session:', err)
    dueWords.value = []
    dueWordsCount.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchAllWords() {
  try {
    const res = await api.get('/api/learning/learned-words', { params: { limit: 200 } })
    allWords.value = Array.isArray(res.data) ? res.data : (res.data.words || [])
  } catch (err) {
    console.error('Failed to fetch all words:', err)
    allWords.value = []
  }
}

async function fetchHistory() {
  try {
    const res = await api.get('/api/learning/review-history', { params: { days: selectedRange.value, limit: 50 } })
    history.value = res.data.history || []
  } catch (err) {
    console.error('Failed to fetch review history:', err)
    history.value = []
  }
}

async function fetchReviewStats() {
  try {
    const res = await api.get('/api/learning/review-stats', { params: { days: 7 } })
    reviewStats.value = res.data
  } catch (err) {
    console.error('Failed to fetch review stats:', err)
  }
}

function handleQuality(quality) {
  const word = currentWord.value
  if (!word) return

  reviews.value.push({
    userWordId: word.userWordId || word.id,
    word: word.word,
    quality,
    timestamp: new Date().toISOString()
  })

  stats.value.total++
  if (quality >= 3) {
    stats.value.correct++
  } else {
    stats.value.wrong++
  }

  if (currentIndex.value < dueWords.value.length - 1) {
    currentIndex.value++
    showAnswer.value = false
  } else {
    submitReviews()
  }
}

async function submitReviews() {
  try {
    await api.post('/api/learning/submit-review', {
      sessionId: sessionId.value,
      reviews: reviews.value
    })
    // Refresh streak in user store after submitting reviews
    try {
      const streakRes = await api.get('/api/dashboard/streak-calculation')
      if (streakRes.data.currentStreak !== undefined) {
        userStore.updateStreak(streakRes.data.currentStreak)
      }
    } catch (e) {
      // Non-critical, ignore silently
    }
  } catch (err) {
    console.error('Failed to submit reviews:', err)
  } finally {
    finished.value = true
    fetchReviewStats()
    fetchHistory()
  }
}

async function restartReview() {
  dueWords.value = []
  currentIndex.value = 0
  showAnswer.value = false
  sessionId.value = null
  reviews.value = []
  stats.value = { correct: 0, wrong: 0, total: 0 }
  finished.value = false
  loading.value = true

  await fetchDueWords()
}

onMounted(() => {
  fetchDueWords()
  fetchAllWords()
  fetchHistory()
  fetchReviewStats()
})
</script>
