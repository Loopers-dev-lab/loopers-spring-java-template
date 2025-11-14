package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCondition;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;

    /**
     * 상품 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(ProductCondition condition, Pageable pageable) {
        Page<Product> products = productService.findProducts(condition, pageable);
        return products.map(ProductInfo::from);
    }

    /**
     * 상품 상세 조회 (Product + Brand 조합)
     * 도메인 서비스에서 Product + Brand 조합 로직을 처리하도록 위임
     */
    @Transactional(readOnly = true)
    public ProductInfo getProduct(Long productId) {
        ProductService.ProductWithBrand productWithBrand = productService.getProductDetailWithBrand(productId);
        return ProductInfo.from(productWithBrand);
    }

    /**
     * 상품 저장
     */
    @Transactional
    public ProductInfo saveProduct(ProductInfo productInfo) {
        
        Product product = Product.builder()
                .name(productInfo.name())
                .description(productInfo.description())
                .price(productInfo.price())
                .likeCount(productInfo.likeCount())
                .status(productInfo.status())
                .isVisible(productInfo.isVisible())
                .isSellable(productInfo.isSellable())
                .build();

        Product savedProduct = productService.saveProduct(product)
                .orElseThrow(() -> new com.loopers.support.error.CoreException(
                        com.loopers.support.error.ErrorType.INTERNAL_ERROR,
                        "Product 저장에 실패했습니다."
                ));
        return ProductInfo.from(savedProduct);
    }
}

