package com.loopers.application.ranking;

import java.math.BigDecimal;

public record RankingProductInfo(
        Long productId,
        String name,
        BigDecimal price,
        Long likeCount
) {}



