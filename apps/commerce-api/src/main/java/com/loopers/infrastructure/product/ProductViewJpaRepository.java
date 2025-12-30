package com.loopers.infrastructure.product;

import com.loopers.domain.product.view.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductViewJpaRepository extends JpaRepository<ProductView, Long> {
}

