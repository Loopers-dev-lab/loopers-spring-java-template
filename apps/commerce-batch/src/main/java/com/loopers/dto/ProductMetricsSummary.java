package com.loopers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ProductMetricsSummary {

    private Long productId;
    private LocalDate date;
    private Long likeCount;
    private Long stockCount;
    private Long viewCount;
}
