package com.loopers.domain.product;

import java.util.List;

public interface ProductViewLogRepository {

    List<ProductViewLog> saveAll(List<ProductViewLog> logs);
}
