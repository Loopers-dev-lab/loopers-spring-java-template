package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

public interface ProductOutboxEventJpaRepository extends JpaRepository<ProductOutboxEvent, Long> {
}
