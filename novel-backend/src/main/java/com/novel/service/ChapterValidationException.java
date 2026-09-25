package com.novel.service;

import java.util.List;

/**
 * 章节发布校验失败时抛出，携带每个字段对应的错误提示，供前端逐条展示。
 */
public class ChapterValidationException extends RuntimeException {

    private final List<FieldError> errors;

    public ChapterValidationException(List<FieldError> errors) {
        super("章节校验未通过");
        this.errors = errors;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
