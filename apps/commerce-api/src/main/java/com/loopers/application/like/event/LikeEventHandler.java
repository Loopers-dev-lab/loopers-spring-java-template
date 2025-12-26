package com.loopers.application.like.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.common.event.DomainEventEnvelop;
import com.loopers.domain.common.event.DomainEventRepository;
import com.loopers.domain.product.ProductLikeCountService;
import com.loopers.support.json.JsonConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LikeEventHandler {

    private final ProductLikeCountService productLikeCountService;
    private final DomainEventRepository eventRepository;
    private final JsonConverter jsonConverter;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void handleOutboxEvent(LikeCreatedEvent event) {

        DomainEventEnvelop<LikeCreatedEvent> envelop =
                DomainEventEnvelop.of(
                        "LIKE_CREATED",
                        "PRODUCT",
                        event.productId(),
                        event
                );

        eventRepository.save(
                DomainEvent.pending(
                        "catalog-events",
                        envelop,
                        jsonConverter.serialize(envelop)
                )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void handleOutboxEvent(LikeCanceledEvent event) {

        DomainEventEnvelop<LikeCanceledEvent> envelop =
                DomainEventEnvelop.of(
                        "LIKE_CANCELED",
                        "PRODUCT",
                        event.productId(),
                        event
                );

        eventRepository.save(
                DomainEvent.pending(
                        "catalog-events",
                        envelop,
                        jsonConverter.serialize(envelop)
                )
        );
    }

    /**
     * 좋아요가 새로 생성된 경우, 트랜잭션 커밋 이후 집계 카운트를 1 증가시킵니다.
     * @param event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void handleLikeCreated(LikeCreatedEvent event) {
        productLikeCountService.reflectLike(event.productId());
    }

    /**
     * 좋아요가 취소된 경우, 트랜잭션 커밋 이후 집계 카운트를 1 감소시킵니다.
     * @param event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void handleLikeCanceled(LikeCanceledEvent event) {
        productLikeCountService.reflectUnlike(event.productId());
    }
}
