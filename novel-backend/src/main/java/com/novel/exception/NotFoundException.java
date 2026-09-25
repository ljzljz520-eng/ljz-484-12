package com.novel.exception;

/** 请求的小说 / 章节不存在。 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
