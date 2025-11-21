package com.loopers.domain.point;

import com.loopers.domain.common.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointTest {

    @DisplayName("포인트를 생성할 때, ")
    @Nested
    class Save {
        @DisplayName("0으로 초기화에 성공한다.")
        @Test
        void succeeds_whenPointIsCreatedWithZeroBalance() {
            // arrange + act
            Point point = Point.create(1L);

            // assert
            assertThat(point.getBalance().getAmount()).isEqualByComparingTo("0");
        }
    }

    @DisplayName("포인트를 충전할 때, ")
    @Nested
    class Charge {
        @DisplayName("포인트 0원 충전 시 실패한다.")
        @Test
        void fails_whenAmountIsZeroOrNegative() {
            // arrange
            Point point = Point.create(1L);

            // act + assert
            assertThatThrownBy(() -> point.charge(Money.of(0)))
                    .hasMessageContaining("충전 금액은 0보다 커야합니다.");
        }

        @DisplayName("충전금액이 1 이상일 때, 포인트가 정상 충전된다.")
        @Test
        void succeeds_whenAmountIsPositive() {
            // arrange
            Point point = Point.create(1L);

            // act
            point.charge(Money.of(1000));

            // assert
            assertThat(point.getBalance().getAmount()).isEqualByComparingTo("1000");
        }
    }

    @DisplayName("포인트를 사용할 때, ")
    @Nested
    class Use {

        @DisplayName("사용하려는 포인트가 잔액보다 많을 때 실패한다.")
        @Test
        void fails_whenUseAmountExceedsPointBalance() {

            // arrange
            Point point = Point.create(1L, Money.of(1000));

            // act + assert
            assertThatThrownBy(() -> point.use(Money.of(1001)))
                    .hasMessageContaining("잔여 포인트가 부족합니다.");
        }

        @DisplayName("사용하려는 포인트가 0 이상, 잔액이 사용하려는 포인트보다 많을 경우 성공한다.")
        @Test
        void succeeds_whenAmountIsGreaterThanZero() {
            // arrange
            Point point = Point.create(1L, Money.of(1000));

            // act + assert
            point.use(Money.of(1000));

            // assert
            assertThat(point.getBalance().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
