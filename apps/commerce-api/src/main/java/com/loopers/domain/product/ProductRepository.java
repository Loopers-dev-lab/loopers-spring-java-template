package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findByIdWithLock(Long productId);

    List<Product> getProductList(ProductSortType sortType);

    List<Product> findAllByIdInWithLock(List<Long> productIdList);

    Optional<Product> findById(Long productId);
}
