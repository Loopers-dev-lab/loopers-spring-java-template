package com.loopers.application.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.common.Quantity;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;

    // 상품 다건 조회 - 페이징 지원
    public Page<ProductInfo> getProducts(Pageable pageable, String sort, String brandName) {
        Page<ProductModel> productPage = productService.getProducts(pageable, sort, brandName);
        return productPage.map(ProductInfo::from);
    }

    // 상품 단건 조회
    public ProductInfo getProduct(Long id){
        ProductModel product = productService.getProduct(id);
        if (product == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.");
        }
        return ProductInfo.from(product);
    }

    // 상품 재고 조회
    public Quantity getQuantity(Long id) {
        return productService.getQuantity(id).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다."));
    }

    public void updateQuantity(Long id, Quantity quantity) {
        productService.updateQuantity(id, quantity);
    }
}
