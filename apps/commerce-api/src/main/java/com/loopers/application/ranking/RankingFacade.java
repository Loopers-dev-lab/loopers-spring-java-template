package com.loopers.application.ranking;

import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductService;
import com.loopers.application.ranking.RankingService.RankingItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 랭킹 + 상품 정보 조합 Facade
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final ProductService productService;

    /**
     * 일간 랭킹 페이지 조회 (상품 정보 포함)
     */
    public List<RankingProductInfo> getDailyRanking(String date, int page, int size) {
        // 1. Redis ZSET에서 랭킹 조회
        List<RankingItem> rankingItems = rankingService.getDailyRanking(date, page, size);

        if (rankingItems.isEmpty()) {
            return List.of();
        }

        // 2. Product 정보 조회 (Batch)
        List<Long> productIds = rankingItems.stream()
            .map(RankingItem::getProductId)
            .toList();

        Map<Long, ProductInfo> productMap = productService.findByIds(productIds).stream()
            .collect(Collectors.toMap(ProductInfo::id, p -> p));

        // 3. 랭킹 + 상품 정보 조합
        List<RankingProductInfo> results = new ArrayList<>();
        for (RankingItem item : rankingItems) {
            ProductInfo product = productMap.get(item.getProductId());

            if (product == null) {
                log.warn("랭킹에 있지만 상품 정보 없음 - productId: {}", item.getProductId());
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
     * 오늘 랭킹 페이지 조회
     */
    public List<RankingProductInfo> getTodayRanking(int page, int size) {
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return getDailyRanking(today, page, size);
    }

    /**
     * 랭킹 + 상품 정보 DTO
     */
    @lombok.Getter
    @lombok.Builder
    public static class RankingProductInfo {
        private int rank;               // 순위
        private Double score;           // 점수
        private Long productId;         // 상품 ID
        private String productName;     // 상품명
        private String brandName;       // 브랜드명
        private BigDecimal price;       // 가격
        private Integer stock;          // 재고
        private Long likeCount;         // 좋아요 수
    }
}
