package com.novel.model;

/**
 * 作者提交章节内容（新建草稿 / 保存修改）的请求体。
 */
public record ChapterRequest(String title, String content) {
}
