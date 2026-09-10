package com.novel.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 章节发布校验器。
 *
 * 前端页面在作者点击“发布”时会做同样的预校验并提示先修改，
 * 但后端必须重复校验一次，不能只信任浏览器的判断。
 * 注意：前端 src/utils/chapterValidation.js 与本类规则保持一致，
 * 修改规则时两边需要同步。
 */
@Component
public class ChapterValidator {

    /** 正文最少字数（不计空白字符，中文按字计） */
    public static final int MIN_CONTENT_LENGTH = 50;

    /** 模板占位符，如 {{书名}}、${author} */
    private static final Pattern TEMPLATE_PLACEHOLDER =
            Pattern.compile("\\{\\{[^{}]*\\}\\}|\\$\\{[^{}]*\\}");
    /** 常见英文占位标记（整词匹配，不区分大小写） */
    private static final Pattern TODO_PLACEHOLDER =
            Pattern.compile("(?i)\\b(TODO|FIXME|TBD)\\b");
    /** 连续 3 个及以上 X/x，如 XXX、xxxx */
    private static final Pattern XXX_PLACEHOLDER = Pattern.compile("[Xx]{3,}");
    /** 常见中文占位词 */
    private static final String[] CN_PLACEHOLDER_KEYWORDS = {"待补充", "待定", "占位", "此处省略"};

    /** 统计正文字数：忽略所有空白字符（含全角空格），中文按字计 */
    public int countWords(String content) {
        if (content == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if (!isBlankChar(content.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断空白字符。与前端 JS 的 \s 对齐：
     * Character.isWhitespace 之外，补上不间断空格等 JS 视为空白、
     * 而 Java 默认不视为空白的字符。
     */
    private static boolean isBlankChar(char c) {
        return Character.isWhitespace(c)
                || c == '\u00A0'   // NO-BREAK SPACE
                || c == '\u2007'   // FIGURE SPACE
                || c == '\u202F'   // NARROW NO-BREAK SPACE
                || c == '\uFEFF';  // ZERO WIDTH NO-BREAK SPACE (BOM)
    }

    /** 找出文本中所有未替换的占位符（去重，保持出现顺序） */
    public List<String> findPlaceholders(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isEmpty()) {
            return new ArrayList<>(found);
        }
        Matcher templateMatcher = TEMPLATE_PLACEHOLDER.matcher(text);
        while (templateMatcher.find()) {
            found.add(templateMatcher.group());
        }
        Matcher todoMatcher = TODO_PLACEHOLDER.matcher(text);
        while (todoMatcher.find()) {
            found.add(todoMatcher.group().toUpperCase());
        }
        if (XXX_PLACEHOLDER.matcher(text).find()) {
            found.add("XXX");
        }
        for (String keyword : CN_PLACEHOLDER_KEYWORDS) {
            if (text.contains(keyword)) {
                found.add(keyword);
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * 发布前校验。
     *
     * @param title   章节标题
     * @param content 章节正文
     * @return 错误信息列表；为空表示校验通过，可以标记为已发布
     */
    public List<String> validateForPublish(String title, String content) {
        List<String> errors = new ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            errors.add("章节标题不能为空");
        }

        int wordCount = countWords(content);
        if (wordCount < MIN_CONTENT_LENGTH) {
            errors.add("章节正文太短（当前 " + wordCount + " 字，至少需要 " + MIN_CONTENT_LENGTH + " 字）");
        }

        List<String> titlePlaceholders = findPlaceholders(title);
        if (!titlePlaceholders.isEmpty()) {
            errors.add("标题包含未替换的占位符：" + String.join("、", titlePlaceholders));
        }

        List<String> contentPlaceholders = findPlaceholders(content);
        if (!contentPlaceholders.isEmpty()) {
            errors.add("正文包含未替换的占位符：" + String.join("、", contentPlaceholders));
        }

        return errors;
    }
}
