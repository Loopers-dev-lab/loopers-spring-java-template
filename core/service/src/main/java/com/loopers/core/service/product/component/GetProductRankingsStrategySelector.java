package com.loopers.core.service.product.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProductRankingsStrategySelector {

    private final List<GetProductRankingsStrategy> strategies;

    public GetProductRankingsStrategy select(String type) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 타입의 랭킹입니다."));
    }
}
