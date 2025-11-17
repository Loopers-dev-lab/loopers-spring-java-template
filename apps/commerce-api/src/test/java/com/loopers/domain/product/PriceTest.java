package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PriceTest {

    @DisplayName("가격을 생성할 때,")
    @Nested
    class CreatePrice {

        @DisplayName("가격이 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsPrice_whenPriceIsValid() {
            // arrange
            Integer price = 10000;

            // act
            Price priceValue = Price.createPrice(price);

            // assert
            assertThat(priceValue.getPrice()).isEqualTo(price);
        }

        @DisplayName("가격이 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenPriceIsNull() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Price.createPrice(null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("가격은 필수입니다.");
        }

        @DisplayName("가격이 0 이하면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void throwsException_whenPriceIsZeroOrNegative(int invalidPrice) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Price.createPrice(invalidPrice);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("가격은 0보다 커야 합니다.");
        }
    }
}

