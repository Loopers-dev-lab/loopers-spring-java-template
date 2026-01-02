package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

/**
 * 랭킹 정보 DTO
 */
public class RankingInfo {

    /**
     * 랭킹 페이지 응답
     */
    public record RankingsPageResponse(
            List<RankingItem> items,
            int page,
            int size,
            int total
    ) {
        public static RankingsPageResponse empty(Pageable pageable) {
            return new RankingsPageResponse(
                    Collections.emptyList(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    0
            );
        }

        public static RankingsPageResponse of(
                List<RankingItem> items,
                int page,
                int size,
                int total
        ) {
            return new RankingsPageResponse(items, page, size, total);
        }
    }

    /**
     * 랭킹 항목 (상품 정보 + 순위 + 점수)
     */
    public record RankingItem(
            Long rank,
            Long productId,
            Double score,
            String productName,
            Long brandId,
            Integer price
    ) {
        public static RankingItem of(Long rank, Long productId, Double score, Product product) {
            return new RankingItem(
                    rank,
                    productId,
                    score,
                    product.getName(),
                    product.getBrandId(),
                    product.getPrice().amount()
            );
        }
    }

    /**
     * 상품 랭킹 정보 (상세 조회용)
     */
    public record ProductRankingInfo(
            Long rank,
            Double score
    ) {
        public static ProductRankingInfo of(Long rank, Double score) {
            return new ProductRankingInfo(rank, score);
        }
    }
}

