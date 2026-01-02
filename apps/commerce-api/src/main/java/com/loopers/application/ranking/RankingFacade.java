package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.rank.MonthlyRankJpaRepository;
import com.loopers.infrastructure.rank.WeeklyRankJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToIntFunction;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final ProductRepository productRepository;
    private final WeeklyRankJpaRepository weeklyRankJpaRepository;
    private final MonthlyRankJpaRepository monthlyRankJpaRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional(readOnly = true)
    public List<RankingProductInfo> getDailyRanking(String yyyymmdd, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        long start = (long) (p - 1) * s;
        long end = start + s - 1;

        Set<ZSetOperations.TypedTuple<String>> tuples =
                rankingService.getDailyMembers(yyyymmdd, start, end);

        List<RankingProductInfo> result = new ArrayList<>();
        if (tuples == null || tuples.isEmpty()) {
            return result;
        }

        List<Long> productIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(v -> v != null && !v.isBlank())
                .map(Long::valueOf)
                .toList();

        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product pdt : products) {
            productMap.put(pdt.getId(), pdt);
        }

        int baseRank = (int) start + 1;
        AtomicInteger rankCounter = new AtomicInteger(baseRank);
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            String member = t.getValue();
            if (member == null || member.isBlank()) continue;
            Long productId = Long.valueOf(member);
            Product product = productMap.get(productId);
            if (product == null) continue;
            int rank = rankCounter.getAndIncrement();
            Double score = t.getScore();
            result.add(toInfo(productId, product, rank, score));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RankingProductInfo> getWeeklyRanking(String weekStartYyyymmdd, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        LocalDate periodStart = LocalDate.parse(weekStartYyyymmdd, DATE_FORMATTER);

        var rows = weeklyRankJpaRepository.findByPeriodStartOrderByRankPositionAsc(
                periodStart, PageRequest.of(p - 1, s)
        );
        return buildRanking(
                rows,
                r -> r.getProductId(),
                r -> r.getRankPosition() != null ? r.getRankPosition() : 0,
                r -> r.getTotalScore()
        );
    }

    @Transactional(readOnly = true)
    public List<RankingProductInfo> getMonthlyRanking(String monthStartYyyymmdd, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        LocalDate periodStart = LocalDate.parse(monthStartYyyymmdd, DATE_FORMATTER);

        var rows = monthlyRankJpaRepository.findByPeriodStartOrderByRankPositionAsc(
                periodStart, PageRequest.of(p - 1, s)
        );
        return buildRanking(
                rows,
                r -> r.getProductId(),
                r -> r.getRankPosition() != null ? r.getRankPosition() : 0,
                r -> r.getTotalScore()
        );
    }

    private RankingProductInfo toInfo(Long productId, Product product, int rank, Double score) {
        return new RankingProductInfo(
                rank,
                score,
                productId,
                product.getName(),
                product.getPrice() != null ? product.getPrice().getAmount() : BigDecimal.ZERO,
                product.getStockQuantity(),
                product.getLikeCount() != null ? product.getLikeCount() : 0L
        );
    }

    private <T> List<RankingProductInfo> buildRanking(
            List<T> rows,
            Function<T, Long> productIdExtractor,
            ToIntFunction<T> rankExtractor,
            Function<T, Double> scoreExtractor
    ) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = rows.stream()
                .map(productIdExtractor)
                .toList();

        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product prd : products) {
            productMap.put(prd.getId(), prd);
        }

        List<RankingProductInfo> result = new ArrayList<>();
        for (T row : rows) {
            Long productId = productIdExtractor.apply(row);
            Product product = productMap.get(productId);
            if (product == null) {
                continue;
            }
            int rank = rankExtractor.applyAsInt(row);
            Double score = scoreExtractor.apply(row);
            result.add(toInfo(productId, product, rank, score));
        }
        return result;
    }

 
}


