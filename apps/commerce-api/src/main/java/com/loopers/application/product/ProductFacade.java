package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;

    public ProductDetailInfo getProductDetail(Long productId) {
        Product product = productService.getProductDetail(productId);
        return ProductDetailInfo.from(product);
    }
}