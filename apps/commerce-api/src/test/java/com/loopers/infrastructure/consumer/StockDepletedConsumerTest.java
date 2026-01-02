package com.loopers.infrastructure.consumer;

import com.loopers.domain.product.event.StockDepletedEvent;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.infrastructure.idempotent.EventHandledRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDepletedConsumerTest {

    @Mock
    private EventHandledRepository eventHandledRepository;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private Acknowledgment acknowledgment;

    private StockDepletedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new StockDepletedConsumer(eventHandledRepository, productCacheService);
    }

    @Test
    @DisplayName("재고 소진 이벤트 수신 시 상품 캐시를 무효화한다")
    void consumeTest1() {
        StockDepletedEvent event = StockDepletedEvent.of(1L);
        when(eventHandledRepository.existsByEventId("200")).thenReturn(false);

        consumer.consume(event, "200", acknowledgment);

        verify(productCacheService).deleteProductDetail(1L);
        verify(productCacheService).invalidateProductListCaches();
        verify(eventHandledRepository).save(argThat(e -> e.getEventId().equals("200")));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 무시하고 ack만 한다")
    void consumeTest2() {
        StockDepletedEvent event = StockDepletedEvent.of(1L);
        when(eventHandledRepository.existsByEventId("200")).thenReturn(true);

        consumer.consume(event, "200", acknowledgment);

        verify(productCacheService, never()).deleteProductDetail(any());
        verify(eventHandledRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }
}
