package com.loopers.domain.product;

import com.loopers.interfaces.api.product.ProductV1Dto;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);

    Product save(Product product);

    List<Product> findAll();

    List<Product> searchProductsByCondition(ProductV1Dto.SearchProductRequest request);
}
