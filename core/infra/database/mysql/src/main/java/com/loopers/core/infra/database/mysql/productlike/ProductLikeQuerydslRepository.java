package com.loopers.core.infra.database.mysql.productlike;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.infra.database.mysql.productlike.dto.LikeProductListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductLikeQuerydslRepository {

    Page<LikeProductListProjection> findLikeProductListWithCondition(
            Long userId, Long brandId, OrderSort createdAtSort, OrderSort priceSort, OrderSort likeCountSort, Pageable pageable
    );
}
