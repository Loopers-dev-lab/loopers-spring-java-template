package com.loopers.application.ranking;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.ranking.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final PeriodRankingService periodRankingService;
    private final ProductService productService;

    /**
     * TOP N 랭킹 조회 (상품 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<RankingInfo> getTopRanking(
            RankingType rankingType,
            PeriodType periodType,
            LocalDate date,
            int limit
    ) {
        // period가 null이면 DAILY로 처리
        PeriodType period = periodType != null ? periodType : PeriodType.DAILY;

        List<Ranking> entries = switch (period) {
            case DAILY -> rankingService.getTopRanking(rankingType, LocalDate.now(), limit);
            case WEEKLY -> periodRankingService.getTopWeeklyRanking(rankingType, date, limit);
            case MONTHLY -> periodRankingService.getTopMonthlyRanking(rankingType, date, limit);
        };

        if (entries.isEmpty()) {
            return List.of();
        }

        return enrichWithProductInfo(entries);
    }

    /**
     * 페이지네이션 랭킹 조회
     */
    @Transactional(readOnly = true)
    public List<RankingInfo> getRankingWithPaging(
            RankingType rankingType,
            PeriodType periodType,
            LocalDate date,
            int page,
            int size
    ) {

        PeriodType period = periodType != null ? periodType : PeriodType.DAILY;

        List<Ranking> entries = switch (period) {
            case DAILY -> rankingService.getRankingWithPaging(rankingType, LocalDate.now(), page, size);
            case WEEKLY -> periodRankingService.getWeeklyRankingWithPaging(rankingType, date, page, size);
            case MONTHLY -> periodRankingService.getMonthlyRankingWithPaging(rankingType, date, page, size);
        };

        if (entries.isEmpty()) {
            return List.of();
        }

        return enrichWithProductInfo(entries);
    }

    /**
     * 특정 상품의 특정 랭킹 조회
     */
    @Transactional(readOnly = true)
    public RankingInfo getProductRanking(
            RankingType rankingType,
            LocalDate date,
            Long productId
    ) {
        Ranking entry = rankingService.getProductRanking(rankingType, date, productId);

        if (entry == null) {
            return null;
        }

        Product product = productService.getProductById(productId);

        return RankingInfo.of(entry, ProductInfo.from(product));
    }

    /**
     * 특정 상품의 모든 타입 랭킹 조회 (LIKE, VIEW, ORDER, ALL)
     *
     * @param productId 조회할 상품 ID
     * @param date 조회 날짜 (null이면 오늘)
     * @return ProductRankings (랭킹 정보가 하나도 없으면 null)
     */
    @Transactional(readOnly = true)
    public RankingInfo.ProductRankings getAllRankingsForProduct(Long productId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // 4가지 타입의 랭킹을 조회
        Ranking likeRanking =
                rankingService.getProductRanking(RankingType.LIKE, targetDate, productId);
        Ranking viewRanking =
                rankingService.getProductRanking(RankingType.VIEW, targetDate, productId);
        Ranking orderRanking =
                rankingService.getProductRanking(RankingType.ORDER, targetDate, productId);
        Ranking allRanking =
                rankingService.getProductRanking(RankingType.ALL, targetDate, productId);

        return RankingInfo.ProductRankings.of(
                likeRanking,
                viewRanking,
                orderRanking,
                allRanking
        );
    }

    /**
     * RankingEntry 리스트에 상품 정보 결합
     */
    private List<RankingInfo> enrichWithProductInfo(List<Ranking> entries) {
        List<Long> productIds = entries.stream()
                .map(Ranking::getProductId)
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productService.getAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        return entries.stream()
                .map(entry -> {
                    Product product = productMap.get(entry.getProductId());
                    ProductInfo productInfo = product != null
                            ? ProductInfo.from(product)
                            : null;
                    return RankingInfo.of(entry, productInfo);
                })
                .collect(Collectors.toList());
    }

    /**
     * 전체 랭킹 개수 조회
     */
    public long getTotalRankingCount(RankingType rankingType, LocalDate date) {
        return rankingService.getTotalRankingCount(rankingType, date);
    }
}
