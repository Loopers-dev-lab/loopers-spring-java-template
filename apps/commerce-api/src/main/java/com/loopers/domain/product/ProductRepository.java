package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> save(Product product);
    Optional<Product> findById(Long productId);
    Optional<Product> findByIdWithBrand(Long productId);
    Page<Product> findProducts(ProductCondition condition, Pageable pageable);
    void incrementLikeCount(Long productId);
    void decrementLikeCount(Long productId);
    
}
