package com.loopers.infrastructure.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxTest {

    @Test
    @DisplayName("Outbox 생성 시 상태는 PENDING이어야 한다")
    void createTest1() {
        Outbox outbox = Outbox.create(
                "ORDER",
                "123",
                "OrderCompleted",
                "order-events",
                "{\"orderId\": 123}"
        );

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getAggregateType()).isEqualTo("ORDER");
        assertThat(outbox.getAggregateId()).isEqualTo("123");
        assertThat(outbox.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("markProcessed 호출 시 상태가 PROCESSED로 변경된다")
    void markProcessedTest1() {
        Outbox outbox = Outbox.create(
                "ORDER",
                "123",
                "OrderCompleted",
                "order-events",
                "{\"orderId\": 123}"
        );

        outbox.markProcessed();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(outbox.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 PROCESSED 상태에서 markProcessed 호출해도 멱등하게 동작한다")
    void markProcessedTest2() {
        Outbox outbox = Outbox.create(
                "ORDER",
                "123",
                "OrderCompleted",
                "order-events",
                "{\"orderId\": 123}"
        );

        outbox.markProcessed();
        LocalDateTime firstProcessedAt = outbox.getProcessedAt();

        outbox.markProcessed();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(outbox.getProcessedAt()).isEqualTo(firstProcessedAt);
    }
}
