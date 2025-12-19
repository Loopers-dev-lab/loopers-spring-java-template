package com.loopers.interfaces.api.listener;

import com.loopers.domain.user.UserActionEvent;
import com.loopers.infrastructure.kafka.producer.ProductViewedEventProducer;
import com.loopers.infrastructure.kafka.producer.UserActionEventProducer;
import com.loopers.infrastructure.platform.DataPlatformSender;
import com.loopers.infrastructure.platform.UserActionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionEventListener {

    private final DataPlatformSender dataPlatformSender;
    private final UserActionEventProducer userActionEventProducer;
    private final ProductViewedEventProducer productViewedEventProducer;

    /**
     * 유저 행동 이벤트 → 데이터 플랫폼 전송 + Kafka 이벤트 발행
     */
    @Async
    @EventListener
    public void handleUserAction(UserActionEvent event) {
        log.info("[UserAction] type={}, userId={}, target={}:{}, metadata={}",
                event.actionType(),
                event.userId(),
                event.targetType(),
                event.targetId(),
                event.metadata());

        try {
            if (isLikeOrViewAction(event)) {
                UserActionMessage message = convertToUserActionMessage(event);
                dataPlatformSender.sendUserAction(message);
            }
        } catch (Exception e) {
            log.error("DataPlatform 전송 실패: userId={}, actionType={}",
                    event.userId(), event.actionType(), e);
            // DataPlatform 실패는 무시하고 계속 진행
        }

        try {
            publishKafkaEvent(event);
        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패: userId={}, actionType={}",
                    event.userId(), event.actionType(), e);
        }
    }

    private void publishKafkaEvent(UserActionEvent event) {
        try {
            // 상품 조회 이벤트는 별도 Producer로 발행
            if (event.actionType() == UserActionEvent.ActionType.PRODUCT_VIEW) {
                productViewedEventProducer.sendProductViewedEvent(event.targetId());
            }

            // 모든 유저 행동은 UserAction 토픽으로 발행
            userActionEventProducer.sendUserActionEvent(
                    event.userId(),
                    event.actionType().name(),
                    event.targetType(),
                    event.targetId()
            );
        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패: userId={}, actionType={}",
                    event.userId(), event.actionType(), e);
        }
    }

    private boolean isLikeOrViewAction(UserActionEvent event) {
        return event.actionType() == UserActionEvent.ActionType.PRODUCT_LIKE
                || event.actionType() == UserActionEvent.ActionType.PRODUCT_UNLIKE
                || event.actionType() == UserActionEvent.ActionType.PRODUCT_VIEW;
    }

    private UserActionMessage convertToUserActionMessage(UserActionEvent event) {
        return switch (event.actionType()) {
            case PRODUCT_VIEW -> UserActionMessage.productView(event.userId(), event.targetId());
            case PRODUCT_LIKE -> UserActionMessage.productLike(event.userId(), event.targetId());
            case PRODUCT_UNLIKE -> UserActionMessage.productUnlike(event.userId(), event.targetId());
            default -> throw new IllegalArgumentException("Unsupported action type: " + event.actionType());
        };
    }
}
