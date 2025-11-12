package com.loopers.domain.product;

import java.util.List;

public interface ProductRepository {
    Product registerProduct(Product product);

    boolean existsProductCode(String productCode);

    List<Product> findAllBySortType(ProductSortType sortType);
}
