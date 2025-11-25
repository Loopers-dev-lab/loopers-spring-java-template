package com.loopers.domain.product;

import java.util.Optional;

public interface ProductViewRepository {

    Optional<ProductView> save(ProductView productView);
    Optional<ProductView> findById(Long id);
    void updateLikeCount(Long id, Long count);
    void deleteById(Long id);
}

