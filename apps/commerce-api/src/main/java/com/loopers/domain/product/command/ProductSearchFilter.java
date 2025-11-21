package com.loopers.domain.product.command;

import com.loopers.domain.product.enums.ProductSortCondition;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSearchFilter {
    
    private final String keyword;
    private final ProductSortCondition sortCondition;
    
    public static ProductSearchFilter of(String keyword, ProductSortCondition sortCondition) {
        return ProductSearchFilter.builder()
                .keyword(keyword)
                .sortCondition(sortCondition)
                .build();
    }
}