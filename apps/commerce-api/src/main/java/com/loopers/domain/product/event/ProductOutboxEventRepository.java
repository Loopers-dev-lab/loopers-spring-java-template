package com.loopers.domain.product.event;

import com.loopers.domain.event.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductOutboxEventRepository extends BaseOutboxEventRepository<ProductOutboxEvent> {
}

