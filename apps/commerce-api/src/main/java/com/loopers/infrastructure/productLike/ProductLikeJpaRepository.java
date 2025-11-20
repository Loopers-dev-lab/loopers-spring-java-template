package com.loopers.infrastructure.productLike;

import com.loopers.domain.like.ProductLikeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLikeModel, Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId);
    ProductLikeModel save(ProductLikeModel like);
    void deleteByUserIdAndProductId(Long userPKId, Long productId);

    int countByProductId(Long productId);

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO product_like(user_id, product_id) VALUES (:userId, :productId)",
            nativeQuery = true)
    int insertIgnore(@Param("userId") Long userPkId, @Param("productId") Long productId);
}
