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
     * 상품 ID로 상품을 조회합니다.
     *
     * @param productId 조회할 상품 ID
     * @return 조회된 상품을 담은 Optional
     */
    Optional<Product> findById(Long productId);

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
     * 상품 목록의 총 개수를 조회합니다.
     *
     * @param brandId 브랜드 ID (null이면 전체 조회)
     * @return 상품 총 개수
     */
    long countAll(Long brandId);
}

