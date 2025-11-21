package com.loopers.interfaces.api.like.product;

import com.loopers.application.like.product.LikeProductInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

public class LikeProductV1Dto {
    public record ProductResponse(
            Long id,
            String name,
            String brand,
            int price,
            int likes,
            int stock
    ) {
        public static LikeProductV1Dto.ProductResponse from(LikeProductInfo info) {
            return new LikeProductV1Dto.ProductResponse(
                    info.id(),
                    info.name(),
                    info.brand(),
                    info.price(),
                    info.likes(),
                    info.stock()
            );
        }
    }

    public record ProductsResponse(
            List<LikeProductV1Dto.ProductResponse> content,
            int totalPages,
            long totalElements,
            int number,
            int size

    ) {
        public static LikeProductV1Dto.ProductsResponse from(Page<LikeProductInfo> page) {
            return new LikeProductV1Dto.ProductsResponse(
                    page.map(LikeProductV1Dto.ProductResponse::from).getContent(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getNumber(),
                    page.getSize()
            );
        }
    }
}
