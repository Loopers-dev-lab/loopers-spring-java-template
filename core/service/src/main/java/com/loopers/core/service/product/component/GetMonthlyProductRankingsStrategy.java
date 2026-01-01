package com.loopers.core.service.product.component;

import com.loopers.core.domain.product.repository.MonthlyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductRankings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class GetMonthlyProductRankingsStrategy implements GetProductRankingsStrategy {

    private final MonthlyProductMetricRepository monthlyProductMetricRepository;

    @Value("${product.ranking.score.weight.pay}")
    private Double payWeight;

    @Value("${product.ranking.score.weight.view}")
    private Double viewWeight;

    @Value("${product.ranking.score.weight.like}")
    private Double likeWeight;

    @Override
    public boolean supports(String type) {
        return type.equals("MONTHLY");
    }

    @Override
    public ProductRankings getRankings(LocalDate date, Integer pageNo, Integer pageSize) {
        return monthlyProductMetricRepository.findRankingsBy(date, pageNo, pageSize, payWeight, viewWeight, likeWeight);
    }
}
