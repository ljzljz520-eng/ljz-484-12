// 章节发布前的前端预检。
// 规则必须与后端 ChapterValidator 保持一致；这里只是为了快速反馈，后端会再校验一次。
export const MIN_CONTENT_WORDS = 100

// 与后端同序的占位符规则
const PLACEHOLDER_PATTERNS = [
  /\{\{[^{}]*\}\}/g,                                                    // {{ title }}
  /<[A-Za-z0-9_一-龥]+>/g,                                              // <标题>
  /【[^】]*(?:待补充|待填写|待完善|省略|占位|TODO|请填写|请输入|此处|内容|正文|标题)[^】]*】/g,
  /（[^）]*(?:待补充|待填写|待完善|省略|占位|TODO|请填写|请输入|此处|内容|正文|标题)[^）]*）/g,
  /\[[^\]]*(?:TODO|FIXME|PLACEHOLDER|待补充|待填写|占位|请填写|请输入)[^\]]*\]/gi,
  /\([^)]*(?:TODO|FIXME|PLACEHOLDER|省略|占位|请填写|请输入|此处)[^)]*\)/gi,
  /(?:TODO|FIXME|PLACEHOLDER|待补充|待填写|待完善|此处省略)/gi
]

const MAX_SNIPPETS = 5
const SNIPPET_MAX_LEN = 20

/** 统计字数：去除所有空白字符（含中文全角空格 U+3000）。 */
export function countWords(text) {
  if (!text) return 0
  return text.replace(/\s/g, '').length
}

function findPlaceholders(text) {
  const snippets = []
  for (const re of PLACEHOLDER_PATTERNS) {
    re.lastIndex = 0
    let m
    while ((m = re.exec(text)) !== null) {
      let snippet = m[0].trim()
      if (snippet.length > SNIPPET_MAX_LEN) snippet = snippet.slice(0, SNIPPET_MAX_LEN) + '…'
      if (!snippets.includes(snippet)) snippets.push(snippet)
      if (snippets.length >= MAX_SNIPPETS) return snippets
    }
  }
  return snippets
}

/**
 * 发布前校验。
 * @returns {{ valid: boolean, errors: {field: 'title'|'content', message: string}[] }}
 */
export function validateChapter(title, content) {
  const errors = []
  const trimmedTitle = (title || '').trim()

  if (!trimmedTitle) {
    errors.push({ field: 'title', message: '章节标题不能为空，请先填写标题' })
  } else {
    const titlePlaceholders = findPlaceholders(trimmedTitle)
    if (titlePlaceholders.length > 0) {
      errors.push({
        field: 'title',
        message: `标题中含有未替换的占位符 “${titlePlaceholders[0]}”，请修改后再发布`
      })
    }
  }

  const words = countWords(content)
  if (words < MIN_CONTENT_WORDS) {
    errors.push({
      field: 'content',
      message: `章节正文太短：当前 ${words} 字，至少需要 ${MIN_CONTENT_WORDS} 字（不含空白），请补充内容后再发布`
    })
  }

  const contentPlaceholders = findPlaceholders(content || '')
  if (contentPlaceholders.length > 0) {
    errors.push({
      field: 'content',
      message: `正文中含有未替换的占位符：${contentPlaceholders.join('、')}，请修改后再发布`
    })
  }

  return { valid: errors.length === 0, errors }
}
