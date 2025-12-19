package com.loopers.domain.like.event;

import com.loopers.domain.event.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeOutboxEventRepository extends BaseOutboxEventRepository<LikeOutboxEvent> {
}

