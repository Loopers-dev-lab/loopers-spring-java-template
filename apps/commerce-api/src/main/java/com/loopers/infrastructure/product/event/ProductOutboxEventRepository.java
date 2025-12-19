package com.loopers.infrastructure.product.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.domain.product.event.ProductOutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductOutboxEventRepository extends BaseOutboxEventRepository<ProductOutboxEvent> {
}

