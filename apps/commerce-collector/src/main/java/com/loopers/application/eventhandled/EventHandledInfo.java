package com.loopers.application.eventhandled;

public record EventHandledInfo(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId
) {
    public static EventHandledInfo of(String eventId, String eventType,
                                      String aggregateType, String aggregateId) {
        return new EventHandledInfo(eventId, eventType, aggregateType, aggregateId);
    }
}
