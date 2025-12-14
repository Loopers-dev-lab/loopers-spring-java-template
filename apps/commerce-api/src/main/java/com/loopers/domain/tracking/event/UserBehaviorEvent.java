package com.loopers.domain.tracking.event;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * 유저 행동 추적 이벤트
 * <p>
 * 상품 조회, 클릭, 좋아요, 주문 등 모든 유저 행동을 추적합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
public record UserBehaviorEvent(
        String eventType,           // "PRODUCT_VIEW", "PRODUCT_CLICK", "LIKE_ACTION", "ORDER_CREATE" 등
        Long userId,                // 사용자 ID (비로그인 시 null)
        Long targetId,              // 대상 ID (상품 ID, 주문 ID 등)
        String targetType,          // 대상 타입 ("PRODUCT", "ORDER", "USER" 등)
        Map<String, Object> properties, // 추가 속성 (검색어, 카테고리, 금액 등)
        ZonedDateTime eventTime,    // 이벤트 발생 시간
        String source              // 이벤트 발생 위치 ("WEB", "MOBILE_APP", "API" 등)
) {

    // 상품 조회 이벤트
    public static UserBehaviorEvent productView(
            Long userId,
            Long productId,
            Map<String, Object> properties
    ) {
        return new UserBehaviorEvent(
                "PRODUCT_VIEW",
                userId,
                productId,
                "PRODUCT",
                properties,
                ZonedDateTime.now(),
                "WEB"
        );
    }

    // 좋아요 액션 이벤트
    public static UserBehaviorEvent likeAction(
            Long userId,
            Long productId,
            String action // "LIKE" or "UNLIKE"
    ) {
        return new UserBehaviorEvent(
                "LIKE_ACTION",
                userId,
                productId,
                "PRODUCT",
                Map.of("action", action),
                ZonedDateTime.now(),
                "WEB"
        );
    }

    // 주문 생성 이벤트
    public static UserBehaviorEvent orderCreate(
            Long userId,
            Long orderId,
            Map<String, Object> properties
    ) {
        return new UserBehaviorEvent(
                "ORDER_CREATE",
                userId,
                orderId,
                "ORDER",
                properties,
                ZonedDateTime.now(),
                "WEB"
        );
    }
}
