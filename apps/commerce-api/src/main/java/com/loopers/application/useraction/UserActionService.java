package com.loopers.application.useraction;

import com.loopers.domain.useraction.UserAction;
import com.loopers.domain.useraction.UserActionRepository;
import com.loopers.domain.useraction.UserActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 행동 추적 서비스
 * - 비동기로 사용자 행동을 로깅
 * - 핵심 비즈니스 로직과 독립적으로 동작
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionService {

    private final UserActionRepository userActionRepository;

    /**
     * 사용자 행동 로깅 (비동기)
     * - 독립적인 트랜잭션으로 실행
     * - 실패해도 핵심 비즈니스에 영향 없음
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String userId, UserActionType actionType, String targetId,
        String targetType, String metadata) {
        try {
            UserAction action = UserAction.builder()
                .userId(userId)
                .actionType(actionType)
                .targetId(targetId)
                .targetType(targetType)
                .metadata(metadata)
                .build();

            userActionRepository.save(action);

            log.debug("사용자 행동 로깅 완료 - userId: {}, actionType: {}, targetId: {}",
                userId, actionType, targetId);
        } catch (Exception e) {
            // 행동 로깅 실패는 핵심 비즈니스에 영향을 주지 않음
            log.error("사용자 행동 로깅 실패 - userId: {}, actionType: {}, error: {}",
                userId, actionType, e.getMessage());
        }
    }

    /**
     * 상품 조회 로깅
     */
    public void logProductView(String userId, Long productId) {
        logAction(userId, UserActionType.PRODUCT_VIEW, productId.toString(), "PRODUCT", null);
    }

    /**
     * 상품 클릭 로깅
     */
    public void logProductClick(String userId, Long productId) {
        logAction(userId, UserActionType.PRODUCT_CLICK, productId.toString(), "PRODUCT", null);
    }

    /**
     * 상품 좋아요 로깅
     */
    public void logProductLike(String userId, Long productId) {
        logAction(userId, UserActionType.PRODUCT_LIKE, productId.toString(), "PRODUCT", null);
    }

    /**
     * 상품 좋아요 취소 로깅
     */
    public void logProductUnlike(String userId, Long productId) {
        logAction(userId, UserActionType.PRODUCT_UNLIKE, productId.toString(), "PRODUCT", null);
    }

    /**
     * 주문 생성 로깅
     */
    public void logOrderCreate(String userId, Long orderId, String metadata) {
        logAction(userId, UserActionType.ORDER_CREATE, orderId.toString(), "ORDER", metadata);
    }
}
