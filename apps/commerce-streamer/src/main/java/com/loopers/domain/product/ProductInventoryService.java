package com.loopers.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductInventoryService {

    private final ProductInventoryRepository productInventoryRepository;

    @Transactional(readOnly = true)
    public Optional<ProductInventory> findById(Long productId) {
        return productInventoryRepository.findById(productId);
    }
}
