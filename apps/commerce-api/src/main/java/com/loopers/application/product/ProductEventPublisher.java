package com.loopers.application.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.OutboxEventService;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.kafka.AggregateTypes;
import com.loopers.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    /**
     * 상품 조회 이벤트 발행 (별도 트랜잭션)
     *
     * @Transactional(propagation = REQUIRES_NEW)
     * - 부모 트랜잭션(readOnly)과 독립적인 새로운 쓰기 트랜잭션 생성
     * - 이벤트 발행 실패 시에도 상품 조회는 성공 처리됨
     *
     * @param productId 조회된 상품 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishProductViewedEvent(Long productId) {
        try {
            ProductViewedEvent event = ProductViewedEvent.of(productId);
            String payload = objectMapper.writeValueAsString(event);

            outboxEventService.createOutboxEvent(
                    AggregateTypes.PRODUCT_VIEW,
                    productId.toString(),
                    KafkaTopics.ProductDetail.PRODUCT_VIEWED,
                    payload
            );

            log.debug("ProductViewedEvent 발행 성공 - productId: {}", productId);

        } catch (JsonProcessingException e) {
            // 이벤트 발행 실패 시 로그만 남기고 조회는 성공 처리
            log.error("ProductViewedEvent 직렬화 실패 - 상품 조회는 성공 처리됨. productId: {}",
                    productId, e);
        } catch (Exception e) {
            log.error("ProductViewedEvent 발행 실패 - 상품 조회는 성공 처리됨. productId: {}",
                    productId, e);
        }
    }
}
