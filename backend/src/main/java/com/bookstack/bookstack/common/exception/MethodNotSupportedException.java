package com.bookstack.bookstack.common.exception;

import org.springframework.http.HttpStatus;

public class MethodNotSupportedException extends BaseException {
    public MethodNotSupportedException(String message) {
        super(message, HttpStatus.METHOD_NOT_ALLOWED);
    }
}