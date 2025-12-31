package com.loopers.core.service.product.component;

import com.loopers.core.domain.product.vo.ProductRankings;

import java.time.LocalDate;

public interface GetProductRankingsStrategy {

    ProductRankings getRankings(LocalDate date, Integer pageNo, Integer pageSize);
}
