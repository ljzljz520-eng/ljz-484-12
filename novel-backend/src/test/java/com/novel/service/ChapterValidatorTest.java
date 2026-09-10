package com.novel.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterValidatorTest {

    private final ChapterValidator validator = new ChapterValidator();

    /** 100 字的合法正文 */
    private String longContent() {
        return "夜色渐深，".repeat(20);
    }

    @Test
    void passesForValidChapter() {
        List<String> errors = validator.validateForPublish("第一章：开端", longContent());
        assertTrue(errors.isEmpty());
    }

    @Test
    void rejectsEmptyTitle() {
        assertFalse(validator.validateForPublish(null, longContent()).isEmpty());
        assertFalse(validator.validateForPublish("", longContent()).isEmpty());
        assertFalse(validator.validateForPublish("   ", longContent()).isEmpty());
    }

    @Test
    void rejectsShortContent() {
        List<String> errors = validator.validateForPublish("标题", "太短了");
        assertTrue(errors.stream().anyMatch(e -> e.contains("正文太短")));
    }

    @Test
    void rejectsExactlyBelowMinimum() {
        String content49 = "字".repeat(ChapterValidator.MIN_CONTENT_LENGTH - 1);
        String content50 = "字".repeat(ChapterValidator.MIN_CONTENT_LENGTH);
        assertFalse(validator.validateForPublish("标题", content49).isEmpty());
        assertTrue(validator.validateForPublish("标题", content50).isEmpty());
    }

    @Test
    void rejectsPlaceholdersInContent() {
        String base = longContent();
        assertFalse(validator.validateForPublish("标题", base + "{{主角名}}").isEmpty());
        assertFalse(validator.validateForPublish("标题", base + "${name}").isEmpty());
        assertFalse(validator.validateForPublish("标题", base + "TODO: 这里要改").isEmpty());
        assertFalse(validator.validateForPublish("标题", base + "此处待补充。").isEmpty());
        assertFalse(validator.validateForPublish("标题", base + "XXX").isEmpty());
    }

    @Test
    void rejectsPlaceholdersInTitle() {
        List<String> errors = validator.validateForPublish("第X章 {{待填}}", longContent());
        assertTrue(errors.stream().anyMatch(e -> e.contains("占位符")));
    }

    @Test
    void countWordsIgnoresWhitespace() {
        // 含半角空格、换行与全角空格
        assertEquals(4, validator.countWords("一 二\n三　四"));
        // 不间断空格同样不计入字数（与前端 JS \s 对齐）
        assertEquals(2, validator.countWords("一\u00A0二"));
        assertEquals(0, validator.countWords(null));
        assertEquals(0, validator.countWords("   "));
    }
}
