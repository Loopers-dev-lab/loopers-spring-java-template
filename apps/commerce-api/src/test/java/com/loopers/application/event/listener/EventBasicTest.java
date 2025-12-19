package com.loopers.application.event.listener;

import com.loopers.application.event.order.OrderPlacedEvent;
import com.loopers.domain.common.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("1단계: 이벤트 발행/수신 기본 테스트")
class EventBasicTest {

    private ApplicationEventPublisher eventPublisher;
    private TestEventListener testListener;

    @BeforeEach
    void setUp() {
        testListener = new TestEventListener();
        // Spring Context 없이 간단한 테스트용 Publisher
        eventPublisher = event -> {
            if (event instanceof OrderPlacedEvent orderEvent) {
                testListener.handleImmediate(orderEvent);
            }
        };
    }

    @Test
    @DisplayName("이벤트가 정상적으로 발행되고 리스너가 수신한다")
    void 이벤트_발행_및_수신() {
        // given
        testListener.reset();
        OrderPlacedEvent event = new OrderPlacedEvent(
            "ORD-123",
            1L,
            null,
            Money.of(10000),
            LocalDateTime.now()
        );

        // when
        eventPublisher.publishEvent(event);

        // then
        assertThat(testListener.isReceived()).isTrue();
        assertThat(testListener.getReceivedOrderNo()).isEqualTo("ORD-123");
    }

    @Test
    @DisplayName("이벤트에 쿠폰이 포함되어 있는지 확인한다")
    void 이벤트_쿠폰_포함_확인() {
        // given
        OrderPlacedEvent eventWithCoupon = new OrderPlacedEvent(
            "ORD-456",
            2L,
            100L,  // 쿠폰 있음
            Money.of(20000),
            LocalDateTime.now()
        );

        OrderPlacedEvent eventWithoutCoupon = new OrderPlacedEvent(
            "ORD-789",
            3L,
            null,  // 쿠폰 없음
            Money.of(30000),
            LocalDateTime.now()
        );

        // then
        assertThat(eventWithCoupon.hasCoupon()).isTrue();
        assertThat(eventWithoutCoupon.hasCoupon()).isFalse();
    }

    /**
     * 테스트용 리스너
     */
    public static class TestEventListener {
        private final AtomicBoolean received = new AtomicBoolean(false);
        private String receivedOrderNo;

        public void handleImmediate(OrderPlacedEvent event) {
            received.set(true);
            receivedOrderNo = event.orderNo();
        }

        public boolean isReceived() {
            return received.get();
        }

        public String getReceivedOrderNo() {
            return receivedOrderNo;
        }

        public void reset() {
            received.set(false);
            receivedOrderNo = null;
        }
    }
}
