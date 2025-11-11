package com.loopers.domain.product;

import java.util.List;

public interface ProductRepository {

    List<Product> findAll();

    List<Product> findByBrandId(Long brandId);

    Product save(Product product);
}
