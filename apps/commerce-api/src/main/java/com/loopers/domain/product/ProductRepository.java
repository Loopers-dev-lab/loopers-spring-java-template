package com.loopers.domain.product;

import com.loopers.infrastructure.product.ProductListView;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);
    Product save(Product product);
    List<Product> findAll();
    Optional<Product> findByIdForUpdate(Long id);
    List<ProductListView> findListViewByCondition(ProductSearchCondition condition);
    List<Product> findByIdIn(List<Long> ids);
}
