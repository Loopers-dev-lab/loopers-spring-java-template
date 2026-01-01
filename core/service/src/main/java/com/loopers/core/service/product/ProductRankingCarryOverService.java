package com.loopers.core.service.product;

import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProductRankingCarryOverService {

    private final ProductRankingCacheRepository productRankingCacheRepository;

    @Value("${product.ranking.score.weight.carryOver}")
    private Double carryOverWeight;

    public void carryOver() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        productRankingCacheRepository.carryOver(today, tomorrow, carryOverWeight);
    }
}
