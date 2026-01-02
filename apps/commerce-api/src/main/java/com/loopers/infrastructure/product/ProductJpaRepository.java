package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    List<Product> findByBrandId(Long brandId);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndNotDeleted(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.totalLikes = p.totalLikes + 1 WHERE p.id = :id")
    void incrementLikeCount(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.totalLikes = p.totalLikes - 1 WHERE p.id = :id AND p.totalLikes > 0")
    void decrementLikeCount(Long id);

    @Query(value = """
            SELECT p.id as product_id, p.total_likes, COUNT(pl.id) as actual_likes
            FROM products p
            LEFT JOIN product_likes pl ON p.id = pl.product_id
            GROUP BY p.id
            HAVING p.total_likes != COUNT(pl.id)
            """, nativeQuery = true)
    List<Map<String, Object>> findLikeCountInconsistencies();

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.totalLikes = :totalLikes WHERE p.id = :id")
    void updateProductTotalLikes(Long id, Long totalLikes);
}
