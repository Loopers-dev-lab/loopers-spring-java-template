package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.user.UserCreateV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;

@Tag(name = "Point V1 API", description = "Loopers 예시 API 입니다.")
public interface PointV1ApiSpec {
  @Operation(
      summary = "예시 조회",
      description = "ID로 예시를 조회합니다."
  )
  @Valid
  ApiResponse<BigDecimal> getPoint(
      @Schema(name = "예시 ID", description = "조회할 예시의 ID")
      @RequestHeader(value = "X-USER-ID", required = false) String userId
  );
}
