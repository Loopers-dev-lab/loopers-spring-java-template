package com.loopers.domain.product;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product registerProduct(Product product);

    boolean existsProductCode(String productCode);

    List<Product> findAll(Specification<Product> spec, int page, int size, ProductSortType sortType);

    Optional<Product> findByIdWithBrand(Long productId);

    Optional<Product> findById(Long productId);
    Optional<Product> findByIdWithLock(Long productId);
}
