package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.domain.order.event.OrderCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventHandlerTest {

    @Mock
    private OutboxRepository outboxRepository;

    private ObjectMapper objectMapper;

    private OutboxEventHandler outboxEventHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        outboxEventHandler = new OutboxEventHandler(outboxRepository, objectMapper);
    }

    @Test
    @DisplayName("ProductLikedEvent 발생 시 Outbox에 저장된다")
    void handleProductLikedEventTest1() {
        ProductLikedEvent event = ProductLikedEvent.liked(1L, 100L);

        outboxEventHandler.handleProductLikedEvent(event);

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());

        Outbox saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("PRODUCT");
        assertThat(saved.getAggregateId()).isEqualTo("1");
        assertThat(saved.getEventType()).isEqualTo("ProductLikedEvent");
        assertThat(saved.getTopic()).isEqualTo("product-liked");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
