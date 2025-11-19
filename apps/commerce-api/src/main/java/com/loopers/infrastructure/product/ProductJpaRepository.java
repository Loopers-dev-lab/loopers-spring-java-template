package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

  Page<Product> findByBrandId(Long brandId, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id ASC")
  List<Product> findAllByIdWithLock(@Param("ids") List<Long> ids);

  List<Product> findByIdIn(List<Long> ids);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Product p SET p.likeCount = p.likeCount + 1 WHERE p.id = :productId")
  int incrementLikeCount(@Param("productId") Long productId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Product p SET p.likeCount = p.likeCount - 1 WHERE p.id = :productId AND p.likeCount > 0")
  int decrementLikeCount(@Param("productId") Long productId);
}
