package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductLikeCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductLikeCountJpaRepository extends JpaRepository<ProductLikeCount, Long> {

    @Modifying
    @Query("""
        UPDATE ProductLikeCount plc
           SET plc.likeCount = plc.likeCount + 1
         WHERE plc.id = :id
    """)
    int increaseLikeCountById(Long id);

    @Modifying
    @Query("""
        UPDATE ProductLikeCount plc
           SET plc.likeCount = plc.likeCount - 1
         WHERE plc.id = :id
           AND plc.likeCount > 0
    """)
    int decreaseLikeCountById(Long id);

    @Modifying
    @Query(value = """
        INSERT INTO product_like_counts (product_id, like_count)
        VALUES (:id, 1)
        ON DUPLICATE KEY UPDATE like_count = like_count + 1
        """,
        nativeQuery = true
    )
    int upsertLikeCount(Long id);

    @Query("SELECT plc FROM ProductLikeCount plc WHERE (:brandId IS NULL OR plc.brandId = :brandId)")
    Page<ProductLikeCount> findPageOrderByLikeCountDesc(@Param("brandId") Long brandId, Pageable pageRequest);

}
