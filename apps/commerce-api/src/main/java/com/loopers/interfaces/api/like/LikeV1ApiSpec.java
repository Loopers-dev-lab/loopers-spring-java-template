package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeInfo;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Like V1 API", description = "Loopers 예시 API 입니다.")
public interface LikeV1ApiSpec {
  @Operation(
      summary = "좋아요",
      description = "좋아요 합니다."
  )
  ApiResponse<LikeInfo> like(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @PathVariable(value = "productId") Long productId
  );

  @Operation(
      summary = "좋아요 삭제",
      description = "좋아요를 삭제합니다."
  )
  ApiResponse<LikeInfo> unlike(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @PathVariable(value = "productId") Long productId
  );
}
