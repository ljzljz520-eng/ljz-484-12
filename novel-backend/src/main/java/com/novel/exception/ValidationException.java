package com.novel.exception;

import java.util.List;

/**
 * 章节发布校验失败时抛出。
 * errors 中保存全部校验问题，便于一次性提示作者。
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join("；", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
