package com.loopers.application.ranking;

import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 월간 랭킹 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MonthlyRankingService {

    private final MonthlyRankRepository monthlyRankRepository;

    /**
     * 특정 월의 랭킹을 페이지네이션하여 조회합니다.
     * 
     * @param yearMonth 조회할 월 (예: "2024-12")
     * @param pageable 페이징 정보
     * @return 월간 랭킹 페이지
     */
    public Page<MonthlyRankEntity> getMonthlyRanking(String yearMonth, Pageable pageable) {
        log.debug("월간 랭킹 조회: yearMonth={}, page={}, size={}", 
                yearMonth, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 전체 랭킹 조회 (순위 순으로 정렬됨)
        List<MonthlyRankEntity> allRankings = monthlyRankRepository.findByYearMonth(yearMonth);
        
        if (allRankings.isEmpty()) {
            log.debug("월간 랭킹 데이터 없음: yearMonth={}", yearMonth);
            return Page.empty(pageable);
        }

        // 2. 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allRankings.size());
        
        if (start >= allRankings.size()) {
            return Page.empty(pageable);
        }

        List<MonthlyRankEntity> pagedRankings = allRankings.subList(start, end);
        
        log.debug("월간 랭킹 조회 완료: yearMonth={}, 전체={}, 페이지={}", 
                yearMonth, allRankings.size(), pagedRankings.size());

        return new PageImpl<>(pagedRankings, pageable, allRankings.size());
    }

    /**
     * 특정 월의 전체 랭킹 개수를 조회합니다.
     * 
     * @param yearMonth 조회할 월
     * @return 랭킹 개수
     */
    public long getMonthlyRankingCount(String yearMonth) {
        List<MonthlyRankEntity> rankings = monthlyRankRepository.findByYearMonth(yearMonth);
        return rankings.size();
    }
}