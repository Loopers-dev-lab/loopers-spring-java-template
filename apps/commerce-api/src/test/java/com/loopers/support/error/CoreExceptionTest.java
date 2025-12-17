package com.loopers.support.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoreExceptionTest {
    @DisplayName("ErrorType 기반의 예외 생성 시, 별도의 메시지가 주어지지 않으면 ErrorType의 메시지를 사용한다.")
    @Test
    void messageShouldBeErrorTypeMessage_whenCustomMessageIsNull() {
        // arrange
        ErrorType[] errorTypes = ErrorType.values();

        // act & assert
        for (ErrorType errorType : errorTypes) {
            CoreException exception = new CoreException(errorType);
            assertThat(exception.getMessage()).isEqualTo(errorType.getMessage());
        }
    }

    @DisplayName("ErrorType 기반의 예외 생성 시, 별도의 메시지가 주어지면 해당 메시지를 사용한다.")
    @Test
    void messageShouldBeCustomMessage_whenCustomMessageIsNotNull() {
        // arrange
        String customMessage = "custom message";

        // act
        CoreException exception = new CoreException(ErrorType.INTERNAL_ERROR, customMessage);

        // assert
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}
