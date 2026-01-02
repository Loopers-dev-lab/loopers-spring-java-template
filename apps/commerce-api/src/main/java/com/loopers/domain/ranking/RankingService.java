package com.loopers.domain.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RankingRepository rankingRepository;

    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<RankingEntry> getTopNWithScores(LocalDate date, int n) {
        String key = KEY_PREFIX + date.format(DATE_FORMATTER);

        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, 0, n - 1);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }

            return convertToRankingEntries(tuples);
        } catch (Exception e) {
            log.error("Top-N 랭킹 조회 실패: key={}, n={}", key, n, e);
            return Collections.emptyList();
        }
    }

    public List<RankingEntry> getRankingPage(LocalDate date, int page, int size) {
        String key = KEY_PREFIX + date.format(DATE_FORMATTER);
        long start = (long) page * size;
        long end = start + size - 1;

        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, start, end);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }

            return convertToRankingEntries(tuples);
        } catch (Exception e) {
            log.error("랭킹 페이지 조회 실패: key={}, page={}, size={}", key, page, size, e);
            return Collections.emptyList();
        }
    }

    public Long getRank(Long productId, LocalDate date) {
        String key = KEY_PREFIX + date.format(DATE_FORMATTER);
        String member = productId.toString();

        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(key, member);
            return rank != null ? rank + 1 : null;
        } catch (Exception e) {
            log.error("순위 조회 실패: key={}, productId={}", key, productId, e);
            return null;
        }
    }

    public Long getRankingSize(LocalDate date) {
        String key = KEY_PREFIX + date.format(DATE_FORMATTER);

        try {
            Long size = redisTemplate.opsForZSet().zCard(key);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("랭킹 사이즈 조회 실패: key={}", key, e);
            return 0L;
        }
    }

    public List<RankingEntry> getWeeklyRankingPage(LocalDate date, int page, int size) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);
        int offset = page * size;

        try {
            List<WeeklyRanking> ranks = rankingRepository.findWeeklyByDateOrderByRank(weekStart, weekEnd, size, offset);
            return ranks.stream()
                    .map(r -> new RankingEntry(r.getProductId(), r.getScore()))
                    .toList();
        } catch (Exception e) {
            log.error("주간 랭킹 페이지 조회 실패: weekStart={}, weekEnd={}, page={}, size={}",
                    weekStart, weekEnd, page, size, e);
            return Collections.emptyList();
        }
    }

    public List<RankingEntry> getWeeklyTopN(LocalDate date, int n) {
        return getWeeklyRankingPage(date, 0, n);
    }

    public Long getWeeklyRank(Long productId, LocalDate date) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);

        try {
            return rankingRepository.findWeeklyByProductIdAndDate(productId, weekStart, weekEnd)
                    .map(r -> (long) r.getRank())
                    .orElse(null);
        } catch (Exception e) {
            log.error("주간 순위 조회 실패: productId={}, weekStart={}, weekEnd={}", productId, weekStart, weekEnd, e);
            return null;
        }
    }

    public Long getWeeklyRankingSize(LocalDate date) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);
        return rankingRepository.countWeeklyByDate(weekStart, weekEnd);
    }

    public List<RankingEntry> getMonthlyRankingPage(LocalDate date, int page, int size) {
        YearMonth yearMonth = YearMonth.from(date);
        int offset = page * size;

        try {
            List<MonthlyRanking> ranks = rankingRepository.findMonthlyByPeriodOrderByRank(yearMonth, size, offset);
            return ranks.stream()
                    .map(r -> new RankingEntry(r.getProductId(), r.getScore()))
                    .toList();
        } catch (Exception e) {
            log.error("월간 랭킹 페이지 조회 실패: yearMonth={}, page={}, size={}", yearMonth, page, size, e);
            return Collections.emptyList();
        }
    }

    public List<RankingEntry> getMonthlyTopN(LocalDate date, int n) {
        return getMonthlyRankingPage(date, 0, n);
    }

    public Long getMonthlyRank(Long productId, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);

        try {
            return rankingRepository.findMonthlyByProductIdAndPeriod(productId, yearMonth)
                    .map(r -> (long) r.getRank())
                    .orElse(null);
        } catch (Exception e) {
            log.error("월간 순위 조회 실패: productId={}, yearMonth={}", productId, yearMonth, e);
            return null;
        }
    }

    public Long getMonthlyRankingSize(LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        return rankingRepository.countMonthlyByPeriod(yearMonth);
    }

    private List<RankingEntry> convertToRankingEntries(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<RankingEntry> entries = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() != null && tuple.getScore() != null) {
                entries.add(new RankingEntry(
                        Long.parseLong(tuple.getValue()),
                        tuple.getScore()
                ));
            }
        }
        return entries;
    }
}
