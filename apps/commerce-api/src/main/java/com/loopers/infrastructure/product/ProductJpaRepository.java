package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<Product, Long>, ProductRepository {

    @Override
    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
    Page<Product> findAll(Pageable pageable);

    @Override
    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId AND p.deletedAt IS NULL")
    Page<Product> findByBrandId(@Param("brandId") Long brandId, Pageable pageable);

    @Override
    @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.deletedAt IS NULL")
    List<Product> findByIdIn(@Param("ids") List<Long> ids);
}
