package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductGetListCommand;
import com.loopers.application.product.ProductListInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;

    @Override
    public ApiResponse<ProductV1Dto.ProductListResponse> getProducts(
            Long brandId,
            String sort,
            int page,
            int size
    ) {
        ProductGetListCommand command = new ProductGetListCommand(
                brandId,
                sort,
                PageRequest.of(page, size)
        );

        ProductListInfo listInfo = productFacade.getProducts(command);
        return ApiResponse.success(ProductV1Dto.ProductListResponse.from(listInfo));
    }

    @Override
    public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(Long productId, String userId) {
        ProductDetailInfo detailInfo;

        if (userId != null && !userId.isBlank()) {
            detailInfo = productFacade.getProductDetail(productId, userId);
        } else {
            detailInfo = productFacade.getProductDetail(productId);
        }

        return ApiResponse.success(ProductV1Dto.ProductDetailResponse.from(detailInfo));
    }
}
