package com.novel.exception;

/** 请求的资源不存在（或草稿章节对读者不可见）。 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
