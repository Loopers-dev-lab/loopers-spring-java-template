package com.loopers.domain.order.strategy;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentContext 테스트")
class PaymentContextTest {

    @Test
    @DisplayName("forMixed에서 totalAmount가 pointAmount + cardAmount와 일치하면 생성 성공")
    void test1() {
        // arrange & act
        PaymentContext context = PaymentContext.forMixed(
            1L, 1L, "user1", 10000L, 3000L, 7000L, "VISA", "1234"
        );

        // assert
        assertThat(context.totalAmount()).isEqualTo(10000L);
        assertThat(context.pointAmount()).isEqualTo(3000L);
        assertThat(context.cardAmount()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("forMixed에서 totalAmount != pointAmount + cardAmount 이면 예외 발생")
    void test2() {
        assertThatThrownBy(() -> PaymentContext.forMixed(
            1L, 1L, "user1", 10000L, 3000L, 8000L, "VISA", "1234"
        ))
        .isInstanceOf(CoreException.class)
        .hasMessageContaining("금액");
    }

    @Test
    @DisplayName("builder에서 totalAmount != pointAmount + cardAmount 이면 예외 발생")
    void test3() {
        assertThatThrownBy(() -> PaymentContext.builder()
            .orderId(1L)
            .paymentId(1L)
            .userId("user1")
            .totalAmount(10000L)
            .pointAmount(3000L)
            .cardAmount(6000L)  // 3000 + 6000 = 9000 != 10000
            .cardType("VISA")
            .cardNo("1234")
            .build()
        )
        .isInstanceOf(CoreException.class)
        .hasMessageContaining("금액");
    }

    @Test
    @DisplayName("forPointOnly는 totalAmount == pointAmount 이어야 함")
    void test4() {
        PaymentContext context = PaymentContext.forPointOnly(1L, 1L, "user1", 10000L);

        assertThat(context.totalAmount()).isEqualTo(10000L);
        assertThat(context.pointAmount()).isEqualTo(10000L);
        assertThat(context.cardAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("forCardOnly는 totalAmount == cardAmount 이어야 함")
    void test5() {
        PaymentContext context = PaymentContext.forCardOnly(1L, 1L, "user1", 10000L, "VISA", "1234");

        assertThat(context.totalAmount()).isEqualTo(10000L);
        assertThat(context.pointAmount()).isEqualTo(0L);
        assertThat(context.cardAmount()).isEqualTo(10000L);
    }
}
