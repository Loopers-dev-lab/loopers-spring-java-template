package com.loopers.application.product;

import com.loopers.domain.common.vo.Price;
import com.loopers.domain.supply.vo.Stock;

public record ProductCreateRequest(
        String name,
        Long brandId,
        Price price,
        Stock stock,
        Integer likeCount
) {
}
