package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Order V1 API", description = "Order V1 API 입니다.")
public interface OrderV1ApiSpec {

    @Operation(
            summary = "주문 요청",
            description = "사용자가 주문 요청을 합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> order(
            @Schema(name = "로그인 ID", description = "주문 요청을 하는 사용자의 로그인 ID")
            String loginId,

            @Schema(name = "주문 요청 정보", description = "주문할 정보")
            OrderV1Dto.OrderRequest orderRequest
    );

    @Operation(
            summary = "유저 주문 목록 조회",
            description = "사용자의 주문 목록을 조회합니다."
    )
    ApiResponse<List<OrderV1Dto.OrderResponse>> getOrdersInfo(
            @Parameter(name = "loginId", description = "조회할 사용자의 로그인 ID", required = true)
            String loginId
    );

    @Operation(
            summary = "단일 주문 상세 조회",
            description = "주문 ID로 단일 주문의 상세 정보를 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> getOrderInfo(
            @Schema(name = "로그인 ID", description = "사용자의 로그인 ID")
            String loginId,

            @Schema(name = "주문 ID", description = "조회할 주문 ID")
            @PathVariable Long orderId
    );

}
