package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

/**
 * Product 엔티티에 대한 저장소 인터페이스.
 * <p>
 * 상품 정보의 영속성 계층과의 상호작용을 정의합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface ProductRepository {
    /**
     * 상품을 저장합니다.
     *
     * @param product 저장할 상품
     * @return 저장된 상품
     */
    Product save(Product product);
    
    /**
 * Retrieve a product by its identifier.
 *
 * @param productId the ID of the product to retrieve
 * @return an Optional containing the found Product, or empty if no product exists with the given ID
 */
    Optional<Product> findById(Long productId);

    /**
 * Retrieve a product by its ID while acquiring a pessimistic write lock for update.
 *
 * This method obtains a pessimistic write lock on the targeted product row to prevent concurrent modifications (e.g., during inventory deduction); the lock scope is limited to the row identified by the primary key.
 *
 * @param productId the ID of the product to retrieve
 * @return an Optional containing the product if found, empty otherwise
 */
    Optional<Product> findByIdForUpdate(Long productId);

    /**
 * Retrieves products matching the given list of product IDs.
 *
 * May return fewer items than provided if some IDs do not correspond to existing products.
 *
 * @param productIds the product IDs to retrieve
 * @return the list of found products
 */
    List<Product> findAllById(List<Long> productIds);

    /**
     * 상품 목록을 조회합니다.
     *
     * @param brandId 브랜드 ID (null이면 전체 조회)
     * @param sort 정렬 기준 (latest, price_asc, likes_desc)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 상품 수
     * @return 상품 목록
     */
    List<Product> findAll(Long brandId, String sort, int page, int size);

    /**
 * Get the total number of products optionally filtered by brand.
 *
 * @param brandId brand ID to filter by; `null` to include all brands
 * @return the total number of products matching the filter
 */
    long countAll(Long brandId);

    /**
 * Update the stored like count for a product.
 *
 * Intended for use by an asynchronous aggregation scheduler to persist computed like totals.
 *
 * @param productId the ID of the product to update
 * @param likeCount the new like count to set for the product
 */
    void updateLikeCount(Long productId, Long likeCount);

    /**
 * Retrieve all product IDs in the system.
 *
 * Intended for use by batch readers (for example, a Spring Batch ItemReader).
 *
 * @return a list containing every product ID
 */
    List<Long> findAllProductIds();
}
