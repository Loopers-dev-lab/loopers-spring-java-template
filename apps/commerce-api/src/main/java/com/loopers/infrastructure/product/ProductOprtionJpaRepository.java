package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductOptionModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOprtionJpaRepository extends JpaRepository<ProductOptionModel, Long> {
}
