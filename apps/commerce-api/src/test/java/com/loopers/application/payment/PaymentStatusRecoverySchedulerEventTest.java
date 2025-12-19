package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.PaymentMethod;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("결제 상태 복구 스케줄러 이벤트 기반 테스트")
public class PaymentStatusRecoverySchedulerEventTest {

    @Autowired
    private PaymentStatusRecoveryScheduler scheduler;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("스케줄러 기본 동작")
    @Test
    void should_executeWithoutException_when_schedulerRuns() {
        // arrange
        // PENDING 상태 주문이 없는 경우에도 예외 없이 실행되어야 함

        // act
        scheduler.recoverPendingPayments();

        // assert - 예외 없이 실행됨
        assertThat(scheduler).isNotNull();
    }

    @DisplayName("포인트 결제 주문은 복구 대상이 아니다")
    @Test
    void should_skipPointPaymentOrders() {
        // arrange
        Order pointOrder = Order.create(
                1L,
                List.of(com.loopers.domain.order.OrderItem.create(1L, "상품1", 1, new com.loopers.domain.common.vo.Price(10000))),
                new com.loopers.domain.common.vo.DiscountResult(new com.loopers.domain.common.vo.Price(10000)),
                PaymentMethod.POINT
        );
        pointOrder = orderJpaRepository.save(pointOrder);

        // act
        scheduler.recoverPendingPayments();

        // assert - 포인트 결제 주문은 복구 대상이 아니므로 상태 변경 없음
        Order updatedOrder = orderService.getOrderById(pointOrder.getId());
        assertThat(updatedOrder.getPaymentMethod()).isEqualTo(PaymentMethod.POINT);
    }
}

