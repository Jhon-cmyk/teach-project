export interface PaperSplitResult {
  paper: string
  answers: string
}

export const normalizePaperLine = (line: string) => {
  return String(line || '')
    .replace(/\r/g, '')
    .replace(/^#{1,6}\s*/, '')
    .replace(/^\s*>\s*/, '')
    .replace(/^\s*[-*+]\s+/, '')
    .replace(/\*\*/g, '')
    .replace(/__/g, '')
    .replace(/`/g, '')
    .trim()
}

const QUIZ_SECTION_TITLES = [
  '单项选择题',
  '单选题',
  '选择题',
  '多项选择题',
  '多选题',
  '判断题',
  '填空题',
  '简答题',
  '问答题',
  '论述题',
  '计算题',
  '编程题',
  '代码题',
  '综合题',
  '案例分析题',
]

const QUIZ_SECTION_TITLE_PATTERN = QUIZ_SECTION_TITLES.join('|')
const QUIZ_NUMBERED_SECTION_TITLE_PATTERN = `(?:[一二三四五六七八九十]+|\\d+)[、.．]\\s*(?:${QUIZ_SECTION_TITLE_PATTERN})`

export const normalizeQuizMarkdownLayout = (rawMarkdown: string) => {
  let text = String(rawMarkdown || '')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')

  // Some model outputs glue structural tokens onto the previous sentence:
  // "题干？A. 选项", "题干```java", "答案。2. 答案", "题干。---".
  text = text
    .replace(/([^\n])(```[a-zA-Z0-9_-]*)/g, '$1\n$2')
    .replace(/(```)([ \t]*(?:[A-H][.．、:：)）]))/g, '$1\n$2')
    .replace(new RegExp(`([^\\n])\\s*(#{1,6}\\s*(?:${QUIZ_NUMBERED_SECTION_TITLE_PATTERN}|${QUIZ_SECTION_TITLE_PATTERN}|答案解析区|参考答案与解析|参考答案|标准答案|答案解析))`, 'g'), '$1\n\n$2')
    .replace(new RegExp(`([^\\n])\\s*((?:${QUIZ_NUMBERED_SECTION_TITLE_PATTERN}|${QUIZ_SECTION_TITLE_PATTERN})\\s*(?:参考)?答案(?:与解析|解析)?)`, 'g'), '$1\n\n$2')
    .replace(/([^\n])\s*(---)(?=\s*(?:\n|#{1,6}\s*|答案解析区|参考答案|$))/g, '$1\n\n---')
    .replace(/([\u3002\uff01\uff1f\uff1b;])\s*(\d+[.．、]\s+)/g, '$1\n\n$2')
    .replace(/([)）])\s*(\d+[.．、]\s+)/g, '$1\n\n$2')
    .replace(/(】)\s*(\d+[.．、]\s+)/g, '$1\n\n$2')
    .replace(/([^\n])\s*([A-H][.．、:：)）]\s+)/g, '$1\n$2')

  return text
    .split('\n')
    .map(line => line.replace(/[ \t]+$/g, ''))
    .join('\n')
    .replace(/\n{4,}/g, '\n\n\n')
    .trim()
}

const isAnswerSectionHeading = (line: string) => {
  const text = normalizePaperLine(line)
  if (!text) return false
  return /^(答案解析区|参考答案(?:与解析)?|标准答案(?:与解析)?|答案(?:与解析|解析)?|解析部分|试题答案|答案汇总|附[:：]?\s*参考答案|参考解答)$/.test(text) ||
    /^(?:[一二三四五六七八九十]+|\d+)[、.．]?\s*(?:单项选择题|单选题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题)\s*(?:参考)?答案(?:与解析|解析)?$/.test(text) ||
    /^(?:单项选择题|单选题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题)\s*(?:参考)?答案(?:与解析|解析)?$/.test(text)
}

const isDashSeparator = (line: string) => {
  return /^-{3,}$/.test(normalizePaperLine(line))
}

const isLikelyAnswerLine = (line: string) => {
  const text = normalizePaperLine(line)
  return /^(\d+)[\.．、]\s*[A-H对错√×]\s*$/.test(text) || /^\d+\s*[：:]\s*[A-H对错√×]\s*$/.test(text)
}

export const splitPaperAndAnswers = (rawMarkdown: string): PaperSplitResult => {
  const rawText = normalizeQuizMarkdownLayout(rawMarkdown)
  const lines = rawText.split('\n')
  let cutIndex = -1

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const nextLine = lines[i + 1] || ''

    if (isAnswerSectionHeading(line)) {
      cutIndex = i
      break
    }

    if (isDashSeparator(line) && isAnswerSectionHeading(nextLine)) {
      cutIndex = i
      break
    }

    if (i > 0 && isLikelyAnswerLine(line) && /解析|答案|参考/.test(normalizePaperLine(lines[i - 1]))) {
      cutIndex = i - 1
      break
    }
  }

  if (cutIndex >= 0) {
    return {
      paper: lines.slice(0, cutIndex).join('\n').trim(),
      answers: lines.slice(cutIndex).join('\n').trim(),
    }
  }

  return {
    paper: rawText.trim(),
    answers: '老师未提供标准答案',
  }
}
