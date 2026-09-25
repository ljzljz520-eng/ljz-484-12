package com.novel.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 章节发布校验（服务端版本）。
 *
 * 发布章节必须同时满足：
 * 1. 标题非空（去掉空白后至少 1 个字）；
 * 2. 正文字数达到下限（按非空白字符计数，适配中文）；
 * 3. 标题与正文中不包含未替换的占位符。
 *
 * 浏览器端也会做同样的检查以改善体验，但该类是最终防线：
 * 任何绕过浏览器直接调用接口的请求都会在这里被拦截。
 * 前端 src/utils/chapterValidation.js 的规则必须与本类保持一致。
 */
@Component
public class ChapterValidator {

    /** 章节发布要求的最低正文字数（不含空格、换行）。 */
    public static final int MIN_CONTENT_WORDS = 100;

    // {{标题}}、{{content}}、{{ chapter_content }} 等双花括号模板标记
    private static final Pattern MUSTACHE_PLACEHOLDER =
            Pattern.compile("\\{\\{[^{}]*}}");

    // 【请输入章节标题】【待补充】【正文占位】等方括号提示语
    private static final Pattern BRACKET_HINT =
            Pattern.compile("【[^】]*(?:请输入|待补充|占位|标题|正文|内容|TODO|todo)[^】]*】");

    // 裸占位提示：待补充 / 占位符 / TODO / FIXME / XXX（英文词边界，避免误伤正常用词）
    private static final Pattern BARE_KEYWORD =
            Pattern.compile("(?:待补充|占位符|(?i:\\b(?:TODO|FIXME|XXX)\\b))");

    /**
     * 统计正文字数：去除所有空白字符后计数。
     * 中文按字计数，英文单词的字母也按字符计数，对作者更直观且规则稳定。
     */
    public static int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    /** 判断文本中是否存在未替换的占位符。 */
    public static boolean containsPlaceholder(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return MUSTACHE_PLACEHOLDER.matcher(text).find()
                || BRACKET_HINT.matcher(text).find()
                || BARE_KEYWORD.matcher(text).find();
    }

    /**
     * 执行发布前校验。
     *
     * @return 全部校验问题；返回空列表表示通过
     */
    public List<String> validate(String title, String content) {
        List<String> errors = new ArrayList<>();

        String safeTitle = title == null ? "" : title.trim();
        if (safeTitle.isEmpty()) {
            errors.add("章节标题不能为空，请先填写标题");
        } else if (containsPlaceholder(safeTitle)) {
            errors.add("章节标题中仍有未替换的占位符（如 {{...}}、【请输入标题】等），请修改后再发布");
        }

        if (content == null || content.isEmpty()) {
            errors.add("章节正文不能为空，且至少需要 " + MIN_CONTENT_WORDS + " 字");
        } else {
            int words = countWords(content);
            if (words < MIN_CONTENT_WORDS) {
                errors.add("章节正文太短，至少需要 " + MIN_CONTENT_WORDS + " 字（不含空格换行），当前仅 "
                        + words + " 字，请继续创作");
            }
            if (containsPlaceholder(content)) {
                errors.add("正文中仍有未替换的占位符（如 {{...}}、【待补充】、TODO、占位符等），请修改后再发布");
            }
        }

        return errors;
    }
}
