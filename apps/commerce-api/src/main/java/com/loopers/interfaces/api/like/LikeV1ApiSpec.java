package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeInfo;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Like V1 API", description = "Loopers 예시 API 입니다.")
public interface LikeV1ApiSpec {
  @Operation(
      summary = "좋아요",
      description = "좋아요 합니다."
  )
  ApiResponse<LikeInfo> like(
      @Schema(name = "사용자 ID", description = "조회할 사용자의 ID") Long userId,
      @PathVariable(value = "productId") Long productId
  );

  @Operation(
      summary = "좋아요 삭제",
      description = "좋아요를 삭제합니다."
  )
  ApiResponse<LikeInfo> unlike(
      @Schema(name = "사용자 ID", description = "조회할 사용자의 ID") Long userId,
      @PathVariable(value = "productId") Long productId
  );
}
