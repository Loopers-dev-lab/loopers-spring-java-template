package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxRepository;
import com.loopers.domain.outbox.OutboxStatus;
import com.loopers.domain.outbox.QOutboxEvent;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.loopers.domain.outbox.QOutboxEvent.outboxEvent;

@Component
public class OutboxRepositoryImpl implements OutboxRepository {
    private final OutboxJpaRepository outBoxJpaRepository;
    private final JPAQueryFactory queryFactory;

    public OutboxRepositoryImpl(OutboxJpaRepository outBoxJpaRepository, EntityManager entityManager) {
        this.outBoxJpaRepository = outBoxJpaRepository;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public OutboxEvent save(OutboxEvent outBoxEvent) {
        return outBoxJpaRepository.save(outBoxEvent);
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return queryFactory
                .selectFrom(outboxEvent)
                .where(outboxEvent.status.eq(OutboxStatus.PENDING))
                .orderBy(outboxEvent.createdAt.asc())
                .limit(limit)
                .fetch();
    }

}
