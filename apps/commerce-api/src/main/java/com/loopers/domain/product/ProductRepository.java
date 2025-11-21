package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);
    Product save(Product product);
    List<Product> findAll();
    List<Product> findAllBySortType(ProductSortType sortType);
    Optional<Product> findByIdForUpdate(Long id);
}
