package com.loopers.application.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.vo.Stock;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductDetailInfo {

    private final Long id;
    private final String name;
    private final String description;
    private final Long brandId;
    private final String brandName;
    private final String brandDescription;
    private final Money price;
    private final Stock stock;
    private final int likeCount;
    @JsonProperty("likedByMember")
    private final boolean isLikedByMember;
    private final Integer ranking;  // 순위 (1-based), 순위권 밖이면 null
}