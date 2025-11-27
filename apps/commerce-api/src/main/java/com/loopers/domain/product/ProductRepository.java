package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
    Optional<Product> findByIdWithLock(Long productId);

    List<Product> getProductList(Long brandId, ProductSortType sortType, Pageable pageable);

    List<Product> findAllByIdInWithLock(List<Long> productIdList);

    Optional<Product> findById(Long productId);
}
