package com.novel.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChapterValidatorTest {

    private String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    private String validContent() {
        return "这是一段足够长的合法正文内容。".repeat(10);
    }

    @Test
    void passesWhenTitleAndContentAreValid() {
        assertDoesNotThrow(() -> ChapterValidator.validateForPublish("第一章：起源", validContent()));
    }

    @Test
    void rejectsNullAndBlankTitle() {
        ChapterValidationException ex = assertThrows(ChapterValidationException.class,
                () -> ChapterValidator.validateForPublish("   ", validContent()));
        assertTrue(ex.getErrors().stream().anyMatch(e -> "title".equals(e.getField())));
    }

    @Test
    void rejectsTooShortContent() {
        ChapterValidationException ex = assertThrows(ChapterValidationException.class,
                () -> ChapterValidator.validateForPublish("第一章", "太短了"));
        assertTrue(ex.getErrors().stream()
                .anyMatch(e -> "content".equals(e.getField()) && e.getMessage().contains("太短")));
    }

    @Test
    void wordCountIgnoresWhitespaceIncludingIdeographicSpace() {
        String text = repeat('字', 120) + " \t\n\r　";
        assertEquals(120, ChapterValidator.countWords(text));
        assertDoesNotThrow(() -> ChapterValidator.validateForPublish("标题", text));
    }

    @Test
    void rejectsTemplatePlaceholderInContent() {
        ChapterValidationException ex = assertThrows(ChapterValidationException.class,
                () -> ChapterValidator.validateForPublish("标题",
                        "正文写到这里，" + "{{chapter_content}}" + "。" + validContent()));
        assertTrue(ex.getErrors().stream()
                .anyMatch(e -> "content".equals(e.getField()) && e.getMessage().contains("{{chapter_content}}")));
    }

    @Test
    void rejectsBracketPlaceholderInTitleAndContent() {
        ChapterValidationException ex1 = assertThrows(ChapterValidationException.class,
                () -> ChapterValidator.validateForPublish("【请输入标题】", validContent()));
        assertTrue(ex1.getErrors().stream()
                .anyMatch(e -> "title".equals(e.getField()) && e.getMessage().contains("占位符")));

        ChapterValidationException ex2 = assertThrows(ChapterValidationException.class,
                () -> ChapterValidator.validateForPublish("标题", "（此处省略一万字）" + validContent()));
        assertTrue(ex2.getErrors().stream().anyMatch(e -> "content".equals(e.getField())));
    }

    @Test
    void rejectsNakedTodoAndCollectsMultipleErrorsAtOnce() {
        ChapterValidationException ex = assertThrows(ChapterValidationException.class,
                () -> ChapterValidator.validateForPublish("", "TODO"));
        // 标题为空 + 正文太短 + 占位符，三类错误应一次全部返回
        assertTrue(ex.getErrors().size() >= 3, "应同时返回所有字段错误，实际: " + ex.getErrors());
    }
}
