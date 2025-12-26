package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductViewLog;
import com.loopers.domain.product.ProductViewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductViewLogRepositoryImpl implements ProductViewLogRepository {

    private final ProductViewLogJpaRepository productViewLogJpaRepository;

    @Override
    public List<ProductViewLog> saveAll(List<ProductViewLog> logs) {
        return productViewLogJpaRepository.saveAll(logs);
    }
}
