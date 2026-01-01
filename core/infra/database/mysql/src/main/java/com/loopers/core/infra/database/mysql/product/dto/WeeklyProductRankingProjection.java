package com.loopers.core.infra.database.mysql.product.dto;

import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductRanking;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class WeeklyProductRankingProjection {

    private final Long productId;
    private final Long ranking;
    private final Double score;

    @QueryProjection
    public WeeklyProductRankingProjection(Long productId, Long ranking, Double score) {
        this.productId = productId;
        this.ranking = ranking;
        this.score = score;
    }

    public ProductRanking to() {
        return new ProductRanking(
                new ProductId(this.productId.toString()),
                ranking,
                score
        );
    }
}
