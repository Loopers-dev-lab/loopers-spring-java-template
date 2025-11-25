package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductViewJpaRepository extends JpaRepository<ProductView, Long> {
}

