package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.ranking.ProductRankMonthly;
import com.loopers.infrastructure.ranking.ProductRankMonthlyRepository;
import com.loopers.infrastructure.ranking.ProductRankWeekly;
import com.loopers.infrastructure.ranking.ProductRankWeeklyRepository;
import com.loopers.infrastructure.ranking.RankingEntry;
import com.loopers.infrastructure.ranking.RankingRedisService;
import com.loopers.interfaces.api.ranking.RankingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RankingRedisService rankingRedisService;
    private final ProductRepository productRepository;
    private final ProductRankWeeklyRepository productRankWeeklyRepository;
    private final ProductRankMonthlyRepository productRankMonthlyRepository;

    public RankingDto.RankingListResponse getRankings(String period, String dateStr, int page, int size) {
        return switch (period.toLowerCase()) {
            case "weekly" -> getWeeklyRankings(dateStr, page, size);
            case "monthly" -> getMonthlyRankings(dateStr, page, size);
            default -> getDailyRankings(dateStr, page, size);
        };
    }

    private RankingDto.RankingListResponse getDailyRankings(String dateStr, int page, int size) {
        LocalDate date = parseDate(dateStr);
        int offset = page * size;

        List<RankingEntry> entries = rankingRedisService.getTopProducts(date, offset, size);

        if (entries.isEmpty()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, 0);
        }

        long totalCount = rankingRedisService.getTotalCount(date);

        List<Long> productIds = entries.stream()
                .map(RankingEntry::productId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingDto.RankingResponse> rankings = new ArrayList<>();
        int rank = offset + 1;
        for (RankingEntry entry : entries) {
            Product product = productMap.get(entry.productId());
            if (product != null) {
                rankings.add(new RankingDto.RankingResponse(
                        rank++,
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        entry.score()
                ));
            }
        }

        return new RankingDto.RankingListResponse(rankings, page, size, totalCount);
    }

    private RankingDto.RankingListResponse getWeeklyRankings(String dateStr, int page, int size) {
        LocalDate date = parseDate(dateStr);
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        int offset = page * size;

        // 1. Redis 캐시 먼저 조회
        List<RankingEntry> cachedEntries = rankingRedisService.getWeeklyRankingCache(weekStart, offset, size);
        if (!cachedEntries.isEmpty()) {
            long totalCount = rankingRedisService.getWeeklyRankingCacheCount(weekStart);
            return buildRankingResponse(cachedEntries, offset, page, size, totalCount);
        }

        // 2. 캐시 없으면 DB 조회
        List<ProductRankWeekly> weeklyRanks = productRankWeeklyRepository
                .findByPeriodStartOrderByRankingAsc(weekStart);

        if (weeklyRanks.isEmpty()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, 0);
        }

        // 3. DB 결과를 Redis에 캐시
        List<RankingEntry> allEntries = weeklyRanks.stream()
                .map(r -> new RankingEntry(r.getProductId(), r.getScore().doubleValue()))
                .toList();
        rankingRedisService.cacheWeeklyRanking(weekStart, allEntries);

        // 4. 페이징 처리 후 반환
        int toIndex = Math.min(offset + size, weeklyRanks.size());
        if (offset >= weeklyRanks.size()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, weeklyRanks.size());
        }

        List<ProductRankWeekly> pagedRanks = weeklyRanks.subList(offset, toIndex);

        List<Long> productIds = pagedRanks.stream()
                .map(ProductRankWeekly::getProductId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingDto.RankingResponse> rankings = new ArrayList<>();
        for (ProductRankWeekly rank : pagedRanks) {
            Product product = productMap.get(rank.getProductId());
            if (product != null) {
                rankings.add(new RankingDto.RankingResponse(
                        rank.getRanking(),
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        rank.getScore().doubleValue()
                ));
            }
        }

        return new RankingDto.RankingListResponse(rankings, page, size, weeklyRanks.size());
    }

    private RankingDto.RankingListResponse getMonthlyRankings(String dateStr, int page, int size) {
        LocalDate date = parseDate(dateStr);
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);
        int offset = page * size;

        // 1. Redis 캐시 먼저 조회
        List<RankingEntry> cachedEntries = rankingRedisService.getMonthlyRankingCache(monthStart, offset, size);
        if (!cachedEntries.isEmpty()) {
            long totalCount = rankingRedisService.getMonthlyRankingCacheCount(monthStart);
            return buildRankingResponse(cachedEntries, offset, page, size, totalCount);
        }

        // 2. 캐시 없으면 DB 조회
        List<ProductRankMonthly> monthlyRanks = productRankMonthlyRepository
                .findByPeriodStartOrderByRankingAsc(monthStart);

        if (monthlyRanks.isEmpty()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, 0);
        }

        // 3. DB 결과를 Redis에 캐시
        List<RankingEntry> allEntries = monthlyRanks.stream()
                .map(r -> new RankingEntry(r.getProductId(), r.getScore().doubleValue()))
                .toList();
        rankingRedisService.cacheMonthlyRanking(monthStart, allEntries);

        // 4. 페이징 처리 후 반환
        int toIndex = Math.min(offset + size, monthlyRanks.size());
        if (offset >= monthlyRanks.size()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, monthlyRanks.size());
        }

        List<ProductRankMonthly> pagedRanks = monthlyRanks.subList(offset, toIndex);

        List<Long> productIds = pagedRanks.stream()
                .map(ProductRankMonthly::getProductId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingDto.RankingResponse> rankings = new ArrayList<>();
        for (ProductRankMonthly rank : pagedRanks) {
            Product product = productMap.get(rank.getProductId());
            if (product != null) {
                rankings.add(new RankingDto.RankingResponse(
                        rank.getRanking(),
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        rank.getScore().doubleValue()
                ));
            }
        }

        return new RankingDto.RankingListResponse(rankings, page, size, monthlyRanks.size());
    }

    private RankingDto.RankingListResponse buildRankingResponse(
            List<RankingEntry> entries, int offset, int page, int size, long totalCount) {

        List<Long> productIds = entries.stream()
                .map(RankingEntry::productId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingDto.RankingResponse> rankings = new ArrayList<>();
        int rank = offset + 1;
        for (RankingEntry entry : entries) {
            Product product = productMap.get(entry.productId());
            if (product != null) {
                rankings.add(new RankingDto.RankingResponse(
                        rank++,
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        entry.score()
                ));
            }
        }

        return new RankingDto.RankingListResponse(rankings, page, size, totalCount);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}
