package com.digitalarkcorp.filestorage.api.errors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> nf(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err("not_found", e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> fb(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err("forbidden", e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> se(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err("forbidden", e.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> cf(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err("conflict", e.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> dk(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err("conflict", "duplicate"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<?> mh(MissingRequestHeaderException e) {
        return ResponseEntity.badRequest().body(err("bad_request", "Missing header: " + e.getHeaderName()));
    }

    @ExceptionHandler({ IllegalArgumentException.class, BindException.class, MethodArgumentNotValidException.class })
    public ResponseEntity<?> badInput(Exception e) {
        return ResponseEntity.badRequest().body(err("bad_request", "validation error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> all(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err("internal_error", "unexpected error"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> hmnre(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(err("bad_request", "invalid request payload"));
    }

    private Map<String,String> err(String code, String msg) {
        return Map.of("error", code, "message", msg);
    }
}
