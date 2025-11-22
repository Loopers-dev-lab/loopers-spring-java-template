package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderDto.OrderCreateRequest;
import com.loopers.interfaces.api.order.OrderDto.OrderDetailResponse;
import com.loopers.interfaces.api.order.OrderDto.OrderListResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Order API", description = "주문 API입니다.")
public interface OrderApiSpec {

  @Operation(
      summary = "주문 생성",
      description = "여러 상품을 한 번에 주문하고 포인트로 결제합니다."
  )
  ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(ApiHeaders.USER_ID) Long userId,

      @RequestBody(description = "주문 생성 요청", required = true)
      OrderCreateRequest request
  );

  @Operation(
      summary = "주문 목록 조회",
      description = "사용자의 주문 목록을 조회합니다. 페이지네이션을 지원합니다."
  )
  ApiResponse<OrderListResponse> retrieveOrders(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(ApiHeaders.USER_ID) Long userId,

      @Parameter(description = "페이지 번호 (0부터 시작)", required = false)
      @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "페이지당 주문 수", required = false)
      @RequestParam(defaultValue = "20") int size
  );

  @Operation(
      summary = "주문 상세 조회",
      description = "주문 ID로 주문 상세 정보를 조회합니다. 본인의 주문만 조회할 수 있습니다."
  )
  ApiResponse<OrderDetailResponse> retrieveOrderDetail(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(ApiHeaders.USER_ID) Long userId,

      @Parameter(description = "주문 ID", required = true)
      @PathVariable("orderId") Long orderId
  );
}
