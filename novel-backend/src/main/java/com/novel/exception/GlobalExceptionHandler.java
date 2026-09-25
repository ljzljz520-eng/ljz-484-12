package com.novel.exception;

import com.novel.service.ChapterValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 发布校验失败：400，并逐条返回字段错误（防止只靠浏览器校验被绕过）。 */
    @ExceptionHandler(ChapterValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ChapterValidationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "章节校验未通过，无法发布");
        body.put("details", ex.getErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", e.getMessage()))
                .toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
