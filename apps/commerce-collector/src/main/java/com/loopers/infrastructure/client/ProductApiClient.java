package com.loopers.infrastructure.client;

import com.loopers.config.FeignConfig;
import com.loopers.infrastructure.client.dto.ProductDetailExternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "commerce-api",
        url = "${external.commerce-api.url}",
        configuration = FeignConfig.class
)
public interface ProductApiClient {

    /**
     * 상품 상세 조회
     */
    @GetMapping("/api/v1/products/{productId}")
    ProductDetailExternalDto.ProductDetailResponse getProductDetail(
            @PathVariable("productId") Long productId
    );

}
