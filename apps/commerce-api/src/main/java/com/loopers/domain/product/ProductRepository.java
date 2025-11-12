package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(Long id);

    List<Product> findAll(ProductSortType sortType, int page, int size);

    List<Product> findByBrandId(Long brandId, ProductSortType sortType, int page, int size);

    Product save(Product product);

}
