package com.loopers.application.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import com.loopers.infrastructure.ranking.MvProductRankWeeklyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MvProductRankWeeklyService {

    private final MvProductRankWeeklyRepository mvWeeklyRepository;

    /**
     * 주간 MV 데이터 존재 여부 확인
     */
    public boolean existsByYearMonthWeek(String yearMonthWeek) {
        return mvWeeklyRepository.existsByYearMonthWeek(yearMonthWeek);
    }

    /**
     * 주간 MV에서 랭킹 순서대로 상품 ID 목록 조회
     */
    public List<Long> getWeeklyRankingProductIds(String yearMonthWeek, Pageable pageable) {
        if (!existsByYearMonthWeek(yearMonthWeek)) {
            log.debug("Weekly MV data not found for period: {}", yearMonthWeek);
            return List.of();
        }

        Page<MvProductRankWeekly> mvResult = mvWeeklyRepository
            .findByYearMonthWeekOrderByRanking(yearMonthWeek, pageable);
        
        return mvResult.getContent().stream()
            .map(MvProductRankWeekly::getProductId)
            .toList();
    }

    /**
     * 주간 MV 데이터의 총 개수 조회
     */
    public Long getWeeklyRankingCount(String yearMonthWeek) {
        if (!existsByYearMonthWeek(yearMonthWeek)) {
            return 0L;
        }
        
        return mvWeeklyRepository.countByYearMonthWeek(yearMonthWeek);
    }
}