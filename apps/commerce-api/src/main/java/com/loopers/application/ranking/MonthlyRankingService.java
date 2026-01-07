package com.loopers.application.ranking;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
     * @param pageable  페이징 정보
     * @return 월간 랭킹 페이지
     */
    public Page<MonthlyRankEntity> getMonthlyRanking(String yearMonth, Pageable pageable) {
        log.debug("월간 랭킹 조회: yearMonth={}, page={}, size={}",
                yearMonth, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 전체 랭킹 조회 (순위 순으로 정렬됨)
        Page<MonthlyRankEntity> pagedRankings = monthlyRankRepository.findByYearMonth(yearMonth, pageable);


        log.debug("월간 랭킹 조회 완료: yearMonth={}, 전체={}, 페이지={}",
                yearMonth, pagedRankings.getTotalPages(), pagedRankings.getNumber());

        return pagedRankings;
    }
}
