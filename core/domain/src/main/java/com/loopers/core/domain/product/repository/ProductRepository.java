package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.common.vo.PageNo;
import com.loopers.core.domain.common.vo.PageSize;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductListView;

public interface ProductRepository {

    ProductListView findListWithCondition(
            BrandId brandId,
            OrderSort createdAtSort,
            OrderSort priceSort,
            OrderSort likeCountSort,
            PageNo pageNo,
            PageSize pageSize
    );

    Product save(Product product);
}
