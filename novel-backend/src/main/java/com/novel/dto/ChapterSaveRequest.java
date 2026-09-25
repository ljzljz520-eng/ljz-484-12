package com.novel.dto;

/**
 * 作者保存章节（草稿）请求。
 * 草稿允许内容不完整，因此这里不做 Bean Validation，
 * 但字段仍需存在（不能是 null）。
 */
public class ChapterSaveRequest {
    private String title;
    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
