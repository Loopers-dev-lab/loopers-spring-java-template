package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository {
    Page<ProductModel> findByStatusNot(ProductStatus status, Pageable pageable);
    Page<ProductModel> findByBrandId(Long brandId, Pageable pageable);
    Optional<ProductModel> findById(Long id);
    Optional<ProductModel> findByName(String name);
}
