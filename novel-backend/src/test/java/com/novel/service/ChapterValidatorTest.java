package com.novel.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChapterValidatorTest {

    private final ChapterValidator validator = new ChapterValidator();

    private String repeat(char c, int times) {
        return String.valueOf(c).repeat(times);
    }

    @Test
    void validChapterPasses() {
        String content = "他走进房间，看见屏幕上的光标在闪烁。" + repeat('内', 90);
        List<String> errors = validator.validate("第一章：觉醒", content);
        assertTrue(errors.isEmpty(), "标题正常且正文足够长时应通过，实际：" + errors);
    }

    @Test
    void emptyTitleIsRejected() {
        List<String> errors = validator.validate("   ", repeat('字', 200));
        assertTrue(errors.stream().anyMatch(e -> e.contains("标题不能为空")));
    }

    @Test
    void nullTitleAndContentAreRejected() {
        List<String> errors = validator.validate(null, null);
        assertEquals(2, errors.size());
    }

    @Test
    void tooShortContentIsRejectedWithCurrentCount() {
        List<String> errors = validator.validate("第一章", "太短了");
        assertTrue(errors.stream().anyMatch(e -> e.contains("正文太短") && e.contains("当前仅 3 字")));
    }

    @Test
    void whitespaceIsExcludedFromWordCount() {
        // 100 个有效字 + 大量空白，仍算 100 字，应通过
        String content = repeat('字', 100) + "\n\n   \t  ";
        assertEquals(100, ChapterValidator.countWords(content));
        assertTrue(validator.validate("标题", content).isEmpty());
    }

    @Test
    void mustachePlaceholdersAreDetectedInTitleAndContent() {
        List<String> titleErrors = validator.validate("{{chapter_title}}", repeat('字', 200));
        assertTrue(titleErrors.stream().anyMatch(e -> e.contains("占位符")));

        List<String> contentErrors = validator.validate("第一章", repeat('字', 200) + "{{ 正文内容 }}");
        assertTrue(contentErrors.stream().anyMatch(e -> e.contains("占位符")));
    }

    @Test
    void bracketAndKeywordPlaceholdersAreDetected() {
        String longText = repeat('字', 200);
        assertTrue(ChapterValidator.containsPlaceholder("正文【待补充】"));
        assertTrue(ChapterValidator.containsPlaceholder("【请输入章节标题】"));
        assertTrue(ChapterValidator.containsPlaceholder("这里还没写完，TODO"));
        assertTrue(ChapterValidator.containsPlaceholder("占位符"));
        assertFalse(ChapterValidator.containsPlaceholder(longText));
    }

    @Test
    void allErrorsAreReturnedAtOnce() {
        // 空标题 + 短正文 + 占位符：作者应一次看到全部问题
        List<String> errors = validator.validate("", "短 TODO");
        assertTrue(errors.size() >= 3, "应至少返回标题、字数、占位符三个问题，实际：" + errors);
    }
}
