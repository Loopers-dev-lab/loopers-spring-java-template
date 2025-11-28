package com.loopers.application.product;

import com.loopers.domain.product.ProductSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFacade {

    private final ProductCacheService productCacheService;

    public ProductDetailInfo getProductDetail(Long productId) {
        return productCacheService.getProductDetailWithCache(productId);
    }

    public ProductListInfo getProducts(ProductGetListCommand command) {
        String cacheKey = String.format("brand:%s:sort:%s:page:%d:size:%d",
                command.brandId() != null ? command.brandId() : "all",
                command.sort() != null ? command.sort() : "latest",
                command.pageable().getPageNumber(),
                command.pageable().getPageSize()
        );

        ProductSearchCondition condition = new ProductSearchCondition(
                command.brandId(),
                command.getSortType(),
                command.pageable()
        );

        return productCacheService.getProductListWithCache(cacheKey, condition);
    }
}
