package com.loopers.infrastructure.like;

import com.loopers.domain.like.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLike, Long> {

    @Query("SELECT pl FROM ProductLike pl WHERE pl.userId = :userId AND pl.productId = :productId AND pl.deletedAt IS NULL")
    Optional<ProductLike> findByUserIdAndProductId(Long userId, Long productId);
}
