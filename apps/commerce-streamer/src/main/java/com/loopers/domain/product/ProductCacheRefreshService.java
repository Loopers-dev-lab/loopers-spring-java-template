package com.loopers.domain.product;

import com.loopers.infrastructure.product.ProductCacheRefresher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductCacheRefreshService {

    private final ProductInventoryService productInventoryService;
    private final ProductCacheRefresher productCacheRefresher;

    @Transactional(readOnly = true)
    public void refreshIfSoldOut(Long productId) {
        if (productId == null) {
            return;
        }
        Optional<ProductInventory> inventory = productInventoryService.findById(productId);
        inventory.ifPresent(product -> {
            Long stock = product.getStock();
            if (stock != null && stock <= 0) {
                productCacheRefresher.evict(product.getId(), product.getBrandId());
            }
        });
    }
}
