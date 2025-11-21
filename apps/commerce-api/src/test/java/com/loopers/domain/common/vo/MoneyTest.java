package com.loopers.domain.common.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MoneyTest {

    @DisplayName("금액을 저장할 때, ")
    @Nested
    class Save {

        @DisplayName("금액이 0 이면 정상 저장된다.")
        @Test
        void succeeds_whenMoneyIsZero() {
            // arrange + act
            Money money = Money.zero();

            // assert
            assertThat(money.getAmount()).isEqualTo(BigDecimal.ZERO);
        }

        @DisplayName("금액이 0 보다 크면 정상 저장된다.")
        @ParameterizedTest
        @ValueSource(strings = {"0.00001", "100.234", "10000"})
        void succeeds_whenMoneyIsPositive(String validAmount) {
            // arrange + act
            Money money = Money.of(validAmount);

            // assert
            assertThat(money.getAmount()).isEqualByComparingTo(validAmount);

        }

        @DisplayName("금액이 0 보다 작으면 저장 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"-0.00001", "-100.234", "-10000"})
        void fails_whenMoneyIsLessThanZero(String invalidAmount) {
            // arrange + act + assert
            assertThatThrownBy(() -> Money.of(invalidAmount))
                    .hasMessageContaining("금액은 0 이상이어야 합니다.");

        }
    }

    @DisplayName("금액을 증가시킬 때, ")
    @Nested
    class Increase {

        @DisplayName("금액이 0 이면 변동 없다.")
        @Test
        void succeeds_whenMoneyIsZero() {
            // arrange
            Money original = Money.of("100.123");

            // act
            Money result = original.plus(Money.zero());

            // assert
            assertThat(result.getAmount()).isEqualTo(original.getAmount());
        }

        @DisplayName("금액이 0 보다 크면, 정상적으로 증가된다.")
        @ParameterizedTest
        @ValueSource(strings = {"0.00001", "100.123", "100"})
        void succeeds_whenMoneyIsPositive(String validAmount) {
            // arrange
            Money original = Money.of("100.123");
            Money target = Money.of(validAmount);

            // act
            Money result = original.plus(target);

            // assert
            assertThat(result.getAmount()).isEqualTo(original.getAmount().add(target.getAmount()));
        }

    }

    @DisplayName("금액을 차감할 때, ")
    @Nested
    class Decrease {

        @DisplayName("금액이 0 이면 변동 없다.")
        @Test
        void succeeds_whenMoneyIsZero() {
            // arrange
            Money original = Money.of("100.123");

            // act
            Money result = original.minus(Money.zero());

            // assert
            assertThat(result.getAmount()).isEqualTo(original.getAmount());

        }

        @DisplayName("금액이 0 보다 크면, 정상적으로 차감된다.")
        @ParameterizedTest
        @ValueSource(strings = {"0.00001", "100.123", "100"})
        void succeeds_whenMoneyIsPositive(String validAmount) {
            // arrange
            Money original = Money.of("100.123");
            Money target = Money.of(validAmount);

            // act
            Money result = original.minus(target);

            // assert
            assertThat(result.getAmount()).isEqualTo(original.getAmount().subtract(target.getAmount()));

        }
    }

    @DisplayName("금액을 비교할 때, ")
    @Nested
    class Compare {

        @DisplayName("금액이 비교 대상보다 크거나 같으면 true를 반환한다.")
        @Test
        void returnsTrue_whenAmountIsGreaterOrEqual() {
            // arrange
            Money a = Money.of("100.00");
            Money b = Money.of("100.00");
            Money c = Money.of("50.00");

            // act + assert
            assertThat(a.isGreaterThanOrEqual(b)).isTrue();
            assertThat(a.isGreaterThanOrEqual(c)).isTrue();
        }

        @DisplayName("금액이 비교 대상보다 작으면 false를 반환한다.")
        @Test
        void returnsFalse_whenAmountIsLess() {
            // arrange
            Money a = Money.of("50.00");
            Money b = Money.of("100.00");

            // act + assert
            assertThat(a.isGreaterThanOrEqual(b)).isFalse();
        }

        @DisplayName("금액이 비교 대상보다 크면 true를 반환한다.")
        @Test
        void returnsTrue_whenAmountIsGreater() {
            // arrange
            Money a = Money.of("100.00");
            Money b = Money.of("50.00");

            // act + assert
            assertThat(a.isGreaterThan(b)).isTrue();
        }

        @DisplayName("금액이 비교 대상보다 같거나 작으면 false를 반환한다.")
        @Test
        void returnsFalse_whenAmountIsLessOrEqual() {
            // arrange
            Money a = Money.of("100.00");
            Money b = Money.of("100.00");

            // act + assert
            assertThat(a.isGreaterThan(b)).isFalse();
        }
    }
}
