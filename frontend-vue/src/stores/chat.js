import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])
  const inputText = ref('')
  const quizState = ref({})

  const welcomeMessage = {
    role: 'assistant',
    content: '',
    msgType: 'card',
    cards: [{
      type: 'welcome',
      title: '智能助手',
      content: '您好！我是您的智能学习助手，可回复：',
      items: [
        { label: '学习新词', desc: '说"学习"或"背单词"开始学习新单词' },
        { label: '复习巩固', desc: '说"复习"查看需要复习的单词' },
        { label: '单词解释', desc: '说"解释 define"获取详细释义和例句' },
        { label: '测验练习', desc: '说"测试"或"考考我"进行单词测验' },
        { label: '学习计划', desc: '说"计划"查看复习安排' },
        { label: '配图辅助', desc: '说"为 xxx 画图"生成记忆配图' },
      ],
      suggestions: ['解释 define', '为 barrier 画图', '考考我', '学习计划'],
    }],
    time: new Date().toISOString(),
    agents: [],
  }

  function initMessages() {
    if (messages.value.length === 0) {
      messages.value = [welcomeMessage]
    }
  }

  function addMessage(message) {
    messages.value.push(message)
  }

  function setMessages(newMessages) {
    messages.value = newMessages
  }

  function clearMessages() {
    messages.value = [welcomeMessage]
    inputText.value = ''
    quizState.value = {}
  }

  function setInputText(text) {
    inputText.value = text
  }

  function setQuizState(state) {
    quizState.value = state
  }

  function updateQuizState(key, value) {
    quizState.value = { ...quizState.value, [key]: value }
  }

  function updateMessage(index, updates) {
    if (messages.value[index]) {
      messages.value[index] = { ...messages.value[index], ...updates }
    }
  }

  return {
    messages,
    inputText,
    quizState,
    initMessages,
    addMessage,
    setMessages,
    clearMessages,
    setInputText,
    setQuizState,
    updateQuizState,
    updateMessage,
  }
})