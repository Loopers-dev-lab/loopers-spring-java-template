package com.loopers.domain.ranking;

import com.loopers.domain.metrics.ProductMetricsMonthly;
import com.loopers.domain.metrics.ProductMetricsMonthlyRepository;
import com.loopers.domain.metrics.ProductMetricsWeekly;
import com.loopers.domain.metrics.ProductMetricsWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PeriodRankingService {

    private final ProductMetricsWeeklyRepository weeklyRepository;
    private final ProductMetricsMonthlyRepository monthlyRepository;

    /**
     * 주간 TOP N 랭킹 조회
     */
    public List<Ranking> getTopWeeklyRanking(RankingType type, LocalDate date, int limit) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        int year = targetDate.getYear();
        int week = targetDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        List<ProductMetricsWeekly> weeklyMetrics = switch (type) {
            case LIKE -> weeklyRepository.findByYearAndWeekOrderByLikeCountDesc(year, week, limit);
            case VIEW -> weeklyRepository.findByYearAndWeekOrderByViewCountDesc(year, week, limit);
            case ORDER -> weeklyRepository.findByYearAndWeekOrderByOrderCountDesc(year, week, limit);
            case ALL -> weeklyRepository.findByYearAndWeekOrderByCompositeScoreDesc(year, week, limit);
        };

        return convertWeeklyToRanking(weeklyMetrics, type);
    }

    /**
     * 월간 TOP N 랭킹 조회
     */
    public List<Ranking> getTopMonthlyRanking(RankingType type, LocalDate date, int limit) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        int year = targetDate.getYear();
        int month = targetDate.getMonthValue();

        List<ProductMetricsMonthly> monthlyMetrics = switch (type) {
            case LIKE -> monthlyRepository.findByYearAndMonthOrderByLikeCountDesc(year, month, limit);
            case VIEW -> monthlyRepository.findByYearAndMonthOrderByViewCountDesc(year, month, limit);
            case ORDER -> monthlyRepository.findByYearAndMonthOrderByOrderCountDesc(year, month, limit);
            case ALL -> monthlyRepository.findByYearAndMonthOrderByCompositeScoreDesc(year, month, limit);
        };

        return convertMonthlyToRanking(monthlyMetrics, type);
    }

    /**
     * 주간 페이징 랭킹 조회
     */
    public List<Ranking> getWeeklyRankingWithPaging(RankingType type, LocalDate date, int page, int size) {
        return getTopWeeklyRanking(type, date, (page + 1) * size)
                .stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    /**
     * 월간 페이징 랭킹 조회
     */
    public List<Ranking> getMonthlyRankingWithPaging(RankingType type, LocalDate date, int page, int size) {
        return getTopMonthlyRanking(type, date, (page + 1) * size)
                .stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }


    private List<Ranking> convertWeeklyToRanking(
            List<ProductMetricsWeekly> metrics,
            RankingType type
    ) {
        int rank = 1;
        List<Ranking> rankings = new ArrayList<>();

        for (ProductMetricsWeekly metric : metrics) {
            double score = calculateScore(type,
                    metric.getTotalLikeCount(),
                    metric.getTotalViewCount(),
                    metric.getTotalOrderCount());

            rankings.add(Ranking.of(
                    rank++,
                    metric.getProductId(),
                    score,
                    metric.getTotalLikeCount(),
                    metric.getTotalViewCount(),
                    metric.getTotalOrderCount()
            ));
        }

        return rankings;
    }

    private List<Ranking> convertMonthlyToRanking(List<ProductMetricsMonthly> metrics, RankingType type) {
        int rank = 1;
        List<Ranking> rankings = new ArrayList<>();

        for (ProductMetricsMonthly metric : metrics) {
            double score = calculateScore(
                    type,
                    metric.getTotalLikeCount(),
                    metric.getTotalViewCount(),
                    metric.getTotalOrderCount()
            );

            rankings.add(Ranking.of(
                    rank++,
                    metric.getProductId(),
                    score,
                    metric.getTotalLikeCount(),
                    metric.getTotalViewCount(),
                    metric.getTotalOrderCount()
            ));
        }

        return rankings;
    }

    private double calculateScore(RankingType type, long likeCount, long viewCount, long orderCount) {
        return switch (type) {
            case LIKE -> likeCount;
            case VIEW -> viewCount;
            case ORDER -> orderCount;
            case ALL -> (likeCount * 0.2) + (viewCount * 0.1) + (orderCount * 0.6);
        };
    }
}
