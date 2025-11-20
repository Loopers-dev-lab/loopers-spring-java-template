package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository {
    Page<ProductModel> findByStatusNot(ProductStatus status, Pageable pageable);
    Page<ProductModel> findByBrandId(Long brandId, Pageable pageable);
    Optional<ProductModel> findByIdForUpdate(Long id);
    Optional<ProductModel> findById(Long id);
    Optional<ProductModel> findByName(String name);
    boolean existsById(Long id);
    ProductModel save(ProductModel product);
}
