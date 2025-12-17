package com.loopers.infrastructure.dlq;

import com.loopers.domain.dlq.DeadLetterQueue;
import com.loopers.domain.dlq.DeadLetterQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * DeadLetterQueue Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class DeadLetterQueueRepositoryImpl implements DeadLetterQueueRepository {

    private final DeadLetterQueueJpaRepository jpaRepository;

    @Override
    public DeadLetterQueue save(DeadLetterQueue deadLetterQueue) {
        return jpaRepository.save(deadLetterQueue);
    }
}
