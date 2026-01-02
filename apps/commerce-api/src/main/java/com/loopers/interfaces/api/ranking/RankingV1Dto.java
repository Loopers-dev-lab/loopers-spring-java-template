package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 랭킹 API DTO
 */
public class RankingV1Dto {

    /**
     * 랭킹 페이지 응답
     */
    @Schema(description = "랭킹 페이지 응답")
    public record RankingsPageResponse(
            @Schema(description = "랭킹 목록")
            List<RankingItem> items,
            @Schema(description = "현재 페이지 번호")
            int page,
            @Schema(description = "페이지 크기")
            int size,
            @Schema(description = "전체 랭킹 수")
            int total
    ) {
        public static RankingsPageResponse from(RankingInfo.RankingsPageResponse info) {
            return new RankingsPageResponse(
                    info.items().stream()
                            .map(RankingItem::from)
                            .toList(),
                    info.page(),
                    info.size(),
                    info.total()
            );
        }
    }

    /**
     * 랭킹 항목
     */
    @Schema(description = "랭킹 항목")
    public record RankingItem(
            @Schema(description = "순위 (1부터 시작)")
            Long rank,
            @Schema(description = "상품 ID")
            Long productId,
            @Schema(description = "랭킹 점수")
            Double score,
            @Schema(description = "상품명")
            String productName,
            @Schema(description = "브랜드 ID")
            Long brandId,
            @Schema(description = "가격")
            Integer price
    ) {
        public static RankingItem from(RankingInfo.RankingItem info) {
            return new RankingItem(
                    info.rank(),
                    info.productId(),
                    info.score(),
                    info.productName(),
                    info.brandId(),
                    info.price()
            );
        }
    }

    /**
     * 상품 랭킹 정보 (상세 조회용)
     */
    @Schema(description = "상품 랭킹 정보")
    public record ProductRankingInfo(
            @Schema(description = "순위 (null이면 랭킹에 없음)")
            Long rank,
            @Schema(description = "랭킹 점수")
            Double score
    ) {
        public static ProductRankingInfo from(RankingInfo.ProductRankingInfo info) {
            if (info == null) {
                return null;
            }
            return new ProductRankingInfo(info.rank(), info.score());
        }
    }
}

