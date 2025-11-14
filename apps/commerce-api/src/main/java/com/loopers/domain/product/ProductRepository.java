package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Repository Interface
 * Domain Layer에 위치하며, 구현체는 Infrastructure Layer에 존재한다
 */
public interface ProductRepository {

    /**
     * 상품 저장
     *
     * @param product 저장할 상품
     * @return 저장된 상품 (ID 포함)
     */
    Product save(Product product);

    /**
     * 상품 ID로 조회
     *
     * @param productId 상품 ID
     * @return 상품 (존재하지 않으면 empty)
     */
    Optional<Product> findById(Long productId);

    /**
     * 브랜드별 상품 조회
     *
     * @param brandId 브랜드 ID
     * @return 상품 목록
     */
    List<Product> findByBrandId(Long brandId);

    /**
     * 상품 상태로 조회
     *
     * @param status 상품 상태
     * @return 상품 목록
     */
    List<Product> findByStatus(ProductStatus status);

    /**
     * 모든 상품 조회
     *
     * @return 모든 상품 목록
     */
    List<Product> findAll();

    /**
     * 정렬된 상품 목록 조회
     *
     * @param sortType 정렬 타입
     * @return 정렬된 상품 목록
     */
    List<Product> findAllSorted(ProductSortType sortType);

    /**
     * 상품 존재 여부 확인
     *
     * @param productId 상품 ID
     * @return 존재 여부
     */
    boolean existsById(Long productId);

    /**
     * 상품 삭제
     *
     * @param productId 상품 ID
     */
    void deleteById(Long productId);
}
