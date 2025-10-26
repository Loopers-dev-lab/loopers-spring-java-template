package com.loopers.application.api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

    @DisplayName("ErrorType 기반의 예외 생성 시, 별도의 메시지가 주어지지 않으면 ErrorType의 메시지를 사용한다.")
    @Test
    void messageShouldBeErrorTypeMessage_whenCustomMessageIsNull() {
        // arrange
        ApiErrorType[] apiErrorTypes = ApiErrorType.values();

        // act & assert
        for (ApiErrorType apiErrorType : apiErrorTypes) {
            ApiException exception = new ApiException(apiErrorType);
            assertThat(exception.getMessage()).isEqualTo(apiErrorType.getMessage());
        }
    }

    @DisplayName("ErrorType 기반의 예외 생성 시, 별도의 메시지가 주어지면 해당 메시지를 사용한다.")
    @Test
    void messageShouldBeCustomMessage_whenCustomMessageIsNotNull() {
        // arrange
        String customMessage = "custom message";

        // act
        ApiException exception = new ApiException(ApiErrorType.INTERNAL_ERROR, customMessage);

        // assert
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}
