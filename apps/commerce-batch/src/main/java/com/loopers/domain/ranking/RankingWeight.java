package com.loopers.domain.ranking;

import lombok.Getter;

@Getter
public enum RankingWeight {
    VIEW(0.1, "조회수"),
    LIKE(0.2, "좋아요"),
    ORDER_CREATED(0.7, "주문 생성");

    private final double weight;
    private final String description;

    RankingWeight(double weight, String description) {
        this.weight = weight;
        this.description = description;
    }
}


