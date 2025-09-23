package com.digitalarkcorp.filestorage.api.errors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BindException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "bad_request",
                        "message", "Missing header: " + ex.getHeaderName()
                ));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "bad_request",
                        "message", "Invalid request"
                ));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "conflict",
                        "message", "Duplicate resource"
                ));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "forbidden",
                        "message", ex.getMessage() == null ? "Forbidden" : ex.getMessage()
                ));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "not_found",
                        "message", ex.getMessage() == null ? "Resource not found" : ex.getMessage()
                ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "conflict",
                        "message", ex.getMessage() == null ? "Conflict" : ex.getMessage()
                ));
    }

    @ExceptionHandler({ ResponseStatusException.class, ErrorResponseException.class })
    public ResponseEntity<Map<String, Object>> handleResponseStatus(Exception ex) {
        HttpStatus status;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            message = rse.getReason();
        } else {
            ErrorResponseException ere = (ErrorResponseException) ex;
            status = HttpStatus.resolve(ere.getStatusCode().value());
            message = ere.getBody() != null ? ere.getBody().getDetail() : null;
        }

        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (message == null || message.isBlank()) message = status.getReasonPhrase();

        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", toErrorKey(status),
                        "message", message
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "internal_error",
                        "message", "Unexpected error"
                ));
    }

    private String toErrorKey(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "bad_request";
            case FORBIDDEN -> "forbidden";
            case NOT_FOUND -> "not_found";
            case CONFLICT -> "conflict";
            default -> "error";
        };
    }
}
