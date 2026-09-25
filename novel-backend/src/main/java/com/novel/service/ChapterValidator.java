package com.novel.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 章节发布前的服务端校验。
 *
 * 规则（与前端 src/utils/chapterValidation.js 保持一致，以后端为准）：
 *  1. 标题不能为空（纯空白也不允许）；
 *  2. 正文字数（去除所有空白字符后的字符数）必须达到 {@link #MIN_CONTENT_WORDS}；
 *  3. 标题与正文中不得残留未替换的占位符，例如 {{xxx}}、【待补充】、<请输入标题>、（此处省略）等。
 *
 * 校验全部通过后才允许把章节标记为已发布；任何一条不通过都会抛出
 * {@link ChapterValidationException}，浏览器端的判断不能替代这里的检查。
 */
public final class ChapterValidator {

    /** 发布所需的最少正文字数（不含空白字符）。 */
    public static final int MIN_CONTENT_WORDS = 100;

    /** 各种常见的未替换占位符形态，命中任意一个即拒绝发布。 */
    private static final Pattern[] PLACEHOLDER_PATTERNS = new Pattern[] {
            // 模板变量：{{ title }}、{{chapter_content}}
            Pattern.compile("\\{\\{[^{}]*}}"),
            // 尖括号占位：<标题>、<请在此输入正文>（支持中文内容）
            Pattern.compile("<[A-Za-z0-9_\\u4e00-\\u9fa5]+>"),
            // 全角方括号关键字：【待补充】【此处省略】【请填写】...
            Pattern.compile("【[^】]*(?:待补充|待填写|待完善|省略|占位|TODO|请填写|请输入|此处|内容|正文|标题)[^】]*】"),
            // 全角圆括号关键字：（此处省略N字）（请输入标题）
            Pattern.compile("（[^）]*(?:待补充|待填写|待完善|省略|占位|TODO|请填写|请输入|此处|内容|正文|标题)[^）]*）"),
            // 半角方括号关键字：[TODO] [placeholder]
            Pattern.compile("\\[[^\\]]*(?:TODO|FIXME|PLACEHOLDER|待补充|待填写|占位|请填写|请输入)[^\\]]*]", Pattern.CASE_INSENSITIVE),
            // 半角圆括号关键字：(此处省略) (placeholder)
            Pattern.compile("\\([^)]*(?:TODO|FIXME|PLACEHOLDER|省略|占位|请填写|请输入|此处)[^)]*\\)", Pattern.CASE_INSENSITIVE),
            // 裸关键字：xxx内容、正文xxx、标题xxx（仅针对明显的编辑提示语）
            Pattern.compile("(?:TODO|FIXME|PLACEHOLDER|待补充|待填写|待完善|此处省略)", Pattern.CASE_INSENSITIVE)
    };

    private static final int MAX_PLACEHOLDER_SNIPPETS = 5;
    private static final int SNIPPET_MAX_LEN = 20;

    private ChapterValidator() {
    }

    /**
     * 发布前校验。
     *
     * @throws ChapterValidationException 校验不通过，异常中携带全部错误信息（字段 -> 提示）
     */
    public static void validateForPublish(String title, String content) {
        List<ChapterValidationException.FieldError> errors = new ArrayList<>();

        // 1. 标题不能为空
        if (title == null || title.trim().isEmpty()) {
            errors.add(new ChapterValidationException.FieldError("title", "章节标题不能为空，请先填写标题"));
        } else {
            String titlePlaceholder = findFirstPlaceholder(title);
            if (titlePlaceholder != null) {
                errors.add(new ChapterValidationException.FieldError(
                        "title", "标题中含有未替换的占位符 \"" + titlePlaceholder + "\"，请修改后再发布"));
            }
        }

        // 2. 正文字数检查
        int wordCount = countWords(content);
        if (wordCount < MIN_CONTENT_WORDS) {
            errors.add(new ChapterValidationException.FieldError(
                    "content", "章节正文太短：当前 " + wordCount + " 字，至少需要 "
                            + MIN_CONTENT_WORDS + " 字（不含空白），请补充内容后再发布"));
        }

        // 3. 正文占位符检查（即使字数不足也要同时报出，方便作者一次改完）
        if (content != null) {
            List<String> placeholders = findPlaceholders(content);
            if (!placeholders.isEmpty()) {
                errors.add(new ChapterValidationException.FieldError(
                        "content", "正文中含有未替换的占位符：" + String.join("、", placeholders) + "，请修改后再发布"));
            }
        }

        if (!errors.isEmpty()) {
            throw new ChapterValidationException(errors);
        }
    }

    /**
     * 统计字数：去除全部空白字符（含中文全角空格 U+3000）后的字符数。
     */
    public static int countWords(String text) {
        if (text == null) {
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

    private static String findFirstPlaceholder(String text) {
        List<String> all = findPlaceholders(text);
        return all.isEmpty() ? null : all.get(0);
    }

    private static List<String> findPlaceholders(String text) {
        Set<String> snippets = new LinkedHashSet<>();
        for (Pattern pattern : PLACEHOLDER_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find() && snippets.size() < MAX_PLACEHOLDER_SNIPPETS) {
                String snippet = matcher.group().trim();
                if (snippet.length() > SNIPPET_MAX_LEN) {
                    snippet = snippet.substring(0, SNIPPET_MAX_LEN) + "…";
                }
                snippets.add(snippet);
            }
            if (snippets.size() >= MAX_PLACEHOLDER_SNIPPETS) {
                break;
            }
        }
        return new ArrayList<>(snippets);
    }
}
