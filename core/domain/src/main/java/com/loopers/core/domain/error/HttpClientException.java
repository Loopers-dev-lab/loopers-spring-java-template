package com.loopers.core.domain.error;

public class HttpClientException extends RuntimeException {

    private final HttpStatus status;
    private final String message;

    private HttpClientException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public static HttpClientException of(int status, String message) {
        HttpStatus httpStatus = new HttpStatus(status);

        if (httpStatus.isNotFound()) {
            return new NotFound(message);
        }

        if (httpStatus.isBadRequest()) {
            return new BadRequest(message);
        }

        if (httpStatus.isInternalServerError()) {
            return new InternalServerError(message);
        }

        if (httpStatus.isServiceUnavailable()) {
            return new ServiceUnavailable(message);
        }

        if (httpStatus.isUnprocessableEntity()) {
            return new UnprocessableEntity(message);
        }

        if (httpStatus.isGatewayTimeout()) {
            return new GatewayTimeout(message);
        }

        if (httpStatus.isTooManyRequests()) {
            return new TooManyRequests(httpStatus, message);
        }

        return new HttpClientException(httpStatus, message);
    }

    public static class NotFound extends HttpClientException {

        public NotFound(String message) {
            super(HttpStatus.notFound(), message);
        }
    }

    public static class BadRequest extends HttpClientException {

        public BadRequest(String message) {
            super(HttpStatus.badRequest(), message);
        }
    }

    public static class InternalServerError extends HttpClientException {

        public InternalServerError(String message) {
            super(HttpStatus.internalServerError(), message);
        }
    }

    public static class ServiceUnavailable extends HttpClientException {

        public ServiceUnavailable(String message) {
            super(HttpStatus.serviceUnavailable(), message);
        }
    }

    public static class UnprocessableEntity extends HttpClientException {

        private UnprocessableEntity(String message) {
            super(HttpStatus.unprocessableEntity(), message);
        }
    }

    public static class GatewayTimeout extends HttpClientException {

        public GatewayTimeout(String message) {
            super(HttpStatus.gatewayTimeout(), message);
        }
    }

    public static class TooManyRequests extends HttpClientException {

        private TooManyRequests(HttpStatus status, String message) {
            super(HttpStatus.tooManyRequests(), message);
        }
    }
}
