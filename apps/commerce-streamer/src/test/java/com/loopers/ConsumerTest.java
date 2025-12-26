package com.loopers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.infrastructure.event.EventHandledJpaRepository;
import com.loopers.infrastructure.productmetrics.ProductMetricsJpaRepository;
import com.loopers.interfaces.consumer.OrderMetricsConsumer;
import com.loopers.messaging.event.KafkaEventMessage;
import com.loopers.support.event.OrderPaidPayload;
import com.loopers.utils.DatabaseCleanUp;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsumerTest {

    @Autowired
    private OrderMetricsConsumer orderConsumer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductMetricsJpaRepository productMetricsRepository;

    @Autowired
    private EventHandledJpaRepository eventHandledJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables(); // 각 테스트 전에 테이블 초기화
    }

    @Test
    @DisplayName("같은 OrderPaid 이벤트가 2번 와도 판매량은 한 번만 증가한다")
    void idempotent_consume_test() throws Exception {

        // given
        UUID eventId = UUID.randomUUID();

        KafkaEventMessage<OrderPaidPayload> message =
                new KafkaEventMessage<>(
                        eventId,
                        "ORDER_PAID",
                        "ORDER",
                        100L,
                        new OrderPaidPayload(
                                List.of(
                                        new OrderPaidPayload.OrderItem(1L, 2, false),
                                        new OrderPaidPayload.OrderItem(2L, 1, false)
                                )
                        ),
                        Instant.now()
                );

        String payload = objectMapper.writeValueAsString(message);

        ConsumerRecord<Object, Object> record =
                new ConsumerRecord<>("order-events", 0, 0L, "100", payload);

        // when (중복 처리)
        orderConsumer.handle(record);
        orderConsumer.handle(record);

        // then
        ProductMetrics p1 =
                productMetricsRepository.findByProductId(1L).orElseThrow();
        ProductMetrics p2 =
                productMetricsRepository.findByProductId(2L).orElseThrow();

        assertAll(
                () -> assertThat(p1.getSalesCount()).isEqualTo(2),
                () -> assertThat(p2.getSalesCount()).isEqualTo(1),
                () -> assertThat(eventHandledJpaRepository.existsById(eventId)).isTrue()
        );
    }
}
