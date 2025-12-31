package com.loopers.core.service.product.component;

import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.domain.product.vo.ProductRankings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class GetDailyProductRankingsStrategy implements GetProductRankingsStrategy {

    private final ProductRankingCacheRepository productRankingCacheRepository;

    @Override
    public ProductRankings getRankings(LocalDate date, Integer pageNo, Integer pageSize) {
        return productRankingCacheRepository.getRankings(date, pageNo, pageSize);
    }
}
