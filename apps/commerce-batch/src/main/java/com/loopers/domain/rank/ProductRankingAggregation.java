package com.loopers.domain.rank;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductRankingAggregation {
    private Long productId;


    private Integer likeCount;


    private Integer viewCount;


    private Integer orderCount;

    private BigDecimal salesAmount;

    private Integer rankPosition;
}
