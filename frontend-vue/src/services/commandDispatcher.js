/**
 * commandDispatcher.js — 指令分发器
 *
 * 数据结构定义 + 策略模式意图分类 + 消息工厂
 *
 * ── 消息数据结构 ──────────────────────────────────────────
 * message = {
 *   role:       'user' | 'assistant',
 *   content:    String,                   // 文本内容
 *   msgType:    'text' | 'card' | 'quiz' | 'image' | 'stats' | 'schedule' | 'loading',
 *   agents:     String[],                 // 调用的 agent 名称
 *   agent_type: String | null,            // 主 agent
 *   cards:      Card[],                   // 卡片数组
 *   time:       ISO String,
 *   // ── 以下为前端增强字段 ──
 *   quizResult: { total, correct, answered },   // 测验汇总
 *   imageUrl:   String,                  // pollinations.ai 图片 URL
 *   imageLoading: Boolean,               // 配图加载中
 *   streamDone: Boolean,                 // 流式文本是否完成
 * }
 *
 * card = {
 *   type: 'word_card' | 'review_card' | 'stats' | 'recommendations'
 *       | 'schedule' | 'quiz' | 'image_prompt' | 'image' | 'error',
 *   ...type-specific fields
 * }
 */

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  意图定义 (Intent Definitions)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
export const INTENT_DEFS = {
  learn_new:  { patterns: [/学习/, /背单词/, /新词/, /教我/],                keywords: ['学习', '背单词', '新词', '新单词', '开始学习'] },
  review:     { patterns: [/复习/, /巩固/, /回顾/],                           keywords: ['复习', '回顾', '巩固', '该复习'] },
  explain:    { patterns: [/^解释\s+(\S+)$/],                                 keywords: ['解释', '什么意思', '用法', '例句'] },
  diagnosis:  { patterns: [/诊断/, /学习报告/, /分析.*学习/, /我学得怎么样/],    keywords: ['诊断', '学习报告', '分析', '评估', '掌握'] },
  quiz:       { patterns: [/测试/, /考考我/, /测验/, /出题/],                 keywords: ['测试', '测验', '考考我', '做题'] },
  schedule:   { patterns: [/计划/, /安排/, /日程/, /复习.*计划/, /学习.*计划/], keywords: ['计划', '安排', '日程', '学习规划', '学习计划', '复习计划'] },
  recommend:  { patterns: [/建议/, /推荐.*方法/, /怎么.*学/],                 keywords: ['建议', '推荐', '怎么学', '提高效率'] },
  image:      { patterns: [/^为\s+(.+?)\s*画图$/],                            keywords: ['画图', '配图', '图片'] },
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  意图分类器 (Intent Classifier)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * 对用户输入进行意图分类
 * @param {string} input - 用户输入文本
 * @returns {{ intent: string, confidence: number, targetWord: string|null, match: RegExpMatchArray|null }}
 */
export function classifyIntent(input) {
  const text = input.trim()
  let bestIntent = 'chat'
  let bestScore = 0
  let targetWord = null
  let regexMatch = null

  for (const [intent, def] of Object.entries(INTENT_DEFS)) {
    let score = 0

    // 正则匹配（高优先级）
    for (const pat of def.patterns) {
      const m = text.match(pat)
      if (m) {
        score += 3
        if (m[1]) {
          targetWord = m[1].trim()
          regexMatch = m
        }
      }
    }

    // 关键词匹配
    for (const kw of def.keywords) {
      if (text.includes(kw)) score += 1
    }

    if (score > bestScore) {
      bestScore = score
      bestIntent = intent
    }
  }

  // 兜底：从文本中提取英文单词
  if (!targetWord) {
    const asciiMatch = text.match(/[a-zA-Z]{2,}/g)
    if (asciiMatch) {
      const noise = new Set(['the', 'is', 'at', 'of', 'on', 'it', 'to', 'and', 'for', 'with'])
      const clean = asciiMatch.filter(w => !noise.has(w.toLowerCase()))
      if (clean.length > 0) targetWord = clean[0]
    }
  }

  return {
    intent: bestIntent,
    confidence: Math.min(1.0, bestScore / 3),
    targetWord,
    match: regexMatch,
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  消息工厂 (Message Factories)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

export function createUserMessage(text) {
  return { role: 'user', content: text, msgType: 'text', time: new Date().toISOString() }
}

export function createBotMessage(content, extras = {}) {
  return {
    role: 'assistant',
    content: content || '',
    msgType: extras.msgType || 'text',
    agents: extras.agents || [],
    agent_type: extras.agent_type || null,
    cards: extras.cards || [],
    time: new Date().toISOString(),
    quizResult: extras.quizResult || null,
    imageUrl: extras.imageUrl || null,
    imageLoading: extras.imageLoading || false,
    streamDone: extras.streamDone !== undefined ? extras.streamDone : true,
    suggestions: extras.suggestions || null,
    ...extras,
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  调度结果 (Dispatch Result)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * 解析用户输入并返回调度结果
 * @param {string} input
 * @returns {{ intent: string, confidence: number, targetWord: string|null, needsMock: boolean }}
 */
export function dispatchCommand(input) {
  const result = classifyIntent(input)
  return {
    ...result,
    // 标记哪些功能需要前端增强处理
    needsFrontendEnhance: ['image', 'quiz', 'diagnosis', 'recommend'].includes(result.intent),
  }
}
