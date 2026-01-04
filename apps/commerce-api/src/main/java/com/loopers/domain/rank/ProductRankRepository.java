package com.loopers.domain.rank;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProductRank 도메인 Repository 인터페이스.
 * <p>
 * Materialized View에 저장된 상품 랭킹 데이터를 조회합니다.
 * </p>
 */
public interface ProductRankRepository {

    /**
     * 특정 기간의 랭킹 데이터를 조회합니다.
     *
     * @param periodType 기간 타입
     * @param periodStartDate 기간 시작일
     * @param limit 조회할 랭킹 수 (기본: 100)
     * @return 랭킹 리스트 (rank 오름차순)
     */
    List<ProductRank> findByPeriod(ProductRank.PeriodType periodType, LocalDate periodStartDate, int limit);

    /**
     * 특정 기간의 특정 상품 랭킹을 조회합니다.
     *
     * @param periodType 기간 타입
     * @param periodStartDate 기간 시작일
     * @param productId 상품 ID
     * @return 랭킹 정보 (없으면 Optional.empty())
     */
    Optional<ProductRank> findByPeriodAndProductId(
        ProductRank.PeriodType periodType,
        LocalDate periodStartDate,
        Long productId
    );
}

