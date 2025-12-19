package com.loopers.interfaces.api.listener;

import com.loopers.domain.like.LikeEvent;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.infrastructure.kafka.dto.LikeChangedDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeEventListener {

    private final OutboxService outboxService;

    @Value("${kafka.topic.product-like-name}")
    private String productLikeTopic;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLikeChangedOutboxEvent(LikeEvent event) {
        log.debug("좋아요 Outbox 이벤트 저장: productId={}, action={}",
                event.productId(), event.action());

        String likeType = switch (event.action()) {
            case ADDED -> "LIKED";
            case REMOVED -> "UNLIKED";
        };

        LikeChangedDto payload = LikeChangedDto.of(
                event.productId(),
                likeType
        );

        outboxService.saveEvent(
                "PRODUCT",
                event.productId().toString(),
                "LIKE_CHANGED",
                productLikeTopic,
                event.productId().toString(),
                payload
        );
    }
}
