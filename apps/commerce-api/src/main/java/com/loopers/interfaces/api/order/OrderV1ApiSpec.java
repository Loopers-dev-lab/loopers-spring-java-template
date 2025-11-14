package com.loopers.interfaces.api.order;

import com.loopers.domain.order.Order;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Order V1 API", description = "Loopers 예시 API 입니다.")
public interface OrderV1ApiSpec {
  @Operation(
      summary = "주문 목록 조회",
      description = "사용자 ID로 주문 목록 조회합니다."
  )
  @Valid
  ApiResponse<Page<Order>> getOrderList(
      @Schema(name = "사용자 ID", description = "조회할 사용자의 ID")
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @RequestParam(name = "sortType", defaultValue = "latest") String sortType,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  );

  @Operation(
      summary = "주문 요청",
      description = "ID로 주문를 충전합니다."
  )
  @Valid
  ApiResponse<OrderCreateV1Dto.OrderResponse> createOrder(
      @Schema(name = "사용자 ID", description = "충전할 사용자의 ID")
      @RequestHeader(value = "X-USER-ID", required = false) Long userId
      , @RequestBody OrderCreateV1Dto.OrderRequest request
  );


  @Operation(
      summary = "주문 상세조회",
      description = "주문 ID로 주문 상세조회합니다."
  )
  @Valid
  ApiResponse<OrderCreateV1Dto.OrderResponse> getOrderDetail(
      @Schema(name = "사용자 ID", description = "조회할 사용자의 ID")
      @RequestHeader(value = "X-USER-ID", required = false) Long userId
      , Long orderId
  );
}
