package com.novel.dto;

/**
 * 作者提交章节时的请求体。
 * publish=false 仅保存草稿（不做发布校验）；publish=true 必须通过发布校验后才能标记为已发布。
 */
public class ChapterRequest {
    private String title;
    private String content;
    private boolean publish;

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

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }
}
