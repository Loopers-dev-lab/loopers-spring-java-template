package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductInventoryJpaRepository extends JpaRepository<ProductInventory, Long> {
}
