package com.loopers.interfaces.api.listener;

import com.loopers.domain.user.UserActionEvent;
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

    /**
     * 유저 행동 이벤트 → 데이터 플랫폼 전송
     * - 모든 유저 행동 로깅을 여기서 통합 처리
     */
    @Async
    @EventListener
    public void handleUserAction(UserActionEvent event) {
        log.info("[UserAction] type={}, loginId={}, target={}:{}, metadata={}",
                event.actionType(),
                event.userId(),
                event.targetType(),
                event.targetId(),
                event.metadata());

        try {
            if (isLikeAction(event)) {
                UserActionMessage message = convertToUserActionMessage(event);
                dataPlatformSender.sendUserAction(message);
            } else {
                // 주문/결제 등은 별도 메시지 타입으로 처리되므로 로깅만
                log.info("[DataPlatform] UserAction logged: type={}, loginId={}, targetId={}",
                        event.actionType(), event.userId(), event.targetId());
            }
        } catch (Exception e) {
            log.error("유저 행동 데이터 플랫폼 전송 실패: loginId={}, action={}, reason={}",
                    event.userId(), event.actionType(), e.getMessage());
        }
    }

    private boolean isLikeAction(UserActionEvent event) {
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
