package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Order V1 API", description = "주문 관련 API 입니다.")
public interface OrderV1ApiSpec {

  @Operation(
      summary = "주문 생성",
      description = "사용자가 상품을 주문합니다."
  )
  ApiResponse<OrderV1Dto.OrderResponse> createOrder(
      @Schema(name = "사용자 ID", description = "주문하는 사용자의 ID")
      @RequestHeader("X-USER-ID") Long userId,

      @Schema(name = "주문 생성 요청", description = "주문할 상품 정보")
      @RequestBody OrderV1Dto.OrderRequest request
  );
}
