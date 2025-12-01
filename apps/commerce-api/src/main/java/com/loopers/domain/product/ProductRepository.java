package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);

    List<Product> findAllById(List<Long> ids);

    Page<ProductListProjection> findAllWithLikeCount(Pageable pageRequest);

    Page<ProductListProjection> findAllByBrandIdWithLikeCount(Long brandId, Pageable pageRequest);

    Optional<ProductDetailProjection> findByIdWithBrandAndLikeCount(Long id);

    Product save(Product product);

    Page<ProductListProjection> findAllByBrandIdWithLikeCountV1(Long brandId, Pageable pageRequest);

    Page<ProductListProjection> findAllWithLikeCountV1(Pageable pageRequest);
}
