/**
 * mockData.js — 8 大功能 Mock 数据 + Promise 模拟延迟
 *
 * 每个 handler 返回 Promise，模拟后端延迟，
 * 返回结构与后端 ChatController 一致的 { reply, agents, cards } 格式。
 */

// ━━ 工具函数 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
const delay = (ms) => new Promise((r) => setTimeout(r, ms))

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  1. 学习新词 — 返回 5 个详细单词卡片
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
const MOCK_WORDS = [
  {
    type: 'word_card',
    word: 'elaborate',
    phonetic: '/ɪˈlæbərət/',
    definitions: [
      { pos: 'adj', en: 'involving many carefully arranged parts or details', cn: '精心制作的；详尽的' },
      { pos: 'v', en: 'to explain or describe something in a more detailed way', cn: '详尽阐述' },
    ],
    mnemonic: { method: '词根词缀', content: 'e-(出) + labor(劳动) + -ate(使) → 付出劳动做出来的 → 精心制作的' },
    examples: [
      { en: 'She wore an elaborate dress to the gala.', cn: '她穿了一件精致的礼服出席晚会。' },
      { en: 'Could you elaborate on your proposal?', cn: '你能详细阐述一下你的提案吗？' },
    ],
    collocations: ['elaborate plan', 'elaborate design', 'elaborate on'],
    synonyms: [{ word: 'intricate', diff: '侧重复杂精细' }, { word: 'detailed', diff: '侧重细节丰富' }],
    exam_tips: 'CET-6 / 考研高频词，注意形容词和动词两种用法的区别。',
    progress: 0,
  },
  {
    type: 'word_card',
    word: 'ubiquitous',
    phonetic: '/juːˈbɪkwɪtəs/',
    definitions: [
      { pos: 'adj', en: 'present, appearing, or found everywhere', cn: '无处不在的' },
    ],
    mnemonic: { method: '词根词缀', content: 'ubique(到处) + -ous(…的) → 到处都有的 → 无处不在的' },
    examples: [
      { en: 'Smartphones have become ubiquitous in modern life.', cn: '智能手机在现代生活中已无处不在。' },
      { en: 'The ubiquitous influence of social media is hard to ignore.', cn: '社交媒体无处不在的影响难以忽视。' },
    ],
    collocations: ['ubiquitous presence', 'ubiquitous in', 'become ubiquitous'],
    synonyms: [{ word: 'omnipresent', diff: '更正式，常用于文学' }, { word: 'pervasive', diff: '侧重渗透性' }],
    exam_tips: 'IELTS / TOEFL 写作加分词汇，适合描述科技、文化类话题。',
    progress: 0,
  },
  {
    type: 'word_card',
    word: 'pragmatic',
    phonetic: '/præɡˈmætɪk/',
    definitions: [
      { pos: 'adj', en: 'dealing with things sensibly and realistically', cn: '务实的；实用主义的' },
    ],
    mnemonic: { method: '联想', content: 'prag(=practice 实践) + matic → 注重实践的 → 务实的' },
    examples: [
      { en: 'We need a pragmatic approach to solving this problem.', cn: '我们需要一种务实的方法来解决这个问题。' },
      { en: 'She is known for her pragmatic leadership style.', cn: '她以务实的领导风格著称。' },
    ],
    collocations: ['pragmatic approach', 'pragmatic solution', 'pragmatic view'],
    synonyms: [{ word: 'realistic', diff: '侧重现实考量' }, { word: 'sensible', diff: '侧重合理明智' }],
    exam_tips: '考研阅读高频词，常出现在议论文中描述政策或态度。',
    progress: 0,
  },
  {
    type: 'word_card',
    word: 'ambiguous',
    phonetic: '/æmˈbɪɡjuəs/',
    definitions: [
      { pos: 'adj', en: 'open to more than one interpretation; not clear', cn: '模棱两可的；含糊不清的' },
    ],
    mnemonic: { method: '词根词缀', content: 'ambi-(两者) + gu(=agere 引导) + -ous → 引向两个方向的 → 模棱两可的' },
    examples: [
      { en: 'The wording of the contract is deliberately ambiguous.', cn: '这份合同的措辞故意含糊不清。' },
      { en: 'His reply was ambiguous, leaving us confused.', cn: '他的回答模棱两可，让我们困惑不已。' },
    ],
    collocations: ['ambiguous statement', 'morally ambiguous', 'deliberately ambiguous'],
    synonyms: [{ word: 'vague', diff: '侧重不够清晰具体' }, { word: 'equivocal', diff: '更正式，有意含糊' }],
    exam_tips: 'CET-4/6 高频词，注意与 vague 的细微区别。',
    progress: 0,
  },
  {
    type: 'word_card',
    word: 'resilient',
    phonetic: '/rɪˈzɪliənt/',
    definitions: [
      { pos: 'adj', en: 'able to recover quickly from difficult conditions', cn: '有韧性的；能迅速恢复的' },
      { pos: 'adj', en: '(of a material) able to recoil or spring back into shape', cn: '有弹性的' },
    ],
    mnemonic: { method: '词根词缀', content: 're-(回) + sili(=salire 跳) + -ent → 能跳回来的 → 有恢复力的' },
    examples: [
      { en: 'Children are remarkably resilient.', cn: '孩子们有着惊人的恢复力。' },
      { en: 'The economy proved resilient despite the crisis.', cn: '尽管面临危机，经济仍展现出强大的韧性。' },
    ],
    collocations: ['resilient economy', 'emotionally resilient', 'highly resilient'],
    synonyms: [{ word: 'tough', diff: '更口语化' }, { word: 'adaptable', diff: '侧重适应能力' }],
    exam_tips: 'IELTS 写作高频词，适合描述人物品质或社会发展。',
    progress: 0,
  },
]

export async function mockLearnNew() {
  await delay(1200)
  return {
    reply: '为您精选了 5 个新单词，每个都附有详细知识点，开始学习吧！',
    agents: ['coordinator', 'review-scheduling', 'content-generation'],
    cards: MOCK_WORDS,
    suggestions: ['复习', '考考我', '建议'],
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  2. 复习巩固 — 返回待复习单词统计
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
const MOCK_REVIEW_WORDS = [
  {
    type: 'review_card', word: 'elaborate', phonetic: '/ɪˈlæbərət/',
    definitions: [{ pos: 'adj', en: 'involving many carefully arranged parts', cn: '精心制作的' }],
    examples: [{ en: 'She gave an elaborate explanation.', cn: '她给出了详尽的解释。' }],
    progress: 45,
  },
  {
    type: 'review_card', word: 'pragmatic', phonetic: '/præɡˈmætɪk/',
    definitions: [{ pos: 'adj', en: 'dealing with things sensibly and realistically', cn: '务实的' }],
    examples: [{ en: 'A pragmatic approach is needed.', cn: '需要务实的方法。' }],
    progress: 72,
  },
  {
    type: 'review_card', word: 'ambiguous', phonetic: '/æmˈbɪɡjuəs/',
    definitions: [{ pos: 'adj', en: 'open to more than one interpretation', cn: '模棱两可的' }],
    examples: [{ en: 'The message was ambiguous.', cn: '这条消息含糊不清。' }],
    progress: 30,
  },
  {
    type: 'review_card', word: 'resilient', phonetic: '/rɪˈzɪliənt/',
    definitions: [{ pos: 'adj', en: 'able to recover quickly', cn: '有韧性的' }],
    examples: [{ en: 'She is resilient in the face of adversity.', cn: '面对逆境她很有韧性。' }],
    progress: 60,
  },
  {
    type: 'review_card', word: 'ubiquitous', phonetic: '/juːˈbɪkwɪtəs/',
    definitions: [{ pos: 'adj', en: 'present everywhere', cn: '无处不在的' }],
    examples: [{ en: 'Technology is ubiquitous today.', cn: '如今科技无处不在。' }],
    progress: 55,
  },
]

export async function mockReview() {
  await delay(900)
  const count = MOCK_REVIEW_WORDS.length
  return {
    reply: `今日有 ${count} 个单词需要复习，加油！`,
    agents: ['coordinator', 'review-scheduling', 'content-generation'],
    cards: MOCK_REVIEW_WORDS,
    reviewCount: count,
    suggestions: ['学习', '考考我', '诊断'],
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  3. 单词解释 — 返回单个单词详解
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
const EXPLAIN_DB = {
  define: {
    word: 'define', phonetic: '/dɪˈfaɪn/',
    definitions: [
      { pos: 'v', en: 'to state or describe exactly the nature, scope, or meaning of', cn: '给…下定义；明确界定' },
      { pos: 'v', en: 'to mark out the boundary or limits of', cn: '划定…的界限' },
    ],
    examples: [
      { en: 'It is hard to define what makes a good leader.', cn: '很难定义是什么造就了一个好的领导者。' },
      { en: 'The borders were clearly defined in the treaty.', cn: '条约中明确划定了边界。' },
    ],
  },
  example: {
    word: 'example', phonetic: '/ɪɡˈzæmpl/',
    definitions: [
      { pos: 'n', en: 'a thing characteristic of its kind or illustrating a rule', cn: '例子；榜样' },
    ],
    examples: [
      { en: 'Can you give me an example of how to use this word?', cn: '你能给我一个如何使用这个词的例子吗？' },
      { en: 'She set a good example for her classmates.', cn: '她为同学们树立了好榜样。' },
    ],
  },
  apple: {
    word: 'apple', phonetic: '/ˈæpl/',
    definitions: [
      { pos: 'n', en: 'the round fruit of a tree of the rose family', cn: '苹果' },
    ],
    examples: [
      { en: 'An apple a day keeps the doctor away.', cn: '一天一苹果，医生远离我。' },
      { en: 'She bit into a crisp red apple.', cn: '她咬了一口脆脆的红苹果。' },
    ],
  },
}

export async function mockExplain(word) {
  await delay(800)
  const key = (word || '').toLowerCase()
  const data = EXPLAIN_DB[key] || {
    word: word, phonetic: '/…/',
    definitions: [{ pos: '…', en: `Definition of "${word}"`, cn: `"${word}" 的释义（后端加载后可获得完整数据）` }],
    examples: [
      { en: `This is an example sentence using "${word}".`, cn: `这是使用 "${word}" 的例句。` },
    ],
  }
  return {
    reply: `以下是 "${data.word}" 的详细释义：`,
    agents: ['coordinator', 'content-generation'],
    cards: [{ type: 'word_card', ...data }],
    suggestions: [`为 ${word} 画图`, '考考我', '复习'],
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  4. 记忆诊断 — 学习分析摘要 + 遗忘曲线
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
export async function mockDiagnosis() {
  await delay(1500)
  return {
    reply: '已完成学习数据诊断分析，以下是你的记忆状态报告：',
    agents: ['coordinator', 'memory-diagnosis'],
    cards: [
      {
        type: 'stats',
        title: '学习诊断报告',
        data: {
          memoryStrength: 6.8,
          forgettingRate: 0.015,
          halfLifeDays: 46.2,
          masteryPercent: 62,
          totalWordsLearned: 48,
          wordsDueForReview: 12,
          averageAccuracy: 78,
          optimalDailyWords: 15,
        },
        curvePoints: [
          { day: 0, predicted: 100, actual: 100 },
          { day: 1, predicted: 85, actual: 88 },
          { day: 2, predicted: 72, actual: 75 },
          { day: 3, predicted: 65, actual: 62 },
          { day: 5, predicted: 55, actual: 58 },
          { day: 7, predicted: 48, actual: 52 },
          { day: 10, predicted: 42, actual: 45 },
          { day: 14, predicted: 38, actual: 42 },
          { day: 21, predicted: 34, actual: 38 },
          { day: 30, predicted: 30, actual: 35 },
        ],
        insights: [
          '你的记忆保持力优于平均水平，实际留存率高于艾宾浩斯预测曲线',
          '建议重点复习掌握度低于 30% 的词汇',
          '当前学习节奏良好，每日 15 词为最优负荷',
        ],
      },
      {
        type: 'recommendations',
        title: '诊断建议',
        items: [
          '保持每日复习节奏，当前遗忘率仅为 1.5%，表现优秀',
          '建议对掌握度低于 30% 的词汇进行集中突破',
          '尝试使用词根词缀记忆法提升新词学习效率',
        ],
      },
    ],
    suggestions: ['复习', '计划', '建议'],
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  5. 测验练习 — 3 道单选题
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
export async function mockQuiz() {
  await delay(1000)
  return {
    reply: '准备好了！以下是为你量身定制的 3 道词汇测验题：',
    agents: ['coordinator', 'content-generation'],
    cards: [
      {
        type: 'quiz',
        title: '词汇测验',
        questions: [
          {
            question: 'elaborate',
            phonetic: '/ɪˈlæbərət/',
            options: ['简单的', '精心制作的', '模糊的', '快速的'],
            answer: 1,
            explanation: '正确答案：精心制作的\nelaborate 的中文释义是：精心制作的；详尽的',
          },
          {
            question: 'ubiquitous',
            phonetic: '/juːˈbɪkwɪtəs/',
            options: ['罕见的', '有毒的', '无处不在的', '昂贵的'],
            answer: 2,
            explanation: '正确答案：无处不在的\nubiquitous 的中文释义是：无处不在的',
          },
          {
            question: 'pragmatic',
            phonetic: '/præɡˈmætɪk/',
            options: ['理想主义的', '务实的', '浪漫的', '固执的'],
            answer: 1,
            explanation: '正确答案：务实的\npragmatic 的中文释义是：务实的；实用主义的',
          },
        ],
      },
    ],
    suggestions: ['学习', '诊断', '复习'],
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  6. 学习计划 — 7 天复习时间表
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
export async function mockSchedule() {
  await delay(800)
  const tasks = [
    {
      type: 'review',
      taskName: '复习已过期单词',
      description: '优先复习已过期的单词，防止遗忘',
      durationMinutes: 30,
      targetCount: 42,
      wordCount: 42,
      priority: 'high',
      suggestedMethod: '使用卡片复习模式，专注于回忆单词含义',
    },
    {
      type: 'urgent_review',
      taskName: '紧急单词强化',
      description: '复习即将遗忘的高风险单词',
      durationMinutes: 15,
      targetCount: 5,
      wordCount: 143,
      priority: 'high',
      suggestedMethod: '重点关注掌握度低且错误率高的单词',
    },
    {
      type: 'new_words',
      taskName: '学习新单词',
      description: '学习新单词，建立初步记忆',
      durationMinutes: 18,
      targetCount: 30,
      wordCount: 30,
      priority: 'medium',
      suggestedMethod: '结合例句和图片，多感官记忆',
    },
    {
      type: 'weak_review',
      taskName: '薄弱单词巩固',
      description: '复习错误率较高的单词，巩固记忆',
      durationMinutes: 6,
      targetCount: 10,
      wordCount: 10,
      priority: 'low',
      suggestedMethod: '回顾错题，分析错误原因，加强练习',
    },
  ]
  return {
    reply: '这是你今天的学习计划：\n\n1. 优先：复习已过期单词（42个单词），预计30分钟\n2. 优先：紧急单词强化（143个单词），预计15分钟\n3. 普通：学习新单词（30个单词），预计18分钟\n4. 次要：薄弱单词巩固（10个单词），预计6分钟',
    agents: ['coordinator', 'review-scheduling'],
    cards: [{ type: 'schedule', title: '今日计划', tasks }],
    suggestions: ['学习', '复习'],
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  7. 学习建议 — 流式文本（完整文本，前端逐字渲染）
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
export async function mockRecommend() {
  await delay(600)
  const fullText = `📊 基于你最近的学习数据分析，以下是个性化学习建议：

1. 复习节奏优化
你目前的记忆强度为 6.8（中等偏上），建议每日复习 12-15 个单词，保持当前节奏。遗忘率仅 1.5%，说明你的复习间隔设置合理。

2. 薄弱点突破
掌握度低于 30% 的词汇主要集中在形容词类，建议：
  • 使用词根词缀法拆解记忆
  • 每个生词至少造 2 个句子
  • 将难词与已知词建立联想关联

3. 最佳学习时段
根据你的学习记录，上午 9:00-11:00 和晚上 19:00-21:00 是你的高效时段，建议在这些时间段进行新词学习。

4. 长期规划
按照当前进度，预计 30 天后可掌握当前词库 85% 的核心词汇。建议每周进行一次诊断评估，及时调整学习策略。

加油！你的学习势头非常好 💪`

  return {
    reply: fullText,
    agents: ['coordinator', 'review-scheduling', 'memory-diagnosis'],
    cards: [],
    stream: true,
    suggestions: ['诊断', '计划', '复习'],
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  8. 配图辅助 — 返回 pollinations.ai 图片 URL
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
export async function mockImage(word) {
  await delay(500) // 前端拿到 URL 后由浏览器加载图片
  const prompt = `A vivid, photorealistic educational illustration of the concept of "${word}". Bright colours, clear composition, no text in the image, high quality, detailed, artistic style suitable for vocabulary learning memory aid.`
  const encoded = encodeURIComponent(prompt)
  const imageUrl = `https://image.pollinations.ai/prompt/${encoded}?width=1024&height=1024&model=flux&nologo=true&seed=${Date.now()}`
  return {
    reply: `已为 "${word}" 生成记忆配图：`,
    agents: ['coordinator', 'content-generation'],
    cards: [{ type: 'image', word, imageUrl, imageLoading: false }],
    suggestions: [`解释 ${word}`, '学习', '复习'],
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  兜底 — 普通聊天
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
export async function mockChat(text) {
  await delay(600)
  return {
    reply: `你好！我是背词鸭智能助手。你可以试试以下功能：\n\n• 说"学习"开始学习新词\n• 说"复习"查看待复习单词\n• 说"解释 xxx"获取单词详解\n• 说"诊断"查看学习报告\n• 说"考考我"进行测验\n• 说"计划"查看复习安排\n• 说"建议"获取学习建议\n• 说"为 xxx 画图"生成配图`,
    agents: ['coordinator'],
    cards: [],
    suggestions: ['学习', '复习', '考考我', '诊断'],
  }
}
