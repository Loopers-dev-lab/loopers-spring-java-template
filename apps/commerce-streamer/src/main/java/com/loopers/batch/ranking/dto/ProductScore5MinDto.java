package com.loopers.batch.ranking.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Step 1에서 사용하는 DTO
 */
@Getter
@Setter
@Builder
public class ProductScore5MinDto {
    private Long productId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal orderAmountSum;
    private Long likeCount;
    private Long viewCount;
}

