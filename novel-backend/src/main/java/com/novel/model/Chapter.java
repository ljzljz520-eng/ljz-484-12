package com.novel.model;

import java.time.LocalDateTime;

public class Chapter {
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    private Long id;
    private Long novelId;
    private String title;
    private Integer orderNo;
    private String content;
    private Integer wordCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public Chapter() {
    }

    public Chapter(Long id, Long novelId, String title, Integer orderNo, String content, LocalDateTime createdAt) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.orderNo = orderNo;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Chapter(Long id, Long novelId, String title, Integer orderNo, String content,
                   Integer wordCount, String status, LocalDateTime createdAt, LocalDateTime publishedAt) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.orderNo = orderNo;
        this.content = content;
        this.wordCount = wordCount;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isPublished() {
        return STATUS_PUBLISHED.equals(status);
    }
}
