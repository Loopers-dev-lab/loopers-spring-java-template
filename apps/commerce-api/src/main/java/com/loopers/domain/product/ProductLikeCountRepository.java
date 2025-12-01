package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductLikeCountRepository {

    Optional<ProductLikeCount> findById(Long productId);

    ProductLikeCount save(ProductLikeCount productLikeCount);

    int upsertLikeCount(Long id);

    int decreaseLikeCount(Long id);

    Page<ProductLikeCount> findPageOrderByLikeCountDesc(Long brandId, Pageable pageRequest);
}
