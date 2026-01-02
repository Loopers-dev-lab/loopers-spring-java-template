package com.loopers.batch.dto;

import java.math.BigDecimal;

public record ProductRankRow(
        Long productId,
        long viewCountSum,
        long likeCountSum,
        long salesVolumeSum,
        BigDecimal score
) {
}
