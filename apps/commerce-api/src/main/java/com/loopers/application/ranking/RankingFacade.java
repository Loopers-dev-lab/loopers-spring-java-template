package com.loopers.application.ranking;

import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductService;
import com.loopers.application.ranking.RankingService.RankingItem;
import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.WeeklyProductRank;
import com.loopers.infrastructure.rank.MonthlyRankJpaRepository;
import com.loopers.infrastructure.rank.WeeklyRankJpaRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Facade for combining ranking data with product information.
 *
 * <p>This service provides unified access to daily, weekly, and monthly rankings
 * by merging ranking data with detailed product information.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final ProductService productService;
    private final WeeklyRankJpaRepository weeklyRankJpaRepository;
    private final MonthlyRankJpaRepository monthlyRankJpaRepository;

    /**
     * Retrieves daily rankings with product information.
     *
     * @param date the target date in yyyyMMdd format
     * @param page the page number (1-based)
     * @param size the page size
     * @return list of rankings with product details
     */
    public List<RankingProductInfo> getDailyRanking(String date, int page, int size) {
        List<RankingItem> rankingItems = rankingService.getDailyRanking(date, page, size);

        if (rankingItems.isEmpty()) {
            return List.of();
        }

        return combineWithProductInfo(rankingItems);
    }

    /**
     * Retrieves today's rankings.
     *
     * @param page the page number (1-based)
     * @param size the page size
     * @return list of rankings with product details
     */
    public List<RankingProductInfo> getTodayRanking(int page, int size) {
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return getDailyRanking(today, page, size);
    }

    /**
     * Retrieves weekly rankings with product information.
     *
     * @param yearWeek the target week in ISO format (e.g., "2025-W01")
     * @param page the page number (1-based)
     * @param size the page size
     * @return list of rankings with product details
     */
    public List<RankingProductInfo> getWeeklyRanking(String yearWeek, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        List<WeeklyProductRank> weeklyRanks = weeklyRankJpaRepository.findByYearWeekOrderByRankPositionAsc(
            yearWeek, pageRequest
        );

        if (weeklyRanks.isEmpty()) {
            return List.of();
        }

        return combineWithProductInfo(
            weeklyRanks.stream()
                .map(rank -> new RankingItem(
                    rank.getRankPosition(),
                    rank.getProductId(),
                    rank.getTotalScore()
                ))
                .toList()
        );
    }

    /**
     * Retrieves monthly rankings with product information.
     *
     * @param yearMonth the target month (e.g., "2025-01")
     * @param page the page number (1-based)
     * @param size the page size
     * @return list of rankings with product details
     */
    public List<RankingProductInfo> getMonthlyRanking(String yearMonth, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        List<MonthlyProductRank> monthlyRanks = monthlyRankJpaRepository.findByYearMonthOrderByRankPositionAsc(
            yearMonth, pageRequest
        );

        if (monthlyRanks.isEmpty()) {
            return List.of();
        }

        return combineWithProductInfo(
            monthlyRanks.stream()
                .map(rank -> new RankingItem(
                    rank.getRankPosition(),
                    rank.getProductId(),
                    rank.getTotalScore()
                ))
                .toList()
        );
    }

    /**
     * Combines ranking items with product information.
     *
     * @param rankingItems the list of ranking items
     * @return list of rankings with product details
     */
    private List<RankingProductInfo> combineWithProductInfo(List<RankingItem> rankingItems) {
        List<Long> productIds = rankingItems.stream()
            .map(RankingItem::getProductId)
            .toList();

        Map<Long, ProductInfo> productMap = productService.findByIds(productIds).stream()
            .collect(Collectors.toMap(ProductInfo::id, p -> p));

        List<RankingProductInfo> results = new ArrayList<>();
        for (RankingItem item : rankingItems) {
            ProductInfo product = productMap.get(item.getProductId());

            if (product == null) {
                log.warn("Product not found for ranking: productId={}", item.getProductId());
                continue;
            }

            results.add(RankingProductInfo.builder()
                .rank(item.getRank())
                .score(item.getScore())
                .productId(product.id())
                .productName(product.name())
                .brandName(product.brand().name())
                .price(product.price())
                .stock(product.stock())
                .likeCount(product.likeCount())
                .build());
        }

        return results;
    }

    /**
     * DTO for ranking combined with product information.
     */
    @lombok.Getter
    @lombok.Builder
    public static class RankingProductInfo {
        private int rank;
        private Double score;
        private Long productId;
        private String productName;
        private String brandName;
        private BigDecimal price;
        private Integer stock;
        private Long likeCount;
    }
}
