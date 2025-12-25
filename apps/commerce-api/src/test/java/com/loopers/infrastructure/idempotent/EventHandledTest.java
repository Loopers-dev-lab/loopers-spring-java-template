package com.loopers.infrastructure.idempotent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventHandledTest {

    @Test
    @DisplayName("EventHandled 생성 시 eventId와 handledAt이 설정된다")
    void createTest1() {
        EventHandled eventHandled = EventHandled.create("event-123");

        assertThat(eventHandled.getEventId()).isEqualTo("event-123");
        assertThat(eventHandled.getHandledAt()).isNotNull();
    }
}
