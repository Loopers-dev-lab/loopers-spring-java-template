package com.loopers.infrastructure.like;

import com.loopers.domain.like.product.ProductLikeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLikeModel, Long> {
    Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    Page<ProductLikeModel> findByUserIdOrderByLikedAtDesc(Long userId, Pageable pageable);
}
