package com.loopers.domain.event;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
public interface EventRepository {
    EventEntity save(EventEntity eventEntity);

    void deleteAll();

    boolean existsById(String eventId);
}
