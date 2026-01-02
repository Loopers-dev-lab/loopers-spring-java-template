package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import org.springframework.data.domain.Page;

import java.util.List;

public class ProductV1Dto {
    public record ProductCreateRequest(
            String name,
            Long brandId,
            Integer price,
            Integer stock
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            String brand,
            int price,
            int likes,
            int stock,
            RankingInfo rankingInfo
    ) {
        public static ProductResponse from(ProductInfo info, RankingInfo rankingInfo) {
            return new ProductResponse(
                    info.id(),
                    info.name(),
                    info.brand(),
                    info.price(),
                    info.likes(),
                    info.stock(),
                    rankingInfo
            );
        }
        
        public static ProductResponse from(ProductInfo info) {
            return from(info, null);
        }
    }
    
    /**
     * 상품 랭킹 정보
     */
    public record RankingInfo(
            Long rank,
            Double score
    ) {
        public static RankingInfo from(com.loopers.application.ranking.RankingInfo.ProductRankingInfo info) {
            if (info == null) {
                return null;
            }
            return new RankingInfo(info.rank(), info.score());
        }
    }

    public record ProductsPageResponse(
            List<ProductResponse> content,
            int totalPages,
            long totalElements,
            int number,
            int size
    ) {
        public static ProductsPageResponse from(Page<ProductInfo> page) {
            return new ProductsPageResponse(
                    page.map(ProductResponse::from).getContent(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getNumber(),
                    page.getNumberOfElements()
            );
        }
    }
}
