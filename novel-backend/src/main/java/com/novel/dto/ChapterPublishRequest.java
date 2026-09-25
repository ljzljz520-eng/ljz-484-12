package com.novel.dto;

/**
 * 作者发布章节请求。
 * 发布时允许同时提交最新的标题 / 正文，服务端会重新做完整校验，
 * 即使作者跳过了浏览器校验（禁用 JS、直接调接口）也无法发布不合格章节。
 */
public class ChapterPublishRequest {
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
