package com.loopers.interfaces.api.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductInfo;
import com.loopers.interfaces.api.brand.BrandV1Dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductV1DTO {

    /**
     * 상품 목록 조회용 DTO
     * */
    public record ProductsResponse(
            List<ProductDetailResponse> products
    ) {
        public static ProductsResponse from(List<ProductDetailInfo> productDetailInfos) {
            return new ProductsResponse(productDetailInfos.stream()
                    .map(ProductDetailResponse::from)
                    .toList());
        }
    }

    /**
     * 상품 조회용 DTO
     * */
    public record ProductDetailResponse(
            Long id,
            String productCode,
            String productName,
            BigDecimal price,
            int stock,
            Long likeCount,
            BrandV1Dto.BrandResponse brand
    ) {
        public static ProductDetailResponse from(ProductDetailInfo productDetailInfo) {
            return new ProductDetailResponse(
                    productDetailInfo.id(),
                    productDetailInfo.productCode(),
                    productDetailInfo.productName(),
                    productDetailInfo.price(),
                    productDetailInfo.stock(),
                    productDetailInfo.likeCount(),
                    productDetailInfo.brand() != null ?
                            BrandV1Dto.BrandResponse.from(productDetailInfo.brand()) : null
            );
        }
    }

    /**
     * 상품 수정 요청 DTO
     */
    public record UpdateProductRequest(
            String productName,
            BigDecimal price
    ) {}

}
