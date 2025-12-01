package com.loopers.core.domain.error;

public record HttpStatus(int value) {

    private static final int NOT_FOUND = 404;
    private static final int BAD_REQUEST = 400;
    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int GATEWAY_TINEOUT = 504;

    public static HttpStatus notFound() {
        return new HttpStatus(NOT_FOUND);
    }

    public static HttpStatus badRequest() {
        return new HttpStatus(BAD_REQUEST);
    }

    public static HttpStatus internalServerError() {
        return new HttpStatus(INTERNAL_SERVER_ERROR);
    }

    public static HttpStatus serviceUnavailable() {
        return new HttpStatus(SERVICE_UNAVAILABLE);
    }

    public static HttpStatus unprocessableEntity() {
        return new HttpStatus(UNPROCESSABLE_ENTITY);
    }

    public static HttpStatus gatewayTimeout() {
        return new HttpStatus(GATEWAY_TINEOUT);
    }

    public boolean isNotFound() {
        return this.value == NOT_FOUND;
    }

    public boolean isBadRequest() {
        return this.value == BAD_REQUEST;
    }

    public boolean isInternalServerError() {
        return this.value == INTERNAL_SERVER_ERROR;
    }

    public boolean isServiceUnavailable() {
        return this.value == SERVICE_UNAVAILABLE;
    }

    public boolean isUnprocessableEntity() {
        return this.value == UNPROCESSABLE_ENTITY;
    }

    public boolean isGatewayTimeout() {
        return this.value == GATEWAY_TINEOUT;
    }
}
