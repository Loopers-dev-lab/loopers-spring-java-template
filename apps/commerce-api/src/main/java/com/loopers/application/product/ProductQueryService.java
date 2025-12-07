package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeCount;
import com.loopers.domain.product.ProductLikeCountService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductQueryService {
    private final ProductService productService;
    private final ProductLikeCountService productLikeCountService;

    public Page<ProductInfo> getProductsSortedByLikes(Long brandId, Pageable pageable) {
        // MV 조회
        Page<ProductLikeCount> plcPage =
                productLikeCountService.getProductLikeCountsOrderByLikeCount(brandId, pageable);

        // 상품 데이터 조회
        Map<Long, Product> products = productService.getProductsMapByIds(plcPage.map(ProductLikeCount::getProductId).getContent());

        // 조합
        List<ProductInfo> infoList =
                plcPage.stream()
                        .map(plc -> ProductInfo.from(products.get(plc.getProductId()), plc))
                        .toList();

        return new PageImpl<>(infoList, pageable, plcPage.getTotalElements());
    }

    public Page<ProductInfo> getProducts(Long brandId, Pageable pageable, String sort) {
        return productService.getProducts(brandId, pageable, sort);
    }
}
