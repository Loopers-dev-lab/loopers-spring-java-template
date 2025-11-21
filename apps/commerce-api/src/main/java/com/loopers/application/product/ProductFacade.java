package com.loopers.application.product;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFacade {

    private final ProductService productService;
    private final LikeService likeService;

    public ProductDetailInfo getProductDetail(Long productId) {
        Product product = productService.getProduct(productId);
        Long likeCount = likeService.getLikeCount(product);
        return ProductDetailInfo.of(product, likeCount);
    }

    public ProductListInfo getProducts(ProductGetListCommand command) {
        ProductSearchCondition condition = new ProductSearchCondition(
                command.brandId(),
                command.pageable()
        );

        Page<Product> productPage = productService.getProducts(condition);
        Map<Long, Long> likeCountMap = likeService.getLikeCounts(productPage.getContent());
        return ProductListInfo.of(productPage, likeCountMap);
    }
}
