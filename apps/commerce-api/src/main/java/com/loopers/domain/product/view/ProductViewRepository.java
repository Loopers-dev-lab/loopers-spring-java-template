package com.loopers.domain.product.view;

import com.loopers.domain.product.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductViewRepository {

    Optional<ProductView> save(ProductView productView);
    Optional<ProductView> findById(Long id);
    List<ProductView> findByIds(List<Long> ids);
    void updateLikeCount(Long id, Long count);
    void update(Long id, String name, BigDecimal price, Long brandId, String brandName, ProductStatus status);
    void deleteById(Long id);
}

