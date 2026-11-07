package com.interview.controller;

import com.interview.dto.ErrorResponse;
import com.interview.exception.DuplicateQuestionException;
import com.interview.exception.InvalidQuestionException;
import com.interview.exception.QuestionNotFoundException;
import com.interview.exception.QuestionStorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(QuestionNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(DuplicateQuestionException.class)
    public ResponseEntity<ErrorResponse> duplicate(DuplicateQuestionException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(InvalidQuestionException.class)
    public ResponseEntity<ErrorResponse> invalid(InvalidQuestionException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fields.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Validation failed.", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Request body must be valid JSON.", null);
    }

    @ExceptionHandler(QuestionStorageException.class)
    public ResponseEntity<ErrorResponse> storage(QuestionStorageException exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, Map<String, String> fields) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, fields);
        return ResponseEntity.status(status).body(body);
    }

}
