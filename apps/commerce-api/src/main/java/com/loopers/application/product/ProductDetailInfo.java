package com.loopers.application.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.vo.Stock;
import lombok.Builder;

@Builder
public class ProductDetailInfo {
    
    private final Long id;
    private final String name;
    private final String description;
    private final String brandName;
    private final String brandDescription;
    private final Money price;
    private final Stock stock;
    private final int likeCount;
    private final boolean isLikedByMember;
    
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getBrandName() { return brandName; }
    public String getBrandDescription() { return brandDescription; }
    public Money getPrice() { return price; }
    public Stock getStock() { return stock; }
    public int getLikeCount() { return likeCount; }
    
    @JsonProperty("likedByMember")
    public boolean isLikedByMember() { return isLikedByMember; }
}