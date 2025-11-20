package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Product 엔티티를 위한 Spring Data JPA 리포지토리.
 * <p>
 * JpaRepository를 확장하여 기본 CRUD 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    /**
     * 브랜드 ID로 상품을 조회합니다.
     *
     * @param brandId 브랜드 ID
     * @param pageable 페이징 정보
     * @return 상품 페이지
     */
    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    /**
     * 전체 상품을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 상품 페이지
     */
    Page<Product> findAll(Pageable pageable);

    /**
 * Counts products that belong to the specified brand.
 *
 * @param brandId ID of the brand to count products for
 * @return the number of products with the given brandId
 */
    long countByBrandId(Long brandId);

    /**
     * Retrieves the Product with the given id and acquires a pessimistic write lock on its row.
     *
     * The lock prevents concurrent modifications of the same product within the transaction.
     *
     * @param productId the id of the product to retrieve and lock
     * @return an Optional containing the Product if found, otherwise empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

    /**
     * Retrieves the IDs of all Product entities.
     *
     * Intended for use by asynchronous aggregation tasks.
     *
     * @return a list containing the IDs of all products
     */
    @Query("SELECT p.id FROM Product p")
    List<Long> findAllProductIds();
}
