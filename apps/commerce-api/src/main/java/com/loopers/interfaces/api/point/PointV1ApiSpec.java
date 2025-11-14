package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;

@Tag(name = "Point V1 API", description = "Loopers 예시 API 입니다.")
public interface PointV1ApiSpec {
  @Operation(
      summary = "포인트 조회",
      description = "ID로 포인트 조회합니다."
  )
  @Valid
  ApiResponse<BigDecimal> getPoint(
      @Schema(name = "사용자 ID", description = "조회할 사용자의 ID")
      @RequestHeader(value = "X-USER-ID", required = false) Long userId
  );

  @Operation(
      summary = "포인트 충전",
      description = "ID로 포인트를 충전합니다."
  )
  @Valid
  ApiResponse<BigDecimal> charge(
      @Schema(name = "사용자 ID", description = "충전할 사용자의 ID")
      @RequestHeader(value = "X-USER-ID", required = false) Long userId
      , @RequestBody BigDecimal amount
  );
}
