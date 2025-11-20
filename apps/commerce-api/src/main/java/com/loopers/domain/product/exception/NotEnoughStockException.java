package com.loopers.domain.product.exception;

import com.loopers.support.error.ErrorType;

public class NotEnoughStockException extends RuntimeException {
    private final ErrorType errorType = ErrorType.BAD_REQUEST;

    public ErrorType getErrorType() {
        return errorType;
    }
    public NotEnoughStockException(String message) {
        super(message);
    }
}
