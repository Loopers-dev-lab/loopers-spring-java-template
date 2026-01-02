package com.loopers.application.ranking;

import java.math.BigDecimal;

public record RankingProductInfo(
        int rank,
        Double score,
        Long productId,
        String name,
        BigDecimal price,
        Integer stock,
        Long likeCount
) {}



