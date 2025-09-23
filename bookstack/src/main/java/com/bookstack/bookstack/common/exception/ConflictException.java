package com.bookstack.bookstack.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends HttpException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
