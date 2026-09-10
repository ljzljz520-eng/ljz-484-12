/**
 * 章节发布前校验规则（与后端 ChapterValidator 保持一致，修改时两边同步）。
 * 前端校验用于即时提示作者先修改；后端发布接口会重复校验一次。
 */

/** 正文最少字数（不计空白字符，中文按字计） */
export const MIN_CONTENT_LENGTH = 50

/** 统计正文字数：忽略所有空白字符（含全角空格） */
export function countWords(content) {
  if (!content) return 0
  return content.replace(/\s+/g, '').length
}

// 模板占位符，如 {{书名}}、${author}
const TEMPLATE_RE = /\{\{[^{}]*\}\}|\$\{[^{}]*\}/g
// 常见英文占位标记（整词匹配，不区分大小写）
const TODO_RE = /\b(TODO|FIXME|TBD)\b/gi
// 连续 3 个及以上 X/x，如 XXX、xxxx
const XXX_RE = /[Xx]{3,}/
// 常见中文占位词
const CN_PLACEHOLDER_KEYWORDS = ['待补充', '待定', '占位', '此处省略']

/** 找出文本中所有未替换的占位符（去重，保持出现顺序） */
export function findPlaceholders(text) {
  if (!text) return []
  const found = []
  const push = (v) => {
    if (!found.includes(v)) found.push(v)
  }
  TEMPLATE_RE.lastIndex = 0
  TODO_RE.lastIndex = 0
  let m
  while ((m = TEMPLATE_RE.exec(text)) !== null) push(m[0])
  while ((m = TODO_RE.exec(text)) !== null) push(m[0].toUpperCase())
  if (XXX_RE.test(text)) push('XXX')
  for (const kw of CN_PLACEHOLDER_KEYWORDS) {
    if (text.includes(kw)) push(kw)
  }
  return found
}

/**
 * 发布前校验。
 * @returns {string[]} 错误信息列表；为空表示校验通过，可以发布
 */
export function validateChapterForPublish(title, content) {
  const errors = []

  if (!title || !title.trim()) {
    errors.push('章节标题不能为空')
  }

  const words = countWords(content)
  if (words < MIN_CONTENT_LENGTH) {
    errors.push(`章节正文太短（当前 ${words} 字，至少需要 ${MIN_CONTENT_LENGTH} 字）`)
  }

  const titlePlaceholders = findPlaceholders(title)
  if (titlePlaceholders.length > 0) {
    errors.push(`标题包含未替换的占位符：${titlePlaceholders.join('、')}`)
  }

  const contentPlaceholders = findPlaceholders(content)
  if (contentPlaceholders.length > 0) {
    errors.push(`正文包含未替换的占位符：${contentPlaceholders.join('、')}`)
  }

  return errors
}
