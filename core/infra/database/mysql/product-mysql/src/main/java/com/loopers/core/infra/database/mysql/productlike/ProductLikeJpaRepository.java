package com.loopers.core.infra.database.mysql.productlike;

import com.loopers.core.infra.database.mysql.productlike.entity.ProductLikeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLikeEntity, Long>, ProductLikeQuerydslRepository {

    Optional<ProductLikeEntity> findByUserIdAndProductId(Long userId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pl from ProductLikeEntity pl where pl.userId = :userId and pl.productId = :productId")
    Optional<ProductLikeEntity> findByUserIdAndProductIdWithLock(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
