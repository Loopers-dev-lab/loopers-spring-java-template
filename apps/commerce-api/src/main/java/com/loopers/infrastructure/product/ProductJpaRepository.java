package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductModel, Long> {
    Page<ProductModel> findByStatusNot(ProductStatus status, Pageable pageable);
    Page<ProductModel> findByBrandId(Long brandId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductModel p where p.id = :id")
    Optional<ProductModel> findByIdForUpdate(@Param("id") Long id);
    Optional<ProductModel> findById(Long id);
    Optional<ProductModel> findByName(String name);
}
