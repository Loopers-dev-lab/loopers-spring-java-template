package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeJpaRepository extends JpaRepository<Like, Long>, LikeRepository {

    @Override
    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.productId = :productId AND l.deletedAt IS NULL")
    Optional<Like> findByUserIdAndProductId(@Param("userId") String userId,
        @Param("productId") Long productId);

    @Override
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Like l WHERE l.userId = :userId AND l.productId = :productId AND l.deletedAt IS NULL")
    boolean existsByUserIdAndProductId(@Param("userId") String userId,
        @Param("productId") Long productId);

    @Override
    @Modifying
    @Query("UPDATE Like l SET l.deletedAt = CURRENT_TIMESTAMP WHERE l.userId = :userId AND l.productId = :productId AND l.deletedAt IS NULL")
    void deleteByUserIdAndProductId(@Param("userId") String userId,
        @Param("productId") Long productId);

    @Override
    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.deletedAt IS NULL ORDER BY l.createdAt DESC")
    Page<Like> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Override
    @Query("SELECT COUNT(l) FROM Like l WHERE l.productId = :productId AND l.deletedAt IS NULL")
    Long countByProductId(@Param("productId") Long productId);

    @Override
    default Map<Long, Long> countByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = findLikeCountsByProductIds(productIds);
        Map<Long, Long> likeCountMap = new HashMap<>();

        for (Object[] result : results) {
            Long productId = (Long) result[0];
            Long count = (Long) result[1];
            likeCountMap.put(productId, count);
        }

        return likeCountMap;
    }

    @Query("SELECT l.productId, COUNT(l) FROM Like l WHERE l.productId IN :productIds AND l.deletedAt IS NULL GROUP BY l.productId")
    List<Object[]> findLikeCountsByProductIds(@Param("productIds") List<Long> productIds);
}
