package com.loopers.batch.ranking.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Step 2에서 사용하는 슬라이딩 윈도우 업데이트 데이터
 */
@Getter
@Builder
public class RankingUpdateData {
    private Long productId;
    private BigDecimal newOrderAmount;
    private Long newLikeCount;
    private Long newViewCount;
    private BigDecimal oldOrderAmount;
    private Long oldLikeCount;
    private Long oldViewCount;
    private LocalDateTime processedTime;
    
    /**
     * NEW 데이터만 있는 경우 (처음 윈도우 누적)
     */
    public static RankingUpdateData fromNewData(Long productId, BigDecimal newOrderAmount, 
                                                 Long newLikeCount, Long newViewCount, 
                                                 LocalDateTime processedTime) {
        return RankingUpdateData.builder()
            .productId(productId)
            .newOrderAmount(newOrderAmount)
            .newLikeCount(newLikeCount)
            .newViewCount(newViewCount)
            .oldOrderAmount(BigDecimal.ZERO)
            .oldLikeCount(0L)
            .oldViewCount(0L)
            .processedTime(processedTime)
            .build();
    }
    
    /**
     * NEW와 OLD 데이터가 모두 있는 경우 (슬라이딩 윈도우)
     */
    public static RankingUpdateData fromSlidingWindow(Long productId, 
                                                      BigDecimal newOrderAmount, Long newLikeCount, Long newViewCount,
                                                      BigDecimal oldOrderAmount, Long oldLikeCount, Long oldViewCount,
                                                      LocalDateTime processedTime) {
        return RankingUpdateData.builder()
            .productId(productId)
            .newOrderAmount(newOrderAmount)
            .newLikeCount(newLikeCount)
            .newViewCount(newViewCount)
            .oldOrderAmount(oldOrderAmount)
            .oldLikeCount(oldLikeCount)
            .oldViewCount(oldViewCount)
            .processedTime(processedTime)
            .build();
    }
}

