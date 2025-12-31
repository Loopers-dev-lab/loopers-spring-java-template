package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    List<Product> findByIdIn(List<Long> ids);

    @Query(value = """
    SELECT
      p.id               AS id,
      p.name             AS name,
      p.stock_quantity   AS stockQuantity,
      p.price            AS price,
      b.name             AS brandName,
      p.like_count       AS likeCount
    FROM product p
    LEFT JOIN brand b ON b.id = p.brand_id
    WHERE (:brandId IS NULL OR p.brand_id = :brandId)
    ORDER BY p.created_at DESC, p.id DESC
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<ProductListView> findListLatest(
            @Param("brandId") Long brandId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
    SELECT
      p.id               AS id,
      p.name             AS name,
      p.stock_quantity   AS stockQuantity,
      p.price            AS price,
      b.name             AS brandName,
      p.like_count       AS likeCount
    FROM product p
    LEFT JOIN brand b ON b.id = p.brand_id
    WHERE (:brandId IS NULL OR p.brand_id = :brandId)
    ORDER BY p.price ASC, p.id DESC
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<ProductListView> findListPriceAsc(
            @Param("brandId") Long brandId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
    SELECT
      p.id               AS id,
      p.name             AS name,
      p.stock_quantity   AS stockQuantity,
      p.price            AS price,
      b.name             AS brandName,
      p.like_count       AS likeCount
    FROM product p
    LEFT JOIN brand b ON b.id = p.brand_id
    WHERE (:brandId IS NULL OR p.brand_id = :brandId)
    ORDER BY p.like_count DESC, p.id DESC
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<ProductListView> findListLikesDesc(
            @Param("brandId") Long brandId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
