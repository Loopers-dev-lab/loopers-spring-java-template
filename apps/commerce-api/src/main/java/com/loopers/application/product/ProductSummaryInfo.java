package com.loopers.application.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loopers.domain.common.vo.Money;
import lombok.Builder;

@Builder
public class ProductSummaryInfo {
    
    private final Long id;
    private final String name;
    private final String brandName;
    private final Money price;
    private final int likeCount;
    private final boolean isLikedByMember;
    
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBrandName() { return brandName; }
    public Money getPrice() { return price; }
    public int getLikeCount() { return likeCount; }
    
    @JsonProperty("likedByMember")
    public boolean isLikedByMember() { return isLikedByMember; }
}