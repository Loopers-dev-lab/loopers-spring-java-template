package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.createdAt DESC")
    List<Product> findProductsByLatest(@Param("brandId") Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.price.price ASC")
    List<Product> findProductsByPriceAsc(@Param("brandId") Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.likeCount.count DESC")
    List<Product> findProductsByLikesDesc(@Param("brandId") Long brandId, Pageable pageable);
}

