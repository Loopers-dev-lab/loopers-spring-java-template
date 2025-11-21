package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    Product save(Product product);

    boolean existsByProductCode(String productCode);

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
    List<Product> findAllByDeletedAtIsNull(Sort sort);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.brand " +
            "LEFT JOIN FETCH p.productLikes " +
            "WHERE p.id = :productId AND p.deletedAt IS NULL")
    Optional<Product> findByIdWithBrand(@Param("productId") Long productId);
}
