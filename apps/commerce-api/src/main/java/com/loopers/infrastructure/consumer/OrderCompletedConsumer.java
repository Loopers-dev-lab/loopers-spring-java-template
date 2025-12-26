package com.loopers.infrastructure.consumer;

import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.infrastructure.idempotent.EventHandled;
import com.loopers.infrastructure.idempotent.EventHandledRepository;
import com.loopers.infrastructure.ranking.RankingRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedConsumer {

    private final EventHandledRepository eventHandledRepository;
    private final RankingRedisService rankingRedisService;

    @KafkaListener(topics = "order-events", groupId = "order-completed-consumer")
    @Transactional
    public void consume(
            @Payload OrderCompletedEvent event,
            @Header("outbox-id") String eventId,
            Acknowledgment ack
    ) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("이미 처리된 이벤트 무시: eventId={}", eventId);
            ack.acknowledge();
            return;
        }

        try {
            processOrderCompletedEvent(event);
            eventHandledRepository.save(EventHandled.create(eventId));
            ack.acknowledge();
            log.info("주문 완료 이벤트 처리 완료: eventId={}, orderId={}", eventId, event.getOrderId());
        } catch (Exception e) {
            log.error("주문 완료 이벤트 처리 실패, 재처리 예정: eventId={}", eventId, e);
        }
    }

    private void processOrderCompletedEvent(OrderCompletedEvent event) {
        for (OrderCompletedEvent.OrderItemInfo item : event.getItems()) {
            rankingRedisService.incrementScoreForOrder(
                    event.getOccurredAt().toLocalDate(),
                    item.getProductId(),
                    item.getQuantity()
            );
        }
    }
}
