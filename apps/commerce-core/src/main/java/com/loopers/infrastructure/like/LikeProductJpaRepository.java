package com.loopers.infrastructure.like;

import com.loopers.domain.like.product.LikeProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface LikeProductJpaRepository extends JpaRepository<LikeProduct, Long> {
    @Lock(LockModeType.OPTIMISTIC)
    Optional<LikeProduct> findByUserIdAndProductId(Long userId, Long productId);

    Page<LikeProduct> getLikeProductsByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);
}
