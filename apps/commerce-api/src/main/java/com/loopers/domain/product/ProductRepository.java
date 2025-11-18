package com.loopers.domain.product;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(Long id);

    Page<Product> findAll(ProductSortType sortType, int page, int size);

    List<Product> findByBrandId(Long brandId);

    Page<Product> findByBrandId(Long brandId, ProductSortType sortType, int page, int size);

    Product save(Product product);

    List<Product> findAllByIdIn(List<Long> ids);

}
