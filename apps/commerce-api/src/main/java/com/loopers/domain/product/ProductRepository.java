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
     * 상품 ID로 상품을 조회합니다. (비관적 락)
     * <p>
     * 동시성 제어가 필요한 경우 사용합니다. (예: 재고 차감)
     * </p>
     * <p>
     * <b>Lock 전략:</b>
     * <ul>
     *   <li><b>PESSIMISTIC_WRITE:</b> SELECT ... FOR UPDATE 사용</li>
     *   <li><b>Lock 범위:</b> PK(id) 기반 조회로 해당 행만 락 (최소화)</li>
     *   <li><b>사용 목적:</b> 재고 차감 시 Lost Update 방지</li>
     * </ul>
     * </p>
     *
     * @param productId 조회할 상품 ID
     * @return 조회된 상품을 담은 Optional
     */
    Optional<Product> findByIdForUpdate(Long productId);

    /**
     * 상품 ID 목록으로 상품 목록을 조회합니다.
     * <p>
     * 배치 조회를 통해 N+1 쿼리 문제를 해결합니다.
     * </p>
     *
     * @param productIds 조회할 상품 ID 목록
     * @return 조회된 상품 목록
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
     * 상품 목록의 총 개수를 조회합니다.
     *
     * @param brandId 브랜드 ID (null이면 전체 조회)
     * @return 상품 총 개수
     */
    long countAll(Long brandId);

    /**
     * 상품의 좋아요 수를 업데이트합니다.
     * <p>
     * 비동기 집계 스케줄러에서 사용됩니다.
     * </p>
     *
     * @param productId 상품 ID
     * @param likeCount 업데이트할 좋아요 수
     */
    void updateLikeCount(Long productId, Long likeCount);

    /**
     * 모든 상품 ID를 조회합니다.
     * <p>
     * Spring Batch Reader에서 사용됩니다.
     * </p>
     *
     * @return 모든 상품 ID 목록
     */
    List<Long> findAllProductIds();
}

