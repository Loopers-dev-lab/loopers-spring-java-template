package com.loopers.infrastructure.productlike;

import com.loopers.domain.productlike.LikedProduct;
import com.loopers.domain.productlike.ProductLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeQueryJpaRepository extends JpaRepository<ProductLike, Long> {

  @Query("""
      SELECT new com.loopers.domain.productlike.LikedProduct(
        p.id,
        p.name,
        p.price.value,
        p.likeCount,
        b.id,
        b.name,
        pl.likedAt
      )
      FROM ProductLike pl
      LEFT JOIN Product p ON p.id = pl.productId
      LEFT JOIN Brand b ON b.id = p.brandId
      WHERE pl.userId = :userId
      ORDER BY pl.likedAt DESC
      """)
  Page<LikedProduct> findByUserIdOrderByLatest(@Param("userId") Long userId, Pageable pageable);

  @Query("""
      SELECT new com.loopers.domain.productlike.LikedProduct(
        p.id,
        p.name,
        p.price.value,
        p.likeCount,
        b.id,
        b.name,
        pl.likedAt
      )
      FROM ProductLike pl
      LEFT JOIN Product p ON p.id = pl.productId
      LEFT JOIN Brand b ON b.id = p.brandId
      WHERE pl.userId = :userId
      ORDER BY p.name ASC
      """)
  Page<LikedProduct> findByUserIdOrderByProductName(@Param("userId") Long userId,
      Pageable pageable);

  @Query("""
      SELECT new com.loopers.domain.productlike.LikedProduct(
        p.id,
        p.name,
        p.price.value,
        p.likeCount,
        b.id,
        b.name,
        pl.likedAt
      )
      FROM ProductLike pl
      LEFT JOIN Product p ON p.id = pl.productId
      LEFT JOIN Brand b ON b.id = p.brandId
      WHERE pl.userId = :userId
      ORDER BY p.price.value ASC
      """)
  Page<LikedProduct> findByUserIdOrderByPriceAsc(@Param("userId") Long userId, Pageable pageable);

  @Query("""
      SELECT new com.loopers.domain.productlike.LikedProduct(
        p.id,
        p.name,
        p.price.value,
        p.likeCount,
        b.id,
        b.name,
        pl.likedAt
      )
      FROM ProductLike pl
      LEFT JOIN Product p ON p.id = pl.productId
      LEFT JOIN Brand b ON b.id = p.brandId
      WHERE pl.userId = :userId
      ORDER BY p.price.value DESC
      """)
  Page<LikedProduct> findByUserIdOrderByPriceDesc(@Param("userId") Long userId, Pageable pageable);
}
