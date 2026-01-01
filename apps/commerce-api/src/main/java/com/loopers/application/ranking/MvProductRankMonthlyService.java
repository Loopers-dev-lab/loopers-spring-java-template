package com.loopers.application.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.infrastructure.ranking.MvProductRankMonthlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MvProductRankMonthlyService {

    private final MvProductRankMonthlyRepository mvMonthlyRepository;

    /**
     * 월간 MV 데이터 존재 여부 확인
     */
    public boolean existsByYearMonth(String yearMonth) {
        return mvMonthlyRepository.existsByYearMonth(yearMonth);
    }

    /**
     * 월간 MV에서 랭킹 순서대로 상품 ID 목록 조회
     */
    public List<Long> getMonthlyRankingProductIds(String yearMonth, Pageable pageable) {
        if (!existsByYearMonth(yearMonth)) {
            log.debug("Monthly MV data not found for period: {}", yearMonth);
            return List.of();
        }

        Page<MvProductRankMonthly> mvResult = mvMonthlyRepository
            .findByYearMonthOrderByRanking(yearMonth, pageable);
        
        return mvResult.getContent().stream()
            .map(MvProductRankMonthly::getProductId)
            .toList();
    }

    /**
     * 월간 MV 데이터의 총 개수 조회
     */
    public Long getMonthlyRankingCount(String yearMonth) {
        if (!existsByYearMonth(yearMonth)) {
            return 0L;
        }
        
        return mvMonthlyRepository.countByYearMonth(yearMonth);
    }
}