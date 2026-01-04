package com.loopers.domain.rank;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ProductRankScore 도메인 Repository 인터페이스.
 * <p>
 * Step 1과 Step 2 간 데이터 전달을 위한 임시 테이블을 관리합니다.
 * </p>
 */
public interface ProductRankScoreRepository {

    /**
     * ProductRankScore를 저장합니다.
     * <p>
     * 같은 product_id가 이미 존재하면 업데이트, 없으면 생성합니다 (UPSERT 방식).
     * </p>
     *
     * @param score 저장할 ProductRankScore
     */
    void save(ProductRankScore score);

    /**
     * 여러 ProductRankScore를 저장합니다.
     * <p>
     * 같은 product_id가 이미 존재하면 업데이트, 없으면 생성합니다 (UPSERT 방식).
     * </p>
     *
     * @param scores 저장할 ProductRankScore 리스트
     */
    void saveAll(List<ProductRankScore> scores);

    /**
     * product_id로 ProductRankScore를 조회합니다.
     *
     * @param productId 상품 ID
     * @return ProductRankScore (없으면 Optional.empty())
     */
    Optional<ProductRankScore> findByProductId(Long productId);

    /**
     * 여러 product_id로 ProductRankScore를 일괄 조회합니다.
     * <p>
     * N+1 쿼리 문제를 방지하기 위해 사용합니다.
     * </p>
     *
     * @param productIds 상품 ID 집합
     * @return ProductRankScore 리스트
     */
    List<ProductRankScore> findAllByProductIdIn(Set<Long> productIds);

    /**
     * 모든 ProductRankScore를 점수 내림차순으로 조회합니다.
     * <p>
     * Step 2에서 TOP 100 선정을 위해 사용합니다.
     * </p>
     *
     * @param limit 조회할 최대 개수 (기본: 전체)
     * @return ProductRankScore 리스트 (점수 내림차순)
     */
    List<ProductRankScore> findAllOrderByScoreDesc(int limit);

    /**
     * 모든 ProductRankScore를 조회합니다.
     *
     * @return ProductRankScore 리스트
     */
    List<ProductRankScore> findAll();

    /**
     * 모든 ProductRankScore를 삭제합니다.
     * <p>
     * Step 2 완료 후 임시 테이블을 정리하기 위해 사용합니다.
     * </p>
     */
    void deleteAll();
}

