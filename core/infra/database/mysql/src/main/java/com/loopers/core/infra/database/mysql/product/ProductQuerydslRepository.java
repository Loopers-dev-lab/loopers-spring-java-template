package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.infra.database.mysql.product.dto.ProductListProjection;
import com.loopers.core.infra.database.mysql.product.dto.ProductRankingListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductQuerydslRepository {

    Page<ProductListProjection> findListWithCondition(
            Long brandId, OrderSort createdAtSort, OrderSort priceSort, OrderSort likeCountSort, Pageable pageable
    );

    List<ProductRankingListProjection> findRankingList(List<Long> productIds);
}
