package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    int countByProductId(Long productId);

    int countByUserIdAndProductId(Long userId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.productId = :productId")
    Optional<Like> findByUserIdAndProductIdForUpdate(Long userId, Long productId);
}
