package com.loopers.domain.product;

import java.util.Optional;

public interface ProductInventoryRepository {

    Optional<ProductInventory> findById(Long productId);
}
