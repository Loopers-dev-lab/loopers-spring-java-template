package com.loopers.infrastructure.dlq;

import com.loopers.domain.dlq.DeadLetterQueue;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DeadLetterQueue JPA Repository
 */
public interface DeadLetterQueueJpaRepository extends JpaRepository<DeadLetterQueue, Long> {
}
