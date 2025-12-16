package com.loopers.domain.activity.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 행동 추적 이벤트
 *
 * 사용자의 모든 행동을 추적하기 위한 이벤트
 */
@Builder
public record UserActivityEvent(
        String userId,
        String action,          // 행동 타입
        String resourceType,    // 리소스 타입 (ex. "PRODUCT", "ORDER")
        Long resourceId,        // 리소스 ID
        LocalDateTime timestamp
) {
    public static UserActivityEvent of(String userId, String action,
                                        String resourceType, Long resourceId) {
        return UserActivityEvent.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
