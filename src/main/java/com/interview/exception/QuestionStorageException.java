package com.interview.exception;

public class QuestionStorageException extends RuntimeException {

    public QuestionStorageException(String message) {
        super(message);
    }

    public QuestionStorageException(String message, Throwable cause) {
        super(message, cause);
    }

}
