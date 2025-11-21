package com.loopers.domain.like.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LikeProductRepository {
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<LikeProduct> findByUserIdAndProductId(Long userId, Long productId);

    void save(LikeProduct likeProduct);

    Page<LikeProduct> getLikeProductsByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);
}
