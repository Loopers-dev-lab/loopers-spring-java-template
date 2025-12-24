package com.loopers.core.service.product.query;

import java.time.LocalDate;

public record GetProductRankingQuery(
        LocalDate date,
        int pageNo,
        int pageSize
) {
}
