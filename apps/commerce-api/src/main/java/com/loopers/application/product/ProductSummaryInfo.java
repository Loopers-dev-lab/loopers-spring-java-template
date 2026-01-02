package com.loopers.application.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loopers.domain.common.vo.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSummaryInfo {

    private final Long id;
    private final String name;
    private final String brandName;
    private final Money price;
    private final int likeCount;
    @JsonProperty("likedByMember")
    private final boolean isLikedByMember;
}