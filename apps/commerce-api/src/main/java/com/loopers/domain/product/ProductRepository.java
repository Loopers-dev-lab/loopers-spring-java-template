package com.loopers.domain.product;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Page<ProductModel> search(Long brandId, String sort, int page, int size);

    ProductModel save(ProductModel productModel);

    void deleteAll();

    Optional<ProductModel> findById(Long aLong);
    
    Optional<ProductModel> findByIdForUpdate(Long id);
    
    List<ProductModel> findByIdIn(List<Long> productIds);
}
