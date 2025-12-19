package com.loopers.interfaces.api.listener;

import com.loopers.domain.like.LikeEvent;
import com.loopers.infrastructure.kafka.producer.LikeChangedEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeEventListener {

    private final LikeChangedEventProducer likeChangedEventProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeChangedKafkaEvent(LikeEvent event) {
        log.debug("좋아요 Kafka 이벤트 발행 시작: productId={}, action={}",
                event.productId(), event.action());

        try {
            String likeType = switch (event.action()) {
                case ADDED -> "LIKED";
                case REMOVED -> "UNLIKED";
            };

            likeChangedEventProducer.sendLikeChangedEvent(event.productId(), likeType);
        } catch (Exception e) {
            log.error("좋아요 Kafka 이벤트 발행 실패: productId={}", event.productId(), e);
        }
    }
}
