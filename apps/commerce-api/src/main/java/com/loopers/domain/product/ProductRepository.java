package com.loopers.domain.product;

import java.util.List;

public interface ProductRepository {

    List<Product> findAll(ProductSortType sortType, int page, int size);

    List<Product> findByBrandId(Long brandId, ProductSortType sortType, int page, int size);

    Product save(Product product);
}
