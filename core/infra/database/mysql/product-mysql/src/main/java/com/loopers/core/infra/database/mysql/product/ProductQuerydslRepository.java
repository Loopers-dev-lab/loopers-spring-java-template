package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.dto.ProductListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductQuerydslRepository {

    Page<ProductListProjection> findListWithCondition(
            Long brandId, String createdAtSort, String priceSort, String likeCountSort, Pageable pageable
    );
}
