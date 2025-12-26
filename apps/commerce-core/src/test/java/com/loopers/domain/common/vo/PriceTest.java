package com.loopers.domain.common.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("가격(Price) Value Object 테스트")
public class PriceTest {

    @DisplayName("가격을 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("정상적인 가격으로 생성할 수 있다. (Happy Path)")
        @Test
        void should_createPrice_when_validAmount() {
            // arrange
            int amount = 10000;

            // act
            Price price = new Price(amount);

            // assert
            assertThat(price.amount()).isEqualTo(10000);
        }

        @DisplayName("가격이 0이어도 생성할 수 있다. (Edge Case)")
        @Test
        void should_createPrice_when_amountIsZero() {
            // arrange
            int amount = 0;

            // act
            Price price = new Price(amount);

            // assert
            assertThat(price.amount()).isEqualTo(0);
        }

        @DisplayName("음수 가격으로 생성하면 예외가 발생한다. (Exception)")
        @ParameterizedTest
        @ValueSource(ints = {-1, -10, -100, -1000, -10000})
        void should_throwException_when_amountIsNegative(int invalidAmount) {
            // arrange: invalidAmount parameter

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new Price(invalidAmount);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("가격은 음수가 될 수 없습니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
