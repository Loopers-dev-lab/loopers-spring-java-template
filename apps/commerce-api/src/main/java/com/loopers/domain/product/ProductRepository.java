package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
    // 상품 목록 조회(다건)
    Page<ProductModel> findAll(Pageable pageable);

    // 브랜드로 상품 목록 조회(다건)
    Page<ProductModel> findByBrandName(String brandName, Pageable pageable);

    // 상품 상세 조회(단건)
    Optional<ProductModel> findById(Long id);

    // 상품 ID 목록으로 조회
    List<ProductModel> findAllById(Set<Long> ids);

}

