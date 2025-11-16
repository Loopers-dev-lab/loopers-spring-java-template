package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long id);

    Page<Product> findAll(Pageable pageable);

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    List<Product> findByIdIn(List<Long> ids);

    Page<Product> findAllSorted(Pageable pageable, ProductSortType sortType);

    Page<Product> findByBrandIdSorted(Long brandId, Pageable pageable, ProductSortType sortType);
}
