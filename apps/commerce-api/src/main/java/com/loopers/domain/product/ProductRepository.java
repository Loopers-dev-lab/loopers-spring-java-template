package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> save(Product product);
    Optional<Product> findById(Long productId);
    Page<ProductView> findProductViews(ProductCondition condition, Pageable pageable);
    Optional<ProductView> findProductViewById(Long productId);
    void incrementLikeCount(Long productId);
    void decrementLikeCount(Long productId);
    
}
