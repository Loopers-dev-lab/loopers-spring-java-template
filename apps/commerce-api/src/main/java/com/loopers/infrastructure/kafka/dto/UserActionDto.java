package com.loopers.infrastructure.kafka.dto;

import java.util.UUID;

public record UserActionDto(
        String eventId,
        Long userId,
        String actionType,
        String targetType,
        Long targetId
) {
    public static UserActionDto of(Long userId, String actionType, String targetType, Long targetId) {
        return new UserActionDto(
                UUID.randomUUID().toString(),
                userId,
                actionType,
                targetType,
                targetId
        );
    }
}
