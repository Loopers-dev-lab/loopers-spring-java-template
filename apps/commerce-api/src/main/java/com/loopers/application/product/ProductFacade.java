package com.loopers.application.product;

import com.loopers.domain.product.service.ProductReadService;
import com.loopers.domain.product.command.ProductSearchFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
@Transactional
public class ProductFacade {

    private final ProductReadService productReadService;

    @Transactional(readOnly = true)
    public Page<ProductSummaryInfo> getProducts(ProductSearchCommand command) {
        ProductSearchFilter filter = ProductSearchFilter.of(
                command.getKeyword(),
                command.getSort()
        );

        Pageable pageable = PageRequest.of(command.getPage(), command.getSize());

        return productReadService.getProducts(
                filter, 
                pageable, 
                command.getMemberIdOrNull()
        );
    }

    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, String memberIdOrNull) {
        return productReadService.getProductDetail(productId, memberIdOrNull);
    }
}