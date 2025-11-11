package com.loopers.interfaces.api.product;

import java.util.List;

public class ProductDto {

    public record ProductListResponse(
            List<ProductResponse> products,
            int totalCount
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            int price,
            int likeCount,
            BrandSummary brand
    ) {
    }

    public record BrandSummary(
            Long id,
            String name
    ) {
    }
}
