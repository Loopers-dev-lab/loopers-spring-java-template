package com.loopers.infrastructure.like;

import com.loopers.domain.like.product.ProductLikeModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLikeModel, Long> {
    Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
