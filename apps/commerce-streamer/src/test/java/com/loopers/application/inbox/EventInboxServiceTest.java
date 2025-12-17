package com.loopers.application.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.inbox.EventInboxRepository;
import com.loopers.testcontainers.RedisTestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(RedisTestContainersConfig.class)
class EventInboxServiceTest {

    @Autowired
    private EventInboxService eventInboxService;

    @Autowired
    private EventInboxRepository eventInboxRepository;

    @BeforeEach
    void setUp() {
        eventInboxRepository.deleteAll();
    }

    @Test
    void 중복_이벤트를_감지한다() {
        // Given
        String eventId = "duplicate-test-001";
        eventInboxService.save(eventId, "ORDER", "123", "OrderCreatedEvent");

        // When
        boolean isDuplicate = eventInboxService.isDuplicate(eventId);

        // Then
        assertThat(isDuplicate).isTrue();
    }

    @Test
    void 신규_이벤트는_중복이_아니다() {
        // Given
        String eventId = "new-event-001";

        // When
        boolean isDuplicate = eventInboxService.isDuplicate(eventId);

        // Then
        assertThat(isDuplicate).isFalse();
    }

    @Test
    void Inbox에_이벤트를_저장한다() {
        // Given
        String eventId = "save-test-001";
        String aggregateType = "PRODUCT";
        String aggregateId = "456";
        String eventType = "ProductViewedEvent";

        // When
        eventInboxService.save(eventId, aggregateType, aggregateId, eventType);

        // Then
        boolean exists = eventInboxRepository.existsByEventId(eventId);
        assertThat(exists).isTrue();
    }
}
