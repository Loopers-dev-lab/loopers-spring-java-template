package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.domain.product.vo.ProductId;

import java.util.List;

public interface ProductRepository {

    void bulkSaveOrUpdate(List<Product> products);

    Product getById(ProductId productId);

    Product getByIdWithLock(ProductId productId);

    ProductListView findListWithCondition(
            BrandId brandId,
            OrderSort createdAtSort,
            OrderSort priceSort,
            OrderSort likeCountSort,
            int pageNo,
            int pageSize
    );

    Product save(Product product);
}
