package com.loopers.application.api.error;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ApiErrorType apiErrorType;
    private final String customMessage;

    public ApiException(ApiErrorType apiErrorType) {
        this(apiErrorType, null);
    }

    public ApiException(ApiErrorType apiErrorType, String customMessage) {
        super(customMessage != null ? customMessage : apiErrorType.getMessage());
        this.apiErrorType = apiErrorType;
        this.customMessage = customMessage;
    }
}
