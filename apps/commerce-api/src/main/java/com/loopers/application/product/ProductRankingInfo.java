package com.loopers.application.product;

import com.loopers.domain.product.Product;

import java.math.BigDecimal;

public record ProductRankingInfo(Long id, Long brandId, String name, BigDecimal price, int stock, int likeCount, int rank, double score) {
    public static ProductRankingInfo from(Product product, RankingInfo ranking) {
        return new ProductRankingInfo(
                product.getId(),
                product.getBrandId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getLikeCount(),
                ranking.rank(),
                ranking.score()
        );
    }
}
