package com.loopers.domain.dlq;

/**
 * DeadLetterQueue Repository
 */
public interface DeadLetterQueueRepository {

    /**
     * DLQ 저장
     */
    DeadLetterQueue save(DeadLetterQueue deadLetterQueue);
}
