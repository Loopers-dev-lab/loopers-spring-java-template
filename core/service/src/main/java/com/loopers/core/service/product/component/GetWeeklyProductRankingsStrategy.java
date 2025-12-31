package com.loopers.core.service.product.component;

import com.loopers.core.domain.product.repository.WeeklyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductRankings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class GetWeeklyProductRankingsStrategy implements GetProductRankingsStrategy {

    private final WeeklyProductMetricRepository weeklyProductMetricRepository;

    @Value("${product.ranking.score.weight.pay}")
    private Double payWeight;

    @Value("${product.ranking.score.weight.view}")
    private Double viewWeight;

    @Value("${product.ranking.score.weight.like}")
    private Double likeWeight;

    @Override
    public ProductRankings getRankings(LocalDate date, Integer pageNo, Integer pageSize) {


        return null;
    }
}
