package com.loopers.application.auditlog;

public record AuditLogCommand(
        String eventId,
        Long userId,
        String actionType,
        String targetType,
        Long targetId,
        String payload
) {
    public static AuditLogCommand of(String payload, String eventType) {
        return new AuditLogCommand(
                null,
                null,
                eventType,
                null,
                null,
                payload
        );
    }

    public String eventType() {
        return actionType;
    }
}
