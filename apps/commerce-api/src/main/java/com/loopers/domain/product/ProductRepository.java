package com.loopers.domain.product;

import org.springframework.data.domain.Page;

public interface ProductRepository {
    Page<ProductModel> search(Long brandId, String sort, int page, int size);

    ProductModel save(ProductModel productModel);

    void deleteAll();
}
