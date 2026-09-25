package com.novel.model;

import java.time.LocalDateTime;

public class Chapter {
    private Long id;
    private Long novelId;
    private String title;
    private Integer orderNo;
    private String content;
    /** 章节状态：DRAFT（草稿，读者不可见）/ PUBLISHED（已发布） */
    private String status;
    private LocalDateTime createdAt;

    public Chapter() {
    }

    public Chapter(Long id, Long novelId, String title, Integer orderNo, String content, LocalDateTime createdAt) {
        this(id, novelId, title, orderNo, content, "PUBLISHED", createdAt);
    }

    public Chapter(Long id, Long novelId, String title, Integer orderNo, String content, String status, LocalDateTime createdAt) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.orderNo = orderNo;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
