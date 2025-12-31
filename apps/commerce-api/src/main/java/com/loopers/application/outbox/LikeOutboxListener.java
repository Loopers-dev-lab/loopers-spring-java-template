package com.loopers.application.outbox;

import com.loopers.domain.like.event.LikeCreatedEvent;
import com.loopers.domain.like.event.LikeDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeOutboxListener {

    private final OutboxEventService outboxEventService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleCreated(LikeCreatedEvent event) {
        outboxEventService.saveEvent(
                "LIKE",
                String.valueOf(event.productId()),
                "LIKE_CREATED",
                String.valueOf(event.productId()),
                Map.of("userId", event.userId(), "productId", event.productId())
        );
        log.info("Outbox 기록 - LIKE_CREATED productId={}, userId={}",
                event.productId(), event.userId());
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleDeleted(LikeDeletedEvent event) {
        outboxEventService.saveEvent(
                "LIKE",
                String.valueOf(event.productId()),
                "LIKE_DELETED",
                String.valueOf(event.productId()),
                Map.of("userId", event.userId(), "productId", event.productId())
        );
        log.info("Outbox 기록 - LIKE_DELETED productId={}, userId={}",
                event.productId(), event.userId());
    }
}



