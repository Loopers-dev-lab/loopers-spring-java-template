package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDetailProjection;
import com.loopers.domain.product.ProductListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    @Query("""
        SELECT p.id AS id, p.name AS name, p.price AS price, p.brandId AS brandId,
        COALESCE(plc.likeCount, 0) AS likeCount, p.createdAt AS createdAt
          FROM Product p
          LEFT JOIN ProductLikeCount plc
            ON p.id = plc.productId
    """)
    Page<ProductListProjection> findAllWithLikeCount(Pageable pageRequest);

    @Query("""
        SELECT p.id AS id, p.name AS name, p.price AS price, p.brandId AS brandId,
               COALESCE(plc.likeCount, 0) AS likeCount, p.createdAt AS createdAt
          FROM Product p
          LEFT JOIN ProductLikeCount plc
            ON p.id = plc.productId
         WHERE p.brandId = :brandId
    """)
    Page<ProductListProjection> findAllByBrandIdWithLikeCount(@Param("brandId") Long brandId, Pageable pageRequest);

    @Query("""
        SELECT p.id AS id, p.name AS name, p.price AS price, p.brandId AS brandId,
               (SELECT COUNT(1) FROM Like l WHERE l.productId = p.id) AS likeCount,
        p.createdAt AS createdAt
          FROM Product p
    """)
    Page<ProductListProjection> findAllWithLikeCountV1(Pageable pageRequest);

    @Query("""
        SELECT p.id AS id, p.name AS name, p.price AS price, p.brandId AS brandId,
        (SELECT COUNT(1) FROM Like WHERE productId = p.id) AS likeCount, p.createdAt AS createdAt
          FROM Product p
         WHERE p.brandId = :brandId
    """)
    Page<ProductListProjection> findAllByBrandIdWithLikeCountV1(@Param("brandId") Long brandId, Pageable pageRequest);


    @Query("""
        SELECT p.id AS productId, p.name AS productName,
               p.brandId AS brandId, b.name AS brandName, p.price AS price,
               COALESCE(plc.likeCount, 0) AS likeCount
          FROM Product p
          LEFT JOIN ProductLikeCount plc
            ON p.id = plc.productId
          JOIN Brand b
            ON p.brandId = b.id
         WHERE p.id = :id
    """)
    Optional<ProductDetailProjection> findByIdWithBrandAndLikeCount(@Param("id") Long id);
}
