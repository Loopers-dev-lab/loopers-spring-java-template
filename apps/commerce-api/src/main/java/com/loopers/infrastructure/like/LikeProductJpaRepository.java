package com.loopers.infrastructure.like;

import com.loopers.domain.like.product.LikeProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface LikeProductJpaRepository extends JpaRepository<LikeProduct, Long> {
    Optional<LikeProduct> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Page<LikeProduct> getLikeProductsByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);
}
