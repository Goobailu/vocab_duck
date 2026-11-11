<template>
  <div class="min-h-screen flex flex-col" :class="theme === 'dark' ? 'bg-gray-900' : 'bg-gray-50'">
    <div v-if="currentView === 'main'" class="flex-1 flex flex-col items-center justify-center p-6">
      <div class="w-24 h-24 rounded-3xl bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center mb-8 shadow-xl shadow-amber-500/30">
        <Bot class="w-12 h-12 text-white" />
      </div>
      <h1 class="text-3xl font-bold mb-3" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">多智能体词汇学习助手</h1>
      <p class="text-center mb-10 max-w-md" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">多个 AI 专家协作，帮助你高效掌握词汇</p>
      
      <div class="flex flex-col gap-4 w-full max-w-sm">
        <button
          @click="startNewConversation"
          class="flex items-center justify-center gap-3 px-6 py-4 rounded-2xl text-lg font-semibold transition-all duration-300 hover:scale-105"
          :class="theme === 'dark'
            ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white hover:shadow-lg hover:shadow-amber-500/30'
            : 'bg-gradient-to-r from-amber-500 to-orange-500 text-white hover:shadow-lg hover:shadow-amber-500/30'"
        >
          <MessageSquarePlus class="w-6 h-6" />
          新对话
        </button>
        
        <button
          @click="viewHistory"
          class="flex items-center justify-center gap-3 px-6 py-4 rounded-2xl text-lg font-semibold transition-all duration-300 hover:scale-105"
          :class="theme === 'dark'
            ? 'bg-gray-800 text-gray-300 hover:bg-gray-700 border border-gray-700'
            : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-200 shadow-sm'"
        >
          <History class="w-6 h-6" />
          查看历史
        </button>
      </div>
    </div>

    <div v-else class="flex flex-col h-screen">
      <div class="flex items-center justify-between px-6 py-4 border-b backdrop-blur-sm"
           :class="theme === 'dark' ? 'border-gray-800 bg-gray-900/80' : 'border-gray-200 bg-white/80'">
        <div class="flex items-center gap-3">
          <button
            @click="goBack"
            class="flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm transition-all"
            :class="theme === 'dark'
              ? 'hover:bg-gray-800 text-gray-400 hover:text-white'
              : 'hover:bg-gray-100 text-gray-500 hover:text-gray-700'"
          >
            <ArrowLeft class="w-4 h-4" />
            返回
          </button>
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center">
            <Bot class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-lg font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
              {{ currentView === 'history' ? '聊天历史' : '智能对话' }}
            </h1>
            <p class="text-xs" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">
              {{ currentView === 'history' ? '最近两次对话记录' : '与AI助手对话中' }}
            </p>
          </div>
        </div>
        <button
          v-if="currentView === 'chat' && messages.length > 0"
          @click="resetConversation"
          class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200"
          :class="theme === 'dark'
            ? 'bg-gray-800 text-gray-300 hover:bg-gray-700 hover:text-white border border-gray-700'
            : 'bg-gray-100 text-gray-600 hover:bg-gray-200 hover:text-gray-800 border border-gray-200'"
        >
          <RotateCcw class="w-4 h-4" />
          重新对话
        </button>
      </div>

      <div ref="messagesContainer" class="flex-1 overflow-y-auto px-4 py-6 space-y-6">
        <div v-if="currentView === 'history'" class="max-w-4xl mx-auto">
          <div v-if="historyConversations.length === 0" class="text-center py-16">
            <History class="w-16 h-16 mx-auto mb-4" :class="theme === 'dark' ? 'text-gray-600' : 'text-gray-300'" />
            <p :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">暂无聊天记录</p>
          </div>
          <div v-else class="space-y-6">
            <div
              v-for="(conversation, convIdx) in historyConversations"
              :key="convIdx"
              class="rounded-2xl p-6"
              :class="theme === 'dark' ? 'bg-gray-800/50 border border-gray-700/50' : 'bg-white border border-gray-200 shadow-sm'"
            >
              <div class="flex items-center justify-between mb-4">
                <span class="text-sm font-medium" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">
                  对话 {{ historyConversations.length - convIdx }} · {{ formatDate(conversation.time) }}
                </span>
                <button
                  @click="loadConversation(conversation)"
                  class="text-xs px-3 py-1 rounded-lg transition-all"
                  :class="theme === 'dark'
                    ? 'bg-amber-500/20 text-amber-400 hover:bg-amber-500/30'
                    : 'bg-amber-50 text-amber-600 hover:bg-amber-100'"
                >
                  继续对话
                </button>
              </div>
              <div class="space-y-3">
                <div
                  v-for="(msg, msgIdx) in conversation.messages"
                  :key="msgIdx"
                  :class="[
                    'flex gap-3',
                    msg.role === 'user' ? 'justify-end' : 'justify-start'
                  ]"
                >
                  <div
                    v-if="msg.role === 'assistant'"
                    class="w-8 h-8 rounded-lg bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center flex-shrink-0"
                  >
                    <Bot class="w-4 h-4 text-white" />
                  </div>
                  <div :class="['flex flex-col max-w-[70%]', msg.role === 'user' ? 'items-end' : 'items-start']">
                    <div
                      v-if="msg.agents && msg.agents.length > 0"
                      class="flex flex-wrap gap-1 mb-1"
                    >
                      <span
                        v-for="agent in msg.agents"
                        :key="agent"
                        class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md text-[10px] font-medium"
                        :class="theme === 'dark' ? 'bg-gray-800 border border-gray-700 text-gray-400' : 'bg-white border border-gray-200 text-gray-500'"
                      >
                        {{ getAgentLabel(agent) }}
                      </span>
                    </div>
                    <div
                      v-if="msg.content"
                      :class="[
                        'rounded-xl px-4 py-2 text-sm',
                        msg.role === 'user'
                          ? 'chat-bubble-user rounded-tr-sm'
                          : 'chat-bubble-bot rounded-tl-sm'
                      ]"
                    >
                      {{ msg.content }}
                    </div>
                    
                    <div v-if="msg.cards && msg.cards.length > 0" class="mt-2 space-y-2">
                      <div v-for="(card, cIdx) in msg.cards" :key="cIdx">
                        <div
                          v-if="card.type === 'word_card' || card.type === 'review_card'"
                          class="glass-card p-3 space-y-2"
                        >
                          <div class="flex items-baseline justify-between gap-2">
                            <div class="flex items-baseline gap-2">
                              <span class="text-xs font-medium px-1.5 py-0.5 rounded-full" :class="theme === 'dark' ? 'bg-gray-800 text-gray-400' : 'bg-gray-100 text-gray-500'">{{ cIdx + 1 }}</span>
                              <h4 class="text-lg font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ card.word }}</h4>
                              <span v-if="card.phonetic" class="text-xs italic" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-400'">{{ card.phonetic }}</span>
                            </div>
                          </div>
                          <div v-if="card.definitions && card.definitions.length > 0" class="space-y-1">
                            <div
                              v-for="(def, dIdx) in card.definitions"
                              :key="dIdx"
                              class="text-xs flex gap-2"
                              :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-600'"
                            >
                              <span v-if="def.pos" class="text-amber-500 font-medium flex-shrink-0">{{ def.pos }}.</span>
                              <span>{{ def.cn || def.meaning || def }}</span>
                            </div>
                          </div>
                        </div>
                        
                        <div
                          v-else-if="card.type === 'stats' || card.type === 'recommendations'"
                          class="glass-card p-3"
                        >
                          <h4 v-if="card.title" class="text-sm font-semibold mb-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
                            {{ card.title }}
                          </h4>
                          <div v-if="card.data && typeof card.data === 'object' && !Array.isArray(card.data)" class="space-y-1">
                            <div v-for="(value, key) in card.data" :key="key" class="text-xs flex justify-between">
                              <span :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">{{ formatKey(key) }}</span>
                              <span class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ value }}</span>
                            </div>
                          </div>
                        </div>
                        
                        <div
                          v-else-if="card.type === 'quiz_card'"
                          class="glass-card p-3"
                        >
                          <div class="text-xs font-medium mb-1" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">测验</div>
                          <div class="text-sm font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ card.question }}</div>
                        </div>
                        
                        <div
                          v-else-if="card.type === 'welcome'"
                          class="glass-card p-3"
                        >
                          <h4 class="text-sm font-semibold mb-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ card.title }}</h4>
                          <p class="text-xs" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">{{ card.content }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div
                    v-if="msg.role === 'user'"
                    class="w-8 h-8 rounded-lg bg-amber-500 flex items-center justify-center flex-shrink-0"
                  >
                    <User class="w-4 h-4 text-white" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="currentView !== 'history'">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="[
              'flex gap-3 max-w-4xl mx-auto animate-in fade-in slide-in-from-bottom-2 duration-300',
              message.role === 'user' ? 'justify-end' : 'justify-start'
            ]"
          >
          <div
            v-if="message.role === 'assistant'"
            class="w-9 h-9 rounded-lg bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center flex-shrink-0 mt-1"
          >
            <Bot class="w-5 h-5 text-white" />
          </div>

          <div :class="['flex flex-col max-w-[80%]', message.role === 'user' ? 'items-end' : 'items-start']">
            <div
              v-if="message.role === 'assistant' && message.agents && message.agents.length > 0"
              class="flex flex-wrap gap-1.5 mb-2"
            >
              <span
                v-for="agent in message.agents"
                :key="agent"
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium"
                :class="theme === 'dark' ? 'bg-gray-800 border border-gray-700 text-gray-300' : 'bg-white border border-gray-200 text-gray-600 shadow-sm'"
              >
                <component :is="getAgentIcon(agent)" class="w-3 h-3" />
                {{ getAgentLabel(agent) }}
              </span>
            </div>

            <!-- 文本气泡 -->
            <div
              v-if="message.content"
              :class="[
                'rounded-2xl px-4 py-3 text-sm leading-relaxed transition-all duration-300',
                message.role === 'user'
                  ? 'chat-bubble-user rounded-tr-sm'
                  : 'chat-bubble-bot rounded-tl-sm',
                ''
              ]"
            >
              <div class="whitespace-pre-wrap">{{ message.content }}<span v-if="message.streaming" class="streaming-cursor">|</span></div>
            </div>

            <!-- 卡片区域 -->
            <div v-if="message.cards && message.cards.length > 0" class="mt-3 space-y-3 w-full">
              <div class="flex items-center justify-between px-1">
                <span class="text-xs font-medium" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">
                  共 {{ message.cards.length }} 个复习单词
                </span>
              </div>
              
              <div v-for="(card, cIdx) in message.cards" :key="cIdx">

                <!-- ═══ 单词卡 / 复习卡 ═══ -->
                <div
                  v-if="card.type === 'word_card' || card.type === 'review_card'"
                  class="glass-card p-5 space-y-3"
                >
                  <div class="flex items-baseline justify-between gap-3 flex-wrap">
                    <div class="flex items-baseline gap-3 flex-wrap">
                      <span class="text-xs font-medium px-2 py-0.5 rounded-full" :class="theme === 'dark' ? 'bg-gray-800 text-gray-400' : 'bg-gray-100 text-gray-500'">{{ cIdx + 1 }}</span>
                      <h3 class="text-xl font-bold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ card.word }}</h3>
                      <span v-if="card.phonetic" class="text-sm italic" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">{{ card.phonetic }}</span>
                    </div>
                    <div class="flex items-center gap-3">
                      <div v-if="card.masteryLevel !== undefined" class="flex items-center gap-1.5">
                        <span class="text-xs" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-400'">掌握度</span>
                        <div class="w-16 h-1.5 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-gray-700' : 'bg-gray-200'">
                          <div
                            class="h-full rounded-full transition-all duration-500"
                            :class="card.masteryLevel >= 80 ? 'bg-emerald-500' : card.masteryLevel >= 50 ? 'bg-amber-500' : 'bg-rose-500'"
                            :style="{ width: `${Math.min(100, card.masteryLevel)}%` }"
                          />
                        </div>
                        <span class="text-xs font-medium" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">{{ card.masteryLevel }}%</span>
                      </div>
                      <div v-if="card.progress !== undefined" class="flex items-center gap-2">
                        <div class="w-24 h-2 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-gray-700' : 'bg-gray-200'">
                          <div
                            class="h-full rounded-full transition-all duration-500"
                            :class="card.progress >= 80 ? 'bg-emerald-500' : card.progress >= 50 ? 'bg-amber-500' : 'bg-rose-500'"
                            :style="{ width: `${Math.min(100, card.progress)}%` }"
                          />
                        </div>
                        <span class="text-xs font-medium" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">{{ Math.min(100, card.progress) }}%</span>
                      </div>
                    </div>
                  </div>

                  <div v-if="card.definitions && card.definitions.length > 0" class="space-y-1.5">
                    <div
                      v-for="(def, dIdx) in card.definitions"
                      :key="dIdx"
                      class="text-sm flex gap-2"
                      :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-700'"
                    >
                      <span v-if="def.pos" class="text-amber-500 font-medium flex-shrink-0">{{ def.pos }}.</span>
                      <div>
                        <span class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ def.en }}</span>
                        <span class="ml-2" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">{{ def.cn || def.meaning || def }}</span>
                      </div>
                    </div>
                  </div>

                  <div
                    v-if="card.mnemonic"
                    class="mt-3 p-3 rounded-lg"
                    :class="theme === 'dark' ? 'bg-amber-500/10 border border-amber-500/30' : 'bg-amber-50 border border-amber-200'"
                  >
                    <div class="flex items-center gap-1.5 text-amber-500 font-medium text-xs mb-1">
                      <Lightbulb class="w-3.5 h-3.5" />
                      记忆技巧 ({{ card.mnemonic.method || '联想' }})
                    </div>
                    <span class="text-sm" :class="theme === 'dark' ? 'text-amber-100' : 'text-amber-800'">{{ card.mnemonic.content || card.mnemonic }}</span>
                  </div>

                  <div v-if="card.examples && card.examples.length > 0" class="space-y-2 pt-2">
                    <div class="text-xs font-medium uppercase tracking-wide" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">例句</div>
                    <div
                      v-for="(ex, eIdx) in card.examples"
                      :key="eIdx"
                      class="text-sm pl-3"
                      :class="theme === 'dark' ? 'text-gray-300 border-l-2 border-gray-700' : 'text-gray-600 border-l-2 border-gray-300'"
                    >
                      <div>{{ ex.en || ex.sentence || ex }}</div>
                      <div v-if="ex.cn || ex.zh || ex.translation" class="text-xs mt-0.5" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-400'">
                        {{ ex.cn || ex.zh || ex.translation }}
                      </div>
                    </div>
                  </div>

                  <div v-if="card.collocations && card.collocations.length > 0" class="pt-1">
                    <div class="text-xs font-medium uppercase tracking-wide mb-1.5" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">常用搭配</div>
                    <div class="flex flex-wrap gap-1.5">
                      <span
                        v-for="(col, colIdx) in card.collocations"
                        :key="colIdx"
                        class="px-2 py-0.5 text-xs rounded-md"
                        :class="theme === 'dark' ? 'bg-amber-500/10 border border-amber-500/20 text-amber-200' : 'bg-amber-50 border border-amber-200 text-amber-700'"
                      >{{ col }}</span>
                    </div>
                  </div>

                  <div v-if="card.synonyms && card.synonyms.length > 0" class="pt-1">
                    <div class="text-xs font-medium uppercase tracking-wide mb-1.5" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">近义词</div>
                    <div class="flex flex-wrap gap-1.5">
                      <span
                        v-for="(syn, synIdx) in card.synonyms"
                        :key="synIdx"
                        class="px-2 py-0.5 text-xs rounded-md"
                        :class="theme === 'dark' ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-200' : 'bg-emerald-50 border border-emerald-200 text-emerald-700'"
                      >{{ syn.word || syn }} <span v-if="syn.diff" class="opacity-60">({{ syn.diff }})</span></span>
                    </div>
                  </div>

                  <div
                    v-if="card.exam_tips"
                    class="p-3 rounded-lg"
                    :class="theme === 'dark' ? 'bg-rose-500/10 border border-rose-500/30' : 'bg-rose-50 border border-rose-200'"
                  >
                    <div class="flex items-center gap-1.5 text-rose-400 font-medium text-xs mb-1">
                      <Lightbulb class="w-3.5 h-3.5" />
                      备考提示
                    </div>
                    <span class="text-sm" :class="theme === 'dark' ? 'text-rose-100' : 'text-rose-800'">{{ card.exam_tips }}</span>
                  </div>

                  <div
                    v-if="card.mini_story"
                    class="p-4 rounded-xl border"
                    :class="theme === 'dark' ? 'bg-gradient-to-br from-indigo-500/15 via-purple-500/15 to-pink-500/15 border-indigo-500/30' : 'bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 border-indigo-200'"
                  >
                    <div class="text-xs font-medium mb-1.5 flex items-center gap-1.5"
                         :class="theme === 'dark' ? 'text-indigo-300' : 'text-indigo-600'">
                      <BookOpen class="w-3.5 h-3.5" />
                      {{ card.mini_story.title || '小故事' }}
                    </div>
                    <p class="text-sm italic leading-relaxed" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-700'">{{ card.mini_story.content }}</p>
                  </div>

                  <div
                    v-if="card.image_prompt"
                    class="p-4 rounded-xl border"
                    :class="theme === 'dark' ? 'bg-gradient-to-br from-purple-500/20 via-pink-500/20 to-orange-500/20 border-purple-500/30' : 'bg-gradient-to-br from-purple-50 via-pink-50 to-orange-50 border-purple-200'"
                  >
                    <div class="text-xs font-medium mb-1.5 flex items-center gap-1.5"
                         :class="theme === 'dark' ? 'text-purple-300' : 'text-purple-600'">
                      <BookOpen class="w-3.5 h-3.5" />
                      联想画面
                    </div>
                    <p class="text-sm italic" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-700'">{{ card.image_prompt }}</p>
                  </div>
                </div>

                <!-- ═══ 统计卡 / 建议卡（含遗忘曲线 SVG）═══ -->
                <div
                  v-else-if="card.type === 'stats' || card.type === 'recommendations'"
                  class="glass-card p-5"
                >
                  <h3 v-if="card.title" class="text-base font-semibold mb-3 flex items-center gap-2"
                      :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
                    <Brain v-if="card.type === 'stats'" class="w-4 h-4 text-amber-500" />
                    <Lightbulb v-else class="w-4 h-4 text-amber-500" />
                    {{ card.title }}
                  </h3>

                  <!-- 数据指标网格 -->
                  <div
                    v-if="card.data && typeof card.data === 'object' && !Array.isArray(card.data)"
                    class="grid grid-cols-2 sm:grid-cols-3 gap-3"
                  >
                    <div
                      v-for="(value, key) in card.data"
                      :key="key"
                      class="p-3 rounded-lg"
                      :class="theme === 'dark' ? 'bg-gray-800/60 border border-gray-700/50' : 'bg-gray-50 border border-gray-200'"
                    >
                      <div class="flex items-center justify-between mb-1.5">
                        <div class="text-xs" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">{{ formatKey(key) }}</div>
                        <TrendingUp v-if="isNumeric(value)" class="w-3 h-3" :class="theme === 'dark' ? 'text-emerald-400' : 'text-emerald-500'" />
                      </div>
                      <div class="flex items-baseline gap-2">
                        <div class="text-lg font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ value }}</div>
                        <span v-if="key.includes('Percent') || key.includes('percent') || key.includes('rate')" class="text-xs" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-400'">%</span>
                      </div>
                      <div v-if="isNumeric(value) && Number(value) <= 100 && Number(value) > 0" class="mt-2">
                        <div class="w-full h-1.5 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-gray-700' : 'bg-gray-200'">
                          <div
                            class="h-full rounded-full transition-all duration-700"
                            :class="Number(value) >= 80 ? 'bg-emerald-500' : Number(value) >= 50 ? 'bg-amber-500' : 'bg-rose-500'"
                            :style="{ width: `${Number(value)}%` }"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 遗忘曲线 SVG 图表 -->
                  <div v-if="card.curvePoints && card.curvePoints.length > 0" class="mt-4">
                    <div class="text-xs font-medium uppercase tracking-wide mb-2" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">遗忘曲线 (预测 vs 实际)</div>
                    <div class="rounded-lg p-3" :class="theme === 'dark' ? 'bg-gray-800/60 border border-gray-700/50' : 'bg-gray-50 border border-gray-200'">
                      <svg viewBox="0 0 300 160" class="w-full" style="max-height: 180px;">
                        <!-- 网格线 -->
                        <line x1="30" y1="10" x2="30" y2="130" :stroke="theme === 'dark' ? '#374151' : '#e5e7eb'" stroke-width="0.5"/>
                        <line x1="30" y1="130" x2="290" y2="130" :stroke="theme === 'dark' ? '#374151' : '#e5e7eb'" stroke-width="0.5"/>
                        <line x1="30" y1="70" x2="290" y2="70" :stroke="theme === 'dark' ? '#374151' : '#e5e7eb'" stroke-width="0.5" stroke-dasharray="4"/>
                        <line x1="30" y1="10" x2="290" y2="10" :stroke="theme === 'dark' ? '#374151' : '#e5e7eb'" stroke-width="0.5" stroke-dasharray="4"/>
                        <!-- Y轴标签 -->
                        <text x="4" y="14" font-size="8" :fill="theme === 'dark' ? '#9ca3af' : '#6b7280'">100%</text>
                        <text x="10" y="74" font-size="8" :fill="theme === 'dark' ? '#9ca3af' : '#6b7280'">50%</text>
                        <text x="14" y="134" font-size="8" :fill="theme === 'dark' ? '#9ca3af' : '#6b7280'">0%</text>
                        <!-- X轴标签 -->
                        <text x="30" y="148" font-size="8" :fill="theme === 'dark' ? '#9ca3af' : '#6b7280'">0天</text>
                        <text x="145" y="148" font-size="8" :fill="theme === 'dark' ? '#9ca3af' : '#6b7280'">14天</text>
                        <text x="260" y="148" font-size="8" :fill="theme === 'dark' ? '#9ca3af' : '#6b7280'">30天</text>
                        <!-- 预测曲线 (虚线灰色) -->
                        <polyline
                          :points="getCurvePath(card.curvePoints, 'predicted')"
                          fill="none"
                          :stroke="theme === 'dark' ? '#6b7280' : '#9ca3af'"
                          stroke-width="1.5"
                          stroke-dasharray="4 2"
                        />
                        <!-- 实际曲线 (实线渐变色) -->
                        <polyline
                          :points="getCurvePath(card.curvePoints, 'actual')"
                          fill="none"
                          stroke="#f59e0b"
                          stroke-width="2"
                          stroke-linejoin="round"
                        />
                        <!-- 实际数据点 -->
                        <circle
                          v-for="(pt, ptIdx) in card.curvePoints"
                          :key="ptIdx"
                          :cx="getCurveX(pt.day)"
                          :cy="getCurveY(pt.actual)"
                          r="3"
                          fill="#f59e0b"
                        />
                      </svg>
                      <div class="flex items-center gap-4 mt-2 text-[10px]" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">
                        <span class="flex items-center gap-1">
                          <span class="inline-block w-4 h-0.5 bg-amber-500 rounded"></span> 实际留存率
                        </span>
                        <span class="flex items-center gap-1">
                          <span class="inline-block w-4 h-0.5 rounded" :class="theme === 'dark' ? 'bg-gray-500' : 'bg-gray-400'" style="border-top: 1px dashed;"></span> 艾宾浩斯预测
                        </span>
                      </div>
                    </div>
                  </div>

                  <!-- 洞察文本 -->
                  <div v-if="card.insights && card.insights.length > 0" class="mt-3 space-y-1.5">
                    <div class="text-xs font-medium uppercase tracking-wide" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">分析洞察</div>
                    <div
                      v-for="(insight, iIdx) in card.insights"
                      :key="iIdx"
                      class="text-sm flex gap-2"
                      :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-700'"
                    >
                      <span class="text-amber-500 mt-0.5">•</span>
                      <span>{{ insight }}</span>
                    </div>
                  </div>

                  <!-- 建议列表 (recommendations) -->
                  <ul
                    v-if="card.type === 'recommendations' && Array.isArray(card.items || card.data)"
                    class="space-y-2 mt-2"
                  >
                    <li
                      v-for="(item, iIdx) in (card.items || card.data)"
                      :key="iIdx"
                      class="text-sm flex gap-2"
                      :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-700'"
                    >
                      <span class="text-amber-500 mt-0.5">•</span>
                      <span>{{ typeof item === 'string' ? item : item.text || item.content || JSON.stringify(item) }}</span>
                    </li>
                  </ul>
                </div>

                <!-- ═══ 复习计划卡 ═══ -->
                <div
                  v-else-if="card.type === 'schedule'"
                  class="glass-card p-5"
                >
                  <h3 class="text-base font-semibold mb-3 flex items-center gap-2"
                      :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
                    <RotateCcw class="w-4 h-4 text-emerald-400" />
                    {{ card.title || '今日计划' }}
                  </h3>
                  <div class="space-y-3">
                    <div
                      v-for="(task, tIdx) in (card.tasks || [])"
                      :key="tIdx"
                      class="p-3 rounded-lg border transition-all"
                      :class="[
                        theme === 'dark' ? 'bg-gray-800/60 border-gray-700/50' : 'bg-gray-50 border-gray-200',
                        task.priority === 'high' ? 'border-l-4 border-red-400' : '',
                        task.priority === 'medium' ? 'border-l-4 border-yellow-400' : '',
                        task.priority === 'low' ? 'border-l-4 border-blue-400' : ''
                      ]"
                    >
                      <div class="flex items-center justify-between mb-1">
                        <div class="flex items-center gap-2">
                          <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                                :class="{
                                  'bg-red-500/20 text-red-400': task.priority === 'high',
                                  'bg-yellow-500/20 text-yellow-400': task.priority === 'medium',
                                  'bg-blue-500/20 text-blue-400': task.priority === 'low',
                                  'bg-gray-500/20 text-gray-400': !task.priority
                                }">
                            {{ task.priority === 'high' ? '优先' : task.priority === 'medium' ? '普通' : task.priority === 'low' ? '次要' : '任务' }}
                          </span>
                          <span class="text-sm font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ task.taskName || task.title }}</span>
                        </div>
                        <span v-if="task.durationMinutes" class="text-xs" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-400'">
                          {{ task.durationMinutes }}分钟
                        </span>
                      </div>
                      <div class="text-xs" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">
                        {{ task.description || (task.wordCount ? `共${task.wordCount}个单词` : '') }}
                      </div>
                      <div v-if="task.wordCount || task.targetCount" class="mt-2 flex items-center gap-4 text-xs">
                        <span :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">
                          单词数：<span class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ task.wordCount || 0 }}</span>
                        </span>
                        <span v-if="task.targetCount" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">
                          目标：<span class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ task.targetCount }}</span>
                        </span>
                      </div>
                    </div>
                    <div v-if="!card.tasks || card.tasks.length === 0" class="text-center py-6">
                      <p class="text-sm" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">今天没有安排学习任务</p>
                      <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-400'">可以自由学习！</p>
                    </div>
                  </div>
                </div>

                <!-- ═══ 测验卡（含对错反馈）═══ -->
                <div
                  v-else-if="card.type === 'quiz'"
                  class="glass-card p-5"
                >
                  <h3 class="text-base font-semibold mb-4 flex items-center gap-2"
                      :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">
                    <BookOpen class="w-4 h-4 text-purple-400" />
                    {{ card.title || '词汇测验' }}
                  </h3>
                  <div class="space-y-4">
                    <div
                      v-for="(q, qIdx) in (card.questions || [])"
                      :key="qIdx"
                      class="p-3 rounded-lg"
                      :class="theme === 'dark' ? 'bg-gray-800/60 border border-gray-700/50' : 'bg-gray-50 border border-gray-200'"
                    >
                      <div class="text-sm mb-3" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-700'">
                        <span class="font-semibold" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ qIdx + 1 }}.</span>
                        {{ q.question || q.text }}
                      </div>
                      <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        <button
                          v-for="(opt, oIdx) in (q.options || [])"
                          :key="oIdx"
                          @click="handleQuizAnswer(index, cIdx, qIdx, oIdx, q)"
                          :disabled="isQuizAnswered(index, cIdx, qIdx)"
                          class="text-left px-3 py-2 rounded-md text-sm border transition-all"
                          :class="getQuizOptionClass(index, cIdx, qIdx, oIdx, q)"
                        >
                          <span class="font-medium mr-2" :class="isQuizAnswered(index, cIdx, qIdx) && oIdx === q.answer ? 'text-emerald-500' : (theme === 'dark' ? 'text-gray-200' : 'text-gray-500')">{{ optionLabel(oIdx) }}</span>
                          {{ typeof opt === 'string' ? opt : opt.text || opt.label }}
                          <span v-if="isQuizAnswered(index, cIdx, qIdx) && oIdx === q.answer" class="ml-1 text-emerald-500">&#10003;</span>
                          <span v-if="isQuizAnswered(index, cIdx, qIdx) && isQuizOptionSelected(index, cIdx, qIdx, oIdx) && oIdx !== q.answer" class="ml-1 text-rose-500">&#10007;</span>
                        </button>
                      </div>
                      <!-- 解释 -->
                      <div
                        v-if="isQuizAnswered(index, cIdx, qIdx) && q.explanation"
                        class="mt-2 px-3 py-2 rounded-md text-xs"
                        :class="isQuizCorrect(index, cIdx, qIdx)
                          ? (theme === 'dark' ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-300' : 'bg-emerald-50 border border-emerald-200 text-emerald-700')
                          : (theme === 'dark' ? 'bg-rose-500/10 border border-rose-500/30 text-rose-300' : 'bg-rose-50 border border-rose-200 text-rose-700')"
                      >
                        {{ isQuizCorrect(index, cIdx, qIdx) ? '回答正确！' : '回答错误。' }} {{ q.explanation }}
                      </div>
                    </div>
                  </div>
                  <!-- 测验成绩汇总 -->
                  <div
                    v-if="isQuizComplete(index, cIdx)"
                    class="mt-4 p-4 rounded-xl border text-center"
                    :class="theme === 'dark' ? 'bg-gradient-to-r from-amber-500/10 to-orange-500/10 border-amber-500/30' : 'bg-gradient-to-r from-amber-50 to-orange-50 border-amber-200'"
                  >
                    <div class="text-2xl font-bold mb-1" :class="theme === 'dark' ? 'text-amber-400' : 'text-amber-600'">
                      {{ getQuizScore(index, cIdx).correct }}/{{ getQuizScore(index, cIdx).total }}
                    </div>
                    <div class="text-sm" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-600'">
                      {{ getQuizScore(index, cIdx).correct === getQuizScore(index, cIdx).total ? '全部正确，太棒了！' : getQuizScore(index, cIdx).correct >= getQuizScore(index, cIdx).total / 2 ? '不错的成绩，继续加油！' : '还需多多练习哦！' }}
                    </div>
                  </div>
                </div>

                <!-- ═══ 配图卡（含 loading）═══ -->
                <div
                  v-else-if="card.type === 'image'"
                  class="p-5 rounded-xl border overflow-hidden"
                  :class="theme === 'dark' ? 'bg-gradient-to-br from-purple-500/20 via-pink-500/20 to-orange-500/20 border-purple-500/30' : 'bg-gradient-to-br from-purple-50 via-pink-50 to-orange-50 border-purple-200'"
                >
                  <div class="text-xs font-medium mb-3 flex items-center gap-1.5"
                       :class="theme === 'dark' ? 'text-purple-300' : 'text-purple-600'">
                    <Image class="w-3.5 h-3.5" />
                    {{ card.word ? `"${card.word}" 记忆配图` : '记忆配图' }}
                  </div>
                  <!-- Loading 状态 -->
                  <div
                    v-if="card.imageLoading"
                    class="flex flex-col items-center justify-center py-16 gap-3"
                  >
                    <Loader2 class="w-8 h-8 animate-spin text-purple-400" />
                    <span class="text-sm" :class="theme === 'dark' ? 'text-gray-200' : 'text-gray-500'">正在生成配图中...</span>
                  </div>
                  <!-- 图片展示 -->
                  <div v-else-if="card.imageUrl" class="rounded-lg overflow-hidden">
                    <img
                      :src="card.imageUrl"
                      :alt="card.word || '记忆配图'"
                      class="w-full rounded-lg"
                      style="max-height: 512px; object-fit: cover;"
                      @error="card.imageUrl = ''; card.imageLoading = false"
                    />
                  </div>
                  <!-- 加载失败兜底 -->
                  <div v-else class="text-center py-8">
                    <span class="text-sm" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-400'">图片加载失败，请重试</span>
                  </div>
                </div>

                <!-- ═══ 欢迎卡片 ═══ -->
                <div
                  v-else-if="card.type === 'welcome'"
                  class="p-5 rounded-xl border"
                  :class="theme === 'dark' ? 'bg-gradient-to-br from-amber-500/20 to-orange-500/20 border-amber-500/30' : 'bg-gradient-to-br from-amber-50 to-orange-50 border-amber-200'"
                >
                  <div class="text-xs font-medium mb-4 flex items-center gap-1.5"
                       :class="theme === 'dark' ? 'text-amber-300' : 'text-amber-600'">
                    <Lightbulb class="w-3.5 h-3.5" />
                    {{ card.title || '智能助手' }}
                  </div>
                  <p class="font-medium mb-4" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ card.content }}</p>
                  <ul v-if="Array.isArray(card.items)" class="space-y-2 mb-4">
                    <li v-for="(item, idx) in card.items" :key="idx" class="flex items-start gap-2">
                      <span class="flex-shrink-0 w-5 h-5 rounded-full bg-amber-500/20 text-amber-500 text-xs font-medium flex items-center justify-center mt-0.5">{{ idx + 1 }}</span>
                      <span><span class="font-medium" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ item.label }}</span> — {{ item.desc }}</span>
                    </li>
                  </ul>
                  <div v-if="Array.isArray(card.suggestions)" class="flex flex-wrap gap-2">
                    <button
                      v-for="(suggestion, sIdx) in card.suggestions"
                      :key="sIdx"
                      @click="inputText = suggestion; sendMessage()"
                      class="px-3 py-1.5 rounded-full text-xs font-medium transition-all hover:scale-105"
                      :class="theme === 'dark' ? 'bg-gray-800 text-gray-300 hover:bg-gray-700 border border-gray-700' : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200 shadow-sm'"
                    >
                      {{ suggestion }}
                    </button>
                  </div>
                </div>

                <!-- ═══ 联想画面文本卡 ═══ -->
                <div
                  v-else-if="card.type === 'image_prompt'"
                  class="p-5 rounded-xl border"
                  :class="theme === 'dark' ? 'bg-gradient-to-br from-purple-500/20 via-pink-500/20 to-orange-500/20 border-purple-500/30' : 'bg-gradient-to-br from-purple-50 via-pink-50 to-orange-50 border-purple-200'"
                >
                  <div class="text-xs font-medium mb-2 flex items-center gap-1.5"
                       :class="theme === 'dark' ? 'text-purple-300' : 'text-purple-600'">
                    <BookOpen class="w-3.5 h-3.5" />
                    {{ card.title || '联想画面' }}
                  </div>
                  <p class="text-sm leading-relaxed italic" :class="theme === 'dark' ? 'text-gray-100' : 'text-gray-700'">
                    {{ card.prompt || card.image_prompt || card.content }}
                  </p>
                </div>

                <!-- ═══ 未知卡片兜底 ═══ -->
                <div v-else class="glass-card p-5">
                  <div v-if="card.title" class="text-base font-semibold mb-2" :class="theme === 'dark' ? 'text-white' : 'text-gray-900'">{{ card.title }}</div>
                  <pre class="text-xs whitespace-pre-wrap" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-600'">{{ JSON.stringify(card, null, 2) }}</pre>
                </div>
              </div>
            </div>

            <!-- 点赞 -->
            <div v-if="message.role === 'assistant'" class="mt-2 flex items-center gap-3">
              <button
                @click="toggleLike(index)"
                class="flex items-center gap-1 text-xs transition-colors"
                :class="likedMessages.has(index)
                  ? 'text-amber-500'
                  : (theme === 'dark' ? 'text-gray-500 hover:text-amber-400' : 'text-gray-400 hover:text-amber-500')"
              >
                <ThumbsUp class="w-3.5 h-3.5" :class="{ 'fill-amber-500': likedMessages.has(index) }" />
                有用
              </button>
            </div>

            <!-- 快捷建议按钮 -->
            <div v-if="message.role === 'assistant' && message.suggestions && message.suggestions.length > 0" class="mt-3 flex flex-wrap gap-2">
              <button
                v-for="(suggestion, sIdx) in message.suggestions"
                :key="sIdx"
                @click="useSuggestion(suggestion)"
                class="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-all"
                :class="theme === 'dark'
                  ? 'bg-gray-800 text-gray-300 hover:bg-gray-700 hover:text-white border border-gray-700'
                  : 'bg-white text-gray-600 hover:bg-amber-50 hover:text-amber-700 border border-gray-200'"
              >
                <MessageSquare class="w-3 h-3" />
                {{ suggestion }}
              </button>
            </div>

            <div v-if="message.time" class="text-[10px] mt-1.5 px-1" :class="theme === 'dark' ? 'text-gray-300' : 'text-gray-400'">
              {{ formatTime(message.time) }}
            </div>
          </div>

          <div
            v-if="message.role === 'user'"
            class="w-9 h-9 rounded-lg bg-amber-500 flex items-center justify-center flex-shrink-0 mt-1"
          >
            <User class="w-5 h-5 text-white" />
          </div>
        </div>
        </div>

        <!-- Loading 指示器 -->
        <div v-if="loading" class="flex gap-3 max-w-4xl mx-auto">
          <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center flex-shrink-0">
            <Bot class="w-5 h-5 text-white" />
          </div>
          <div class="chat-bubble-bot rounded-2xl rounded-tl-sm px-4 py-3 flex items-center gap-2 text-sm"
               :class="theme === 'dark' ? 'bg-gray-800 border border-gray-700 text-gray-400' : 'bg-white border border-gray-200 text-gray-500 shadow-sm'">
            <Loader2 class="w-4 h-4 animate-spin" />
            {{ loadingText }}
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div v-if="currentView === 'chat'" class="border-t px-4 py-4 backdrop-blur-sm"
           :class="theme === 'dark' ? 'border-gray-800 bg-gray-900/80' : 'border-gray-200 bg-white/80'">
        <div class="max-w-4xl mx-auto">
          <div class="flex items-end gap-2 rounded-2xl p-2 transition-colors"
               :class="theme === 'dark' ? 'bg-gray-800 border border-gray-700 focus-within:border-amber-500/60' : 'bg-white border border-gray-200 focus-within:border-amber-400 shadow-sm'">
            <textarea
              ref="inputRef"
              v-model="inputText"
              @keydown="handleKeydown"
              placeholder="输入你的问题，按 Enter 发送…"
              rows="1"
              class="flex-1 bg-transparent resize-none outline-none px-3 py-2 text-sm max-h-32 min-h-[2.25rem]"
              :class="theme === 'dark' ? 'text-gray-100 placeholder-gray-500' : 'text-gray-900 placeholder-gray-400'"
              :disabled="loading"
            />
            <button
              @click="sendMessage"
              :disabled="!inputText.trim() || loading"
              class="flex items-center justify-center w-9 h-9 rounded-xl text-white transition-colors flex-shrink-0 disabled:cursor-not-allowed"
              :class="!inputText.trim() || loading
                ? (theme === 'dark' ? 'bg-gray-700 text-gray-500' : 'bg-gray-200 text-gray-400')
                : 'bg-amber-500 hover:bg-amber-400'"
            >
              <Send v-if="!loading" class="w-4 h-4" />
              <Loader2 v-else class="w-4 h-4 animate-spin" />
            </button>
          </div>
          <p class="text-[11px] mt-2 text-center" :class="theme === 'dark' ? 'text-gray-600' : 'text-gray-400'">
            按 Enter 发送，Shift+Enter 换行
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useTheme } from '../composables/useTheme'
import {
  Send, Bot, User, Loader2, BookOpen, RotateCcw, Brain, Lightbulb,
  HelpCircle, CalendarDays, GraduationCap, Image, ChevronDown, ThumbsUp,
  MessageSquare, ExternalLink, TrendingUp, Zap, MessageSquarePlus, History, ArrowLeft,
} from 'lucide-vue-next'
import { api } from '../services/api'
import { classifyIntent, createUserMessage, createBotMessage } from '../services/commandDispatcher'
import {
  mockLearnNew, mockReview, mockExplain, mockDiagnosis,
  mockQuiz, mockSchedule, mockRecommend, mockImage, mockChat,
} from '../services/mockData'
import { useChatStore } from '../stores/chat'

const { theme } = useTheme()
const chatStore = useChatStore()

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  响应式状态
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
const currentView = ref('main')
const messages = computed(() => chatStore.messages)
const inputText = computed({
  get: () => chatStore.inputText,
  set: (val) => chatStore.setInputText(val),
})
const loading = ref(false)
const loadingText = ref('正在思考中…')
const messagesContainer = ref(null)
const inputRef = ref(null)
const expandedCards = ref({})
const likedMessages = ref(new Set())
const historyConversations = ref([])

// 测验状态: { "msgIdx-cardIdx-qIdx": { selected, correct } }
const quizState = computed({
  get: () => chatStore.quizState,
  set: (val) => chatStore.setQuizState(val),
})

const exampleCommands = [
  '解释 example',
  '为 apple 画图',
  '考考我',
  '学习计划',
]

const agentLabels = {
  memory_diagnostics: '记忆诊断',
  memory_diagnosis: '记忆诊断',
  content_generation: '内容生成',
  review_scheduler: '复习调度',
  'review-scheduling': '复习调度',
  'content-generation': '内容生成',
  'memory-diagnosis': '记忆诊断',
  coordinator: '协调器',
}

const agentIconMap = {
  memory_diagnostics: Brain,
  memory_diagnosis: Brain,
  content_generation: BookOpen,
  'content-generation': BookOpen,
  review_scheduler: RotateCcw,
  'review-scheduling': RotateCcw,
  coordinator: Lightbulb,
}

function getAgentLabel(agent) {
  return agentLabels[agent] || agent
}

function getAgentIcon(agent) {
  return agentIconMap[agent] || Bot
}

function formatKey(key) {
  const str = String(key)
    .replace(/_/g, ' ')
    .replace(/([A-Z])/g, ' $1')
    .trim()
  return str.charAt(0).toUpperCase() + str.slice(1)
}

function formatTime(time) {
  if (!time) return ''
  try {
    const date = new Date(time)
    const h = String(date.getHours()).padStart(2, '0')
    const m = String(date.getMinutes()).padStart(2, '0')
    return `${h}:${m}`
  } catch {
    return time
  }
}

function formatDate(time) {
  if (!time) return ''
  try {
    const date = new Date(time)
    const m = date.getMonth() + 1
    const d = date.getDate()
    const h = String(date.getHours()).padStart(2, '0')
    const min = String(date.getMinutes()).padStart(2, '0')
    return `${m}月${d}日 ${h}:${min}`
  } catch {
    return time
  }
}

function optionLabel(index) {
  return String.fromCharCode(65 + index)
}

function isNumeric(value) {
  return !isNaN(parseFloat(value)) && isFinite(value)
}

function isExpanded(index) {
  return expandedCards.value[index] !== false
}

function toggleExpand(index) {
  expandedCards.value[index] = !isExpanded(index)
}

function toggleLike(index) {
  if (likedMessages.value.has(index)) {
    likedMessages.value.delete(index)
  } else {
    likedMessages.value.add(index)
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  测验交互逻辑
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
function quizKey(msgIdx, cardIdx, qIdx) {
  return `${msgIdx}-${cardIdx}-${qIdx}`
}

function isQuizAnswered(msgIdx, cardIdx, qIdx) {
  return !!quizState.value[quizKey(msgIdx, cardIdx, qIdx)]
}

function isQuizOptionSelected(msgIdx, cardIdx, qIdx, oIdx) {
  const key = quizKey(msgIdx, cardIdx, qIdx)
  return quizState.value[key]?.selected === oIdx
}

function isQuizCorrect(msgIdx, cardIdx, qIdx) {
  return quizState.value[quizKey(msgIdx, cardIdx, qIdx)]?.correct === true
}

function isQuizComplete(msgIdx, cardIdx) {
  const card = messages.value[msgIdx]?.cards?.[cardIdx]
  if (!card || !card.questions) return false
  return card.questions.every((_, qIdx) => isQuizAnswered(msgIdx, cardIdx, qIdx))
}

function getQuizScore(msgIdx, cardIdx) {
  const card = messages.value[msgIdx]?.cards?.[cardIdx]
  if (!card || !card.questions) return { correct: 0, total: 0 }
  const total = card.questions.length
  let correct = 0
  card.questions.forEach((_, qIdx) => {
    if (isQuizCorrect(msgIdx, cardIdx, qIdx)) correct++
  })
  return { correct, total }
}

function getQuizOptionClass(msgIdx, cardIdx, qIdx, oIdx, q) {
  const answered = isQuizAnswered(msgIdx, cardIdx, qIdx)
  const selected = isQuizOptionSelected(msgIdx, cardIdx, qIdx, oIdx)
  const isCorrectOption = oIdx === q.answer
  const isDark = theme.value === 'dark'

  if (!answered) {
    return isDark
      ? 'bg-gray-900/60 border-gray-700 text-gray-300 hover:border-gray-500 hover:bg-gray-800 cursor-pointer'
      : 'bg-white border-gray-200 text-gray-600 hover:border-gray-300 hover:bg-gray-50 cursor-pointer'
  }

  if (selected && isCorrectOption) {
    return isDark
      ? 'bg-emerald-500/20 border-emerald-500/60 text-emerald-100 cursor-default'
      : 'bg-emerald-50 border-emerald-400 text-emerald-800 cursor-default'
  }
  if (selected && !isCorrectOption) {
    return isDark
      ? 'bg-rose-500/20 border-rose-500/60 text-rose-100 cursor-default'
      : 'bg-rose-50 border-rose-400 text-rose-800 cursor-default'
  }
  if (isCorrectOption) {
    return isDark
      ? 'bg-emerald-500/10 border-emerald-500/40 text-emerald-200 cursor-default'
      : 'bg-emerald-50 border-emerald-300 text-emerald-700 cursor-default'
  }
  return isDark
    ? 'bg-gray-900/40 border-gray-700/50 text-gray-500 cursor-default'
    : 'bg-gray-50 border-gray-200 text-gray-400 cursor-default'
}

function handleQuizAnswer(msgIdx, cardIdx, qIdx, oIdx, question) {
  if (isQuizAnswered(msgIdx, cardIdx, qIdx)) return
  const correct = oIdx === question.answer
  chatStore.updateQuizState(quizKey(msgIdx, cardIdx, qIdx), { selected: oIdx, correct })
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  遗忘曲线 SVG 辅助函数
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
function getCurveX(day) {
  return 30 + (day / 30) * 260
}

function getCurveY(percent) {
  return 130 - (percent / 100) * 120
}

function getCurvePath(curvePoints, field) {
  if (!curvePoints || curvePoints.length === 0) return ''
  return curvePoints
    .map((pt) => `${getCurveX(pt.day)},${getCurveY(pt[field])}`)
    .join(' ')
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  配图 URL 构建 (pollinations.ai)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  流式文本效果
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
async function streamTextToMessage(msgIndex, fullText, speed = 25) {
  const msg = messages.value[msgIndex]
  if (!msg) return
  chatStore.updateMessage(msgIndex, { streaming: true, content: '' })
  for (let i = 0; i < fullText.length; i++) {
    chatStore.updateMessage(msgIndex, { content: fullText.slice(0, i + 1) })
    if (i % 5 === 0) {
      await scrollToBottom()
      await new Promise((r) => setTimeout(r, speed))
    }
  }
  chatStore.updateMessage(msgIndex, { streaming: false })
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  消息发送核心逻辑
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  const { intent, targetWord } = classifyIntent(text)

  chatStore.addMessage(createUserMessage(text))
  chatStore.setInputText('')
  loading.value = true

  const loadingTexts = {
    learn_new: '正在为您挑选新单词…',
    review: '正在查询待复习单词…',
    explain: `正在查询 "${targetWord || text}" 的释义…`,
    diagnosis: '正在分析学习数据…',
    quiz: '正在生成测验题…',
    schedule: '正在规划复习安排…',
    recommend: '正在分析学习建议…',
    image: `正在为 "${targetWord || ''}" 生成配图…`,
    chat: '正在思考中…',
  }
  loadingText.value = loadingTexts[intent] || '正在思考中…'

  await scrollToBottom()

  try {
    let useMock = false
    let replyData = null

    try {
      const res = await api.post('/api/chat/message', { message: text })
      const reply = res.data
      const cards = reply.cards || []

      console.log('Backend reply:', JSON.stringify(reply, null, 2))
      console.log('Cards:', cards)
      console.log('Cards length:', cards.length)

      if (reply.reply || cards.length > 0) {
        replyData = {
          content: reply.reply || reply.content || reply.message || '',
          agents: reply.agents || reply.agentsInvoked || [],
          agent_type: reply.agent_type || null,
          cards: cards,
          time: new Date().toISOString(),
          suggestions: reply.suggestions || null,
        }
      } else {
        useMock = true
      }
    } catch (err) {
      console.warn('Backend unavailable, falling back to mock data:', err.message)
      useMock = true
    }

    if (useMock) {
      try {
        let mockResult
        switch (intent) {
          case 'learn_new':  mockResult = await mockLearnNew(); break
          case 'review':     mockResult = await mockReview(); break
          case 'explain':    mockResult = await mockExplain(targetWord); break
          case 'diagnosis':  mockResult = await mockDiagnosis(); break
          case 'quiz':       mockResult = await mockQuiz(); break
          case 'schedule':   mockResult = await mockSchedule(); break
          case 'recommend':  mockResult = await mockRecommend(); break
          case 'image':      mockResult = await mockImage(targetWord || text); break
          default:           mockResult = await mockChat(text); break
        }
        replyData = {
          content: mockResult.reply || '',
          agents: mockResult.agents || [],
          agent_type: null,
          cards: mockResult.cards || [],
          time: new Date().toISOString(),
          suggestions: mockResult.suggestions || null,
          _stream: mockResult.stream || false,
          _mockImage: intent === 'image' ? mockResult : null,
        }
      } catch (mockErr) {
        console.error('Mock data error:', mockErr)
        replyData = {
          content: '抱歉，发生了错误，请稍后再试。',
          agents: [],
          cards: [],
          time: new Date().toISOString(),
        }
      }
    }

    

    const msgIndex = messages.value.length
    chatStore.addMessage({
      role: 'assistant',
      content: replyData.content,
      msgType: (replyData.cards?.length ?? 0) > 0 ? 'card' : 'text',
      agents: replyData.agents,
      agent_type: replyData.agent_type,
      cards: replyData.cards || [],
      time: replyData.time,
      suggestions: replyData.suggestions,
      streaming: false,
    })

    if (replyData._stream && replyData.content) {
      const fullText = replyData.content
      await streamTextToMessage(msgIndex, fullText)
    }

    await scrollToBottom()
    if (inputRef.value) inputRef.value.focus()
  } catch (err) {
    console.error('Send message error:', err)
    chatStore.addMessage({
      role: 'assistant',
      content: '抱歉，处理你的请求时发生了错误：' + err.message,
      msgType: 'text',
      agents: [],
      cards: [],
      time: new Date().toISOString(),
    })
    await scrollToBottom()
  } finally {
    loading.value = false
    loadingText.value = '正在思考中…'
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  其他操作函数
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
function useSuggestion(text) {
  inputText.value = text
  sendMessage()
}

async function resetConversation() {
  if (chatStore.messages.length > 0) {
    try {
      await api.post('/api/chat/end')
    } catch (e) {
      console.warn('Failed to end conversation:', e)
    }
  }
  chatStore.clearMessages()
}

function goBack() {
  currentView.value = 'main'
}

async function startNewConversation() {
  if (chatStore.messages.length > 0) {
    try {
      await api.post('/api/chat/end')
    } catch (e) {
      console.warn('Failed to end conversation:', e)
    }
  }
  currentView.value = 'chat'
  chatStore.clearMessages()
}

async function viewHistory() {
  try {
    loading.value = true
    const res = await api.get('/api/chat/history', { params: { limit: 2 } })
    const data = res.data
    if (Array.isArray(data)) {
      historyConversations.value = data.map((conv) => ({
        ...conv,
        messages: conv.messages || conv.history || [],
      }))
    } else if (data.conversations) {
      historyConversations.value = data.conversations.slice(0, 2).map((conv) => ({
        ...conv,
        messages: conv.messages || [],
      }))
    } else {
      historyConversations.value = []
    }
  } catch (err) {
    console.error('Failed to load chat history:', err)
    historyConversations.value = []
  } finally {
    loading.value = false
    currentView.value = 'history'
  }
}

function loadConversation(conversation) {
  chatStore.setMessages(conversation.messages || [])
  currentView.value = 'chat'
  scrollToBottom()
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

onMounted(() => {
  chatStore.initMessages()
  if (chatStore.messages.length > 1 || (chatStore.messages.length === 1 && chatStore.messages[0].content)) {
    currentView.value = 'chat'
  }
})
</script>

<style scoped>
textarea {
  scrollbar-width: thin;
  scrollbar-color: rgba(75, 85, 99, 0.5) transparent;
}

textarea::-webkit-scrollbar {
  width: 6px;
}

textarea::-webkit-scrollbar-thumb {
  background-color: rgba(75, 85, 99, 0.5);
  border-radius: 3px;
}

.chat-bubble-user {
  @apply dark:bg-amber-500/90 bg-amber-500 text-white;
}

.chat-bubble-bot {
  @apply dark:bg-gray-800 dark:border-gray-700 dark:text-gray-100 bg-white border border-gray-200 text-gray-800;
}

/* 流式文本光标 */
.streaming-cursor {
  display: inline-block;
  animation: blink-cursor 0.7s infinite;
  font-weight: 300;
  color: #f59e0b;
}

@keyframes blink-cursor {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 卡片通用样式 */
.glass-card {
  @apply rounded-xl border;
  @apply dark:bg-gray-800/50 dark:border-gray-700/50;
  @apply bg-white border-gray-200 shadow-sm;
}

/* 图片卡加载动画 */
.image-loading-pulse {
  animation: pulse-bg 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse-bg {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
