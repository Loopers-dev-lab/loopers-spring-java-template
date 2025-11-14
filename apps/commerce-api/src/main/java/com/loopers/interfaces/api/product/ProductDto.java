package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.ProductCondition;
import com.loopers.domain.product.ProductStatus;
import com.loopers.interfaces.api.brand.BrandDto;
import lombok.Builder;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public class ProductDto {

    @Builder
    public record SearchRequest(
            // 검색 조건
            BigDecimal price,
            Long likeCount,
            ZonedDateTime createdAt,
            
            // 페이징
            Integer page,
            Integer size,
            String sort
    ) {
        public ProductCondition toCondition() {
            return new ProductCondition(
                    price,
                    likeCount,
                    createdAt,
                    sort != null ? sort : "latest"
            );
        }
    }

    @Builder
    public record ProductResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            Long likeCount,
            ProductStatus status,
            Boolean isVisible,
            Boolean isSellable,
            BrandDto.BrandResponse brand
    ) {
        public static ProductResponse from(ProductInfo productInfo) {
            return ProductResponse.builder()
                    .id(productInfo.id())
                    .name(productInfo.name())
                    .description(productInfo.description())
                    .price(productInfo.price())
                    .likeCount(productInfo.likeCount())
                    .status(productInfo.status())
                    .isVisible(productInfo.isVisible())
                    .isSellable(productInfo.isSellable())
                    .brand(productInfo.brand() != null ? BrandDto.BrandResponse.from(productInfo.brand()) : null)
                    .build();
        }
    }

    @Builder
    public record PageResponse<T>(
            List<T> content,
            Integer page,
            Integer size,
            Long totalElements,
            Integer totalPages,
            Boolean isFirst,
            Boolean isLast
    ) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return PageResponse.<T>builder()
                    .content(page.getContent())
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .isFirst(page.isFirst())
                    .isLast(page.isLast())
                    .build();
        }
    }
}
