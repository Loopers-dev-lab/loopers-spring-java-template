package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.domain.product.ProductRankingList;
import com.loopers.core.domain.product.vo.ProductId;

import java.util.List;
import java.util.Optional;

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

    List<Product> saveAll(List<Product> products);

    Optional<Product> findById(ProductId productId);

    List<Product> findAllByIdIn(List<ProductId> productIds);

    ProductRankingList findRankingListBy(List<ProductId> productIds);
}
