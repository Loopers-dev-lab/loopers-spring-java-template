package com.loopers.interfaces.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.userbehavior.UserBehaviorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserBehaviorEventListener {

    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserBehaviorEvent.ProductViewed event) {
        logUserBehavior("PRODUCT_VIEWED", event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserBehaviorEvent.LikeRecorded event) {
        logUserBehavior("LIKE_RECORDED", event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserBehaviorEvent.LikeCancelled event) {
        logUserBehavior("LIKE_CANCELLED", event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserBehaviorEvent.OrderCreated event) {
        logUserBehavior("ORDER_CREATED", event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserBehaviorEvent.PaymentRequested event) {
        logUserBehavior("PAYMENT_REQUESTED", event);
    }

    private void logUserBehavior(String eventType, UserBehaviorEvent.BaseEvent event) {
        try {
            String json = objectMapper.writeValueAsString(createLogData(eventType, event));
            log.info("USER_BEHAVIOR: {}", json);
        } catch (JsonProcessingException e) {
            log.error("사용자 행동 이벤트 직렬화 실패: eventType={}, error={}", eventType, e.getMessage(), e);
        }
    }

    private UserBehaviorLogData createLogData(String eventType, UserBehaviorEvent.BaseEvent event) {
        return new UserBehaviorLogData(
                eventType,
                event.userId(),
                event.timestamp(),
                extractEventSpecificData(event)
        );
    }

    private Object extractEventSpecificData(UserBehaviorEvent.BaseEvent event) {
        if (event instanceof UserBehaviorEvent.ProductViewed productViewed) {
            return new ProductViewedData(productViewed.productId());
        } else if (event instanceof UserBehaviorEvent.LikeRecorded likeRecorded) {
            return new LikeRecordedData(likeRecorded.productId());
        } else if (event instanceof UserBehaviorEvent.LikeCancelled likeCancelled) {
            return new LikeCancelledData(likeCancelled.productId());
        } else if (event instanceof UserBehaviorEvent.OrderCreated orderCreated) {
            return new OrderCreatedData(orderCreated.orderKey(), orderCreated.originalTotalPrice(), orderCreated.discountPrice());
        } else if (event instanceof UserBehaviorEvent.PaymentRequested paymentRequested) {
            return new PaymentRequestedData(paymentRequested.orderKey(), paymentRequested.amount());
        }
        return null;
    }

    private record UserBehaviorLogData(
            String eventType,
            Long userId,
            java.time.ZonedDateTime timestamp,
            Object metadata
    ) {}

    private record ProductViewedData(Long productId) {}
    private record LikeRecordedData(Long productId) {}
    private record LikeCancelledData(Long productId) {}
    private record OrderCreatedData(String orderKey, Integer originalTotalPrice, Integer discountPrice) {}
    private record PaymentRequestedData(String orderKey, Integer amount) {}
}
