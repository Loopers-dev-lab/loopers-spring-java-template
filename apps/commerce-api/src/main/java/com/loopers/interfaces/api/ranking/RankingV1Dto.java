package com.loopers.interfaces.api.ranking;

import com.loopers.application.product.ProductInfo;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.domain.ranking.PeriodType;
import com.loopers.domain.ranking.RankingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.stream.Collectors;

public class RankingV1Dto {

    public record GetTopRankingRequest(
            @Schema(description = "랭킹 타입", example = "ALL", allowableValues = {"LIKE", "VIEW", "ORDER", "ALL"})
            RankingType type,

            @Schema(description = "랭킹 조회 타입", example = "DAILY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY"})
            PeriodType periodType,

            @Schema(description = "조회 날짜 (yyyyMMdd)", example = "20251225")
            String date,

            @Min(1)
            @Max(100)
            @Schema(description = "조회할 개수", example = "10", defaultValue = "10")
            Integer limit
    ) {
        public GetTopRankingRequest {
            if (limit == null) {
                limit = 10;
            }
        }
    }

    public record GetRankingWithPagingRequest(
            @Schema(description = "랭킹 타입", example = "ALL")
            RankingType type,

            @Schema(description = "랭킹 조회 타입", example = "DAILY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY"})
            PeriodType periodType,

            @Schema(description = "조회 날짜 (yyyyMMdd)", example = "20251225")
            String date,

            @Min(0)
            @Schema(description = "페이지 번호 (0-based)", example = "0", defaultValue = "0")
            Integer page,

            @Min(1)
            @Max(100)
            @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
            Integer size
    ) {
        public GetRankingWithPagingRequest {
            if (page == null) {
                page = 0;
            }
            if (size == null) {
                size = 20;
            }
        }
    }

    public record RankingItem(
            @Schema(description = "순위", example = "1")
            Integer rank,

            @Schema(description = "상품 ID", example = "101")
            Long productId,

            @Schema(description = "상품 정보")
            ProductInfo product,  // 👈 추가

            @Schema(description = "랭킹 점수", example = "125.5")
            Double score
    ) {
        public static RankingItem from(RankingInfo info) {
            return new RankingItem(
                    info.rank(),
                    info.productId(),
                    info.product(),
                    info.score()
            );
        }
    }

    public record TopRankingResponse(
            @Schema(description = "랭킹 리스트")
            List<RankingItem> rankings,

            @Schema(description = "전체 랭킹 개수", example = "150")
            Long totalCount
    ) {
        public static TopRankingResponse of(List<RankingInfo> infos, long totalCount) {
            return new TopRankingResponse(
                    infos.stream()
                            .map(RankingItem::from)
                            .collect(Collectors.toList()),
                    totalCount
            );
        }
    }

    public record PagingRankingResponse(
            @Schema(description = "랭킹 리스트")
            List<RankingItem> rankings,

            @Schema(description = "현재 페이지", example = "0")
            Integer currentPage,

            @Schema(description = "페이지 크기", example = "20")
            Integer pageSize,

            @Schema(description = "전체 랭킹 개수", example = "150")
            Long totalCount,

            @Schema(description = "전체 페이지 수", example = "8")
            Integer totalPages
    ) {
        public static PagingRankingResponse of(List<RankingInfo> infos, int page, int size, long totalCount) {
            return new PagingRankingResponse(
                    infos.stream()
                            .map(RankingItem::from)
                            .collect(Collectors.toList()),
                    page,
                    size,
                    totalCount,
                    (int) Math.ceil((double) totalCount / size)
            );
        }
    }

    public record ProductRankingResponse(
            @Schema(description = "상품 ID", example = "101")
            Long productId,

            @Schema(description = "상품 정보")
            ProductInfo product,

            @Schema(description = "순위", example = "5")
            Integer rank,

            @Schema(description = "랭킹 점수", example = "88.5")
            Double score
    ) {
        public static ProductRankingResponse from(RankingInfo info) {
            return new ProductRankingResponse(
                    info.productId(),
                    info.product(),
                    info.rank(),
                    info.score()
            );
        }
    }
}
