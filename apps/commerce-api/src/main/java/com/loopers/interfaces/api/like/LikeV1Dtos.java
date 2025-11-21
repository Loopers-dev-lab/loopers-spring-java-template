package com.loopers.interfaces.api.like;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;

import com.loopers.application.like.LikeInfo;

public class LikeV1Dtos {

    @Schema(description = "좋아요 등록 응답")
    public record LikeResponse(
            @Schema(description = "사용자명", example = "testuser")
            String username,

            @Schema(description = "상품 ID", example = "1")
            Long productId,

            @Schema(description = "상품명", example = "나이키 에어맥스")
            String productName,

            @Schema(description = "등록 일시")
            ZonedDateTime createdAt
    ) {
        public static LikeResponse from(LikeInfo likeInfo) {
            return new LikeResponse(
                    likeInfo.username(),
                    likeInfo.productId(),
                    likeInfo.productName(),
                    likeInfo.createdAt()
            );
        }
    }
}

