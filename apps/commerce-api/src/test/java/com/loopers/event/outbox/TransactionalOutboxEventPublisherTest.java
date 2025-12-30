package com.loopers.event.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.order.event.OrderOutboxEvent;
import com.loopers.domain.stock.event.StockOutboxEvent;
import com.loopers.infrastructure.coupon.event.CouponOutboxEventRepository;
import com.loopers.infrastructure.like.event.LikeOutboxEventRepository;
import com.loopers.infrastructure.order.event.OrderOutboxEventRepository;
import com.loopers.infrastructure.payment.event.PaymentOutboxEventRepository;
import com.loopers.infrastructure.product.event.ProductOutboxEventRepository;
import com.loopers.infrastructure.stock.event.StockOutboxEventRepository;
import com.loopers.shared.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalOutboxEventPublisherTest {

    @Mock
    private OrderOutboxEventRepository orderOutboxEventRepository;
    @Mock
    private StockOutboxEventRepository stockOutboxEventRepository;
    @Mock
    private PaymentOutboxEventRepository paymentOutboxEventRepository;
    @Mock
    private CouponOutboxEventRepository couponOutboxEventRepository;
    @Mock
    private ProductOutboxEventRepository productOutboxEventRepository;
    @Mock
    private LikeOutboxEventRepository likeOutboxEventRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionalOutboxEventPublisher eventPublisher;

    @Test
    @DisplayName("ORDER 이벤트를 OrderOutboxEvent에 저장한다")
    void publish_orderEvent_savesToOrderOutbox() throws Exception {
        // given
        String topic = "order.created.v1";
        String key = "100";
        TestDomainEvent event = new TestDomainEvent("test-event-id", "ORDER", key);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventId\":\"test-event-id\",\"aggregateType\":\"ORDER\"}");

        // when
        eventPublisher.publish(topic, key, event);

        // then
        verify(orderOutboxEventRepository).save(any(OrderOutboxEvent.class));
    }

    @Test
    @DisplayName("STOCK 이벤트를 StockOutboxEvent에 저장한다")
    void publish_stockEvent_savesToStockOutbox() throws Exception {
        // given
        String topic = "stock.deducted.v1";
        String key = "200";
        TestDomainEvent event = new TestDomainEvent("test-event-id", "STOCK", key);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventId\":\"test-event-id\",\"aggregateType\":\"STOCK\"}");

        // when
        eventPublisher.publish(topic, key, event);

        // then
        verify(stockOutboxEventRepository).save(any(StockOutboxEvent.class));
    }

    /**
     * 테스트용 DomainEvent 구현체
     */
    private static class TestDomainEvent implements DomainEvent {
        private final String eventId;
        private final String aggregateType;
        private final String partitionKey;
        private final LocalDateTime occurredAt;

        public TestDomainEvent(String eventId, String aggregateType, String partitionKey) {
            this.eventId = eventId;
            this.aggregateType = aggregateType;
            this.partitionKey = partitionKey;
            this.occurredAt = LocalDateTime.now();
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public String getAggregateType() {
            return aggregateType;
        }

        @Override
        public String getPartitionKey() {
            return partitionKey;
        }

        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }
}

