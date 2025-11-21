package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT p FROM Product p JOIN FETCH p.brand b " +
            "WHERE (:brandId IS NULL OR b.id = :brandId)")
    Page<Product> findProducts(@Param("brandId") Long brandId, Pageable pageable);
}
