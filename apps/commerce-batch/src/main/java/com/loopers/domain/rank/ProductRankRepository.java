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
     * 특정 기간의 랭킹 데이터를 저장합니다.
     * <p>
     * 기존 데이터가 있으면 삭제 후 새로 저장합니다 (UPSERT 방식).
     * </p>
     *
     * @param periodType 기간 타입
     * @param periodStartDate 기간 시작일
     * @param ranks 저장할 랭킹 리스트 (TOP 100)
     */
    void saveRanks(ProductRank.PeriodType periodType, LocalDate periodStartDate, List<ProductRank> ranks);

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

    /**
     * 특정 기간의 기존 랭킹 데이터를 삭제합니다.
     *
     * @param periodType 기간 타입
     * @param periodStartDate 기간 시작일
     */
    void deleteByPeriod(ProductRank.PeriodType periodType, LocalDate periodStartDate);
}

