package com.loopers.domain.like;

import com.loopers.domain.product.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductLikeSummaryVO {
    private Long productId;
    private String productName;
    private Long brandId;
    private String brandName;
    private Integer price;
    private ProductStatus status;
    private long likeCount;
}
