package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCondition;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductView;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;
    private final StockService stockService;

    /**
     * 상품 목록 조회 (ProductView 기반)
     */
    @Transactional(readOnly = true)
    public Page<ProductView> getProductViews(ProductCondition condition, Pageable pageable) {
        return productService.findProductViews(condition, pageable);
    }

    /**
     * 상품 상세 조회 (ProductView 기반)
     */
    @Transactional(readOnly = true)
    public ProductView getProductView(Long productId) {
        return productService.findProductViewById(productId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "상품을 찾지 못했습니다."
                ));
    }

    /**
     * 상품 저장
     */
    @Transactional
    public Product saveProduct(Product product) {
        Product savedProduct = productService.saveProduct(product)
                .orElseThrow(() -> new CoreException(
                        ErrorType.INTERNAL_ERROR,
                        "Product 저장에 실패했습니다."
                ));

        // Product 저장 후 Stock 별도 생성
        Stock stock = Stock.builder()
                .productId(savedProduct.getId())
                .quantity(0L)
                .build();
        stockService.saveStock(stock)
                .orElseThrow(() -> new CoreException(
                        ErrorType.INTERNAL_ERROR,
                        "Stock 저장에 실패했습니다."
                ));

        return savedProduct;
    }
}

