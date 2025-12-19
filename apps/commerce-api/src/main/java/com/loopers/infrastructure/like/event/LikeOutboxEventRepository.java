package com.loopers.infrastructure.like.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.domain.like.event.LikeOutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeOutboxEventRepository extends BaseOutboxEventRepository<LikeOutboxEvent> {
}

