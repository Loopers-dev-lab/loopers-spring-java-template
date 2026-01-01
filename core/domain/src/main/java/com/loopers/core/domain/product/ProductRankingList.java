package com.loopers.core.domain.product;

import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.product.vo.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ProductRankingList(
        List<ProductRankingItem> products,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static ProductRankingList create(List<ProductRankingItem> products) {
        return new ProductRankingList(products, 0, 0, false, false);
    }

    public ProductRankingList with(ProductRankings rankings) {
        Map<ProductId, ProductRanking> rankingsByProductId = rankings.getRankings().stream()
                .collect(Collectors.toMap(
                        ProductRanking::productId,
                        r -> r,
                        (existing, replacement) -> replacement
                ));

        List<ProductRankingItem> updatedProducts = this.products.stream()
                .map(item -> {
                    ProductRanking ranking = rankingsByProductId.get(item.id());
                    return item.with(ranking);
                })
                .toList();

        return new ProductRankingList(
                updatedProducts,
                rankings.getTotalElements(),
                rankings.getTotalPages(),
                rankings.isHasNext(),
                rankings.isHasPrevious()
        );
    }

    public record ProductRankingItem(
            ProductId id,
            Long ranking,
            BrandName brandName,
            ProductName name,
            ProductPrice price,
            ProductLikeCount likeCount,
            Double score
    ) {
        public ProductRankingItem with(ProductRanking ranking) {
            return new ProductRankingItem(id, ranking.ranking(), brandName, name, price, likeCount, ranking.score());
        }
    }
}
