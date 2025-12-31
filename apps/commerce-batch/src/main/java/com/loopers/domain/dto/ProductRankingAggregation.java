package com.loopers.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Data Transfer Object for product ranking aggregation results.
 *
 * <p>This DTO carries aggregated metric data from database queries
 * to batch processors for further calculation and ranking assignment.
 */
@Getter
@AllArgsConstructor
public class ProductRankingAggregation {

    /**
     * Product identifier
     */
    private Long productId;

    /**
     * Total like count for the period
     */
    private Integer likeCount;

    /**
     * Total view count for the period
     */
    private Integer viewCount;

    /**
     * Total order count for the period
     */
    private Integer orderCount;

    /**
     * Total sales amount for the period
     */
    private BigDecimal salesAmount;

    /**
     * Calculated rank position (1-based)
     */
    private Integer rankPosition;
}
