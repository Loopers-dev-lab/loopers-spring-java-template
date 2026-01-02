package com.loopers.domain.ranking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankingType {
    LIKE("ranking:like", "좋아요"),
    VIEW("ranking:view", "조회수"),
    ORDER("ranking:order", "주문수"),
    ALL("ranking:all", "종합");

    private final String keyPrefix;
    private final String description;
}
