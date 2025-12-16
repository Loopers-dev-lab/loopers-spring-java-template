package com.loopers.infrastructure.event;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.event.EventEntity;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
public interface EventJpaRepository extends JpaRepository<EventEntity, String> {
}
