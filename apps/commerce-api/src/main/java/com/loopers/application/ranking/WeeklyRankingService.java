package com.loopers.application.ranking;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.ranking.WeeklyRankEntity;
import com.loopers.domain.ranking.WeeklyRankRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주간 랭킹 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WeeklyRankingService {

    private final WeeklyRankRepository weeklyRankRepository;

    /**
     * 특정 주차의 랭킹을 페이지네이션하여 조회합니다.
     *
     * @param yearWeek 조회할 주차 (예: "2024-W52")
     * @param pageable 페이징 정보
     * @return 주간 랭킹 페이지
     */
    public Page<WeeklyRankEntity> getWeeklyRanking(String yearWeek, Pageable pageable) {
        log.debug("주간 랭킹 조회: yearWeek={}, page={}, size={}",
                yearWeek, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 전체 랭킹 조회 (순위 순으로 정렬됨)
        Page<WeeklyRankEntity> pagedRankings = weeklyRankRepository.findByYearWeek(yearWeek , pageable);

        if (pagedRankings.isEmpty()) {
            log.debug("주간 랭킹 데이터 없음: yearWeek={}", yearWeek);
            return Page.empty(pageable);
        }

        return pagedRankings;
    }

    /**
     * 특정 주차의 전체 랭킹 개수를 조회합니다.
     *
     * @param yearWeek 조회할 주차
     * @return 랭킹 개수
     */
    public long getWeeklyRankingCount(String yearWeek) {
        List<WeeklyRankEntity> rankings = weeklyRankRepository.findByYearWeek(yearWeek);
        return rankings.size();
    }
}
