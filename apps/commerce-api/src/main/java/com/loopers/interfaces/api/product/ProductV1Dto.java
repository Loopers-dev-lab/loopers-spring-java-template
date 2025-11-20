package com.loopers.interfaces.api.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.product.ProductInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class ProductV1Dto {

    public record CreateProductRequest(
        @NotNull(message = "브랜드 ID는 필수입니다.")
        Long brandId,
        @NotBlank(message = "상품명은 필수입니다.")
        String name,
        @NotNull(message = "가격은 필수입니다.")
        @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
        BigDecimal price,
        @NotNull(message = "재고는 필수입니다.")
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        Integer stock,
        String description
    ) {
    }

    public record UpdateProductRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        String name,
        @NotNull(message = "가격은 필수입니다.")
        @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
        BigDecimal price,
        @NotNull(message = "재고는 필수입니다.")
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        Integer stock,
        String description
    ) {
    }

    public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String description,
        BrandResponse brand,
        Long likeCount
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.id(),
                info.name(),
                info.price(),
                info.stock(),
                info.description(),
                BrandResponse.from(info.brand()),
                info.likeCount()
            );
        }
    }

    public record BrandResponse(
        Long id,
        String name,
        String description
    ) {
        public static BrandResponse from(BrandInfo info) {
            return new BrandResponse(
                info.id(),
                info.name(),
                info.description()
            );
        }
    }
}
