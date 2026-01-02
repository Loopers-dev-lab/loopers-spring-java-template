package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.ProductRankingService;
import com.loopers.domain.product.view.ProductView;
import jakarta.validation.constraints.Max;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public class RankingDto {

    /**
     * 랭킹 조회 요청 (통합)
     */
    public record SearchRequest(
        Integer page,
        @Max(value = 100, message = "size는 최대 100까지 가능합니다")
        Integer size
    ) {}

    /**
     * 랭킹 응답
     */
    public record Response(
        Long rank,
        Long productId,
        ProductInfo productInfo
    ) {
        public static Response from(ProductRankingService.RankingItem item) {
            return new Response(
                item.rank(),
                item.productId(),
                ProductInfo.from(item.productView())
            );
        }
    }

    /**
     * 상품 정보
     */
    public record ProductInfo(
        Long id,
        String name,
        Long price,
        Long likeCount,
        Long brandId,
        String brandName
    ) {
        public static ProductInfo from(ProductView productView) {
            return new ProductInfo(
                productView.getId(),
                productView.getName(),
                productView.getPrice().longValue(),
                productView.getLikeCount(),
                productView.getBrandId(),
                productView.getBrandName()
            );
        }
    }

    /**
     * 페이지 응답
     */
    public record PageResponse<T>(
        String rankingType,
        LocalDateTime snapshotTime,
        List<T> content,
        PageInfo page
    ) {
        public static <T> PageResponse<T> from(String rankingType, LocalDateTime snapshotTime, Page<T> page) {
            return new PageResponse<>(
                rankingType,
                snapshotTime,
                page.getContent(),
                new PageInfo(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
                )
            );
        }
    }

    /**
     * 페이지 정보
     */
    public record PageInfo(
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages
    ) {}
}



