package com.loopers.core.domain.error;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException withName(String name) {
        return new NotFoundException(DomainErrorCode.notFoundMessage(name));
    }
}
