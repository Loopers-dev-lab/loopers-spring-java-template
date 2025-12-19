package com.loopers.application.event.tracking;

import java.time.LocalDateTime;
import java.util.Map;

public record UserActionEvent(
    String action,
    Long memberId,
    String resourceType,
    String resourceId,
    Map<String, Object> metadata,
    LocalDateTime occurredAt
) {
    public static UserActionEvent of(String action, Long memberId, String resourceType, String resourceId, Map<String, Object> metadata) {
        return new UserActionEvent(action, memberId, resourceType, resourceId, metadata, LocalDateTime.now());
    }
}
