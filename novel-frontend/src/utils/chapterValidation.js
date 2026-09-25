// 章节发布校验（浏览器端版本）
//
// 规则必须与后端 com.novel.service.ChapterValidator 完全一致。
// 前端校验只为体验（即时提示、拦截发布动作），不是安全边界：
// 发布时 Java 后端会用同样的规则再校验一次。

// 章节发布要求的最低正文字数（不含空格、换行）
export const MIN_CONTENT_WORDS = 100

// {{标题}}、{{content}}、{{ chapter_content }} 等双花括号模板标记
const MUSTACHE_PLACEHOLDER = /\{\{[^{}]*\}\}/

// 【请输入章节标题】【待补充】【正文占位】等方括号提示语
const BRACKET_HINT = /【[^】]*(?:请输入|待补充|占位|标题|正文|内容|TODO|todo)[^】]*】/

// 裸占位提示：待补充 / 占位符 / TODO / FIXME / XXX
const BARE_KEYWORD = /待补充|占位符|\bTODO\b|\bFIXME\b|\bXXX\b/

/** 统计字数：去除所有空白字符后计数（中文按字，英文按字符，与后端一致） */
export function countWords(text) {
  if (!text) return 0
  return text.replace(/\s/g, '').length
}

/** 是否包含未替换的占位符 */
export function containsPlaceholder(text) {
  if (!text) return false
  return MUSTACHE_PLACEHOLDER.test(text)
    || BRACKET_HINT.test(text)
    || BARE_KEYWORD.test(text)
}

/**
 * 发布前完整校验。
 * @returns {string[]} 全部问题文案；空数组表示通过
 */
export function validateChapter(title, content) {
  const errors = []
  const safeTitle = (title || '').trim()

  if (!safeTitle) {
    errors.push('章节标题不能为空，请先填写标题')
  } else if (containsPlaceholder(safeTitle)) {
    errors.push('章节标题中仍有未替换的占位符（如 {{...}}、【请输入标题】等），请修改后再发布')
  }

  if (!content) {
    errors.push(`章节正文不能为空，且至少需要 ${MIN_CONTENT_WORDS} 字`)
  } else {
    const words = countWords(content)
    if (words < MIN_CONTENT_WORDS) {
      errors.push(`章节正文太短，至少需要 ${MIN_CONTENT_WORDS} 字（不含空格换行），当前仅 ${words} 字，请继续创作`)
    }
    if (containsPlaceholder(content)) {
      errors.push('正文中仍有未替换的占位符（如 {{...}}、【待补充】、TODO、占位符等），请修改后再发布')
    }
  }

  return errors
}
