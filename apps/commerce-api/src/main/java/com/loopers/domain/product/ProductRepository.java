package com.loopers.domain.product;

public interface ProductRepository {
    Product registerProduct(Product product);

    boolean existsProductCode(String productCode);
}
