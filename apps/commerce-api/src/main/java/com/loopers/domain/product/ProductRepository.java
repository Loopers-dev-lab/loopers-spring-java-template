package com.loopers.domain.product;

import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.view.ProductView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> save(Product product);
    Optional<Product> findById(Long productId);
    Page<ProductView> findProductViews(ProductCondition condition, Pageable pageable);
    Optional<ProductView> findProductViewById(Long productId);
    
}
