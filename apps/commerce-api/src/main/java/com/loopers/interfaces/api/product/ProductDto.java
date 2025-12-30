package com.loopers.interfaces.api.product;

import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.view.ProductView;
import lombok.Builder;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public class ProductDto {

    @Builder
    public record SearchRequest(
            // 검색 조건
            Long brandId,
            ZonedDateTime createdAt,
            
            // 페이징
            Integer page,
            Integer size,
            String sort
    ) {
        public ProductCondition toCondition() {
            return ProductCondition.builder()
                    .brandId(brandId)
                    .createdAt(createdAt)
                    .sort(sort != null ? sort : "likes_desc")
                    .build();
        }
    }

    @Builder
    public record ProductResponse(
            Long id,
            String name,
            BigDecimal price,
            Long likeCount,
            Long brandId,
            String brandName,
            ProductStatus status,
            Long rank  // 랭킹 순위 (없으면 null)
    ) {
        public static ProductResponse from(ProductView productView) {
            return ProductResponse.builder()
                    .id(productView.getId())
                    .name(productView.getName())
                    .price(productView.getPrice())
                    .likeCount(productView.getLikeCount())
                    .brandId(productView.getBrandId())
                    .brandName(productView.getBrandName())
                    .status(productView.getStatus())
                    .rank(null)  // 기본값은 null
                    .build();
        }
        
        public static ProductResponse from(ProductView productView, Long rank) {
            return ProductResponse.builder()
                    .id(productView.getId())
                    .name(productView.getName())
                    .price(productView.getPrice())
                    .likeCount(productView.getLikeCount())
                    .brandId(productView.getBrandId())
                    .brandName(productView.getBrandName())
                    .status(productView.getStatus())
                    .rank(rank)
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
