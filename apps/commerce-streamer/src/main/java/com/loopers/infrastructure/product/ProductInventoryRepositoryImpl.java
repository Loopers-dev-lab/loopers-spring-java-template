package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductInventory;
import com.loopers.domain.product.ProductInventoryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductInventoryRepositoryImpl implements ProductInventoryRepository {

    private final ProductInventoryJpaRepository productInventoryJpaRepository;

    @Override
    public Optional<ProductInventory> findById(Long productId) {
        return productInventoryJpaRepository.findById(productId);
    }
}
