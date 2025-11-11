package com.loopers.domain.product;

import java.util.List;

public interface ProductRepository {

    List<Product> findAll();

    Product save(Product product);
}
