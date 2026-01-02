package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductViewLogJpaRepository extends JpaRepository<ProductViewLog, Long> {
}
