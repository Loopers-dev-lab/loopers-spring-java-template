package com.loopers.application.product;

import com.loopers.domain.product.enums.ProductSortCondition;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSearchCommand {
    
    private final String keyword;
    private final ProductSortCondition sort;
    private final int page;
    private final int size;
    private final String memberIdOrNull;
    
    public static ProductSearchCommand of(String keyword, ProductSortCondition sort, int page, int size, String memberIdOrNull) {
        return ProductSearchCommand.builder()
                .keyword(keyword)
                .sort(sort)
                .page(page)
                .size(size)
                .memberIdOrNull(memberIdOrNull)
                .build();
    }
}
