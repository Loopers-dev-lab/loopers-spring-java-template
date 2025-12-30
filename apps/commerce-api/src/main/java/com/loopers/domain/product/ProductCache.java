package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductCache {

    Optional<Page<Product>> getProductList(Long brandId, Pageable pageable);

    void putProductList(Long brandId, Pageable pageable, Page<Product> products);

    Optional<Product> getProductDetail(Long productId);

    void putProductDetail(Long productId, Product product);
}
