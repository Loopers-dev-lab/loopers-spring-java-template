package com.loopers.interfaces.api.purchasing;

import com.loopers.application.purchasing.OrderInfo;
import com.loopers.application.purchasing.PurchasingFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 주문 API v1 컨트롤러.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class PurchasingV1Controller {

    private final PurchasingFacade purchasingFacade;

    /**
     * 주문을 생성한다.
     *
     * @param userId  X-USER-ID 헤더
     * @param request 주문 생성 요청
     * @return 생성된 주문 정보
     */
    @PostMapping
    public ApiResponse<PurchasingV1Dto.OrderResponse> createOrder(
        @RequestHeader("X-USER-ID") String userId,
        @Valid @RequestBody PurchasingV1Dto.CreateRequest request
    ) {
        OrderInfo orderInfo = purchasingFacade.createOrder(userId, request.toCommands());
        return ApiResponse.success(PurchasingV1Dto.OrderResponse.from(orderInfo));
    }

    /**
     * 현재 사용자의 주문 목록을 조회한다.
     *
     * @param userId X-USER-ID 헤더
     * @return 주문 목록
     */
    @GetMapping
    public ApiResponse<PurchasingV1Dto.OrdersResponse> getOrders(
        @RequestHeader("X-USER-ID") String userId
    ) {
        List<OrderInfo> orderInfos = purchasingFacade.getOrders(userId);
        return ApiResponse.success(PurchasingV1Dto.OrdersResponse.from(orderInfos));
    }

    /**
     * 현재 사용자의 단일 주문을 조회한다.
     *
     * @param userId  X-USER-ID 헤더
     * @param orderId 주문 ID
     * @return 주문 상세 정보
     */
    @GetMapping("/{orderId}")
    public ApiResponse<PurchasingV1Dto.OrderResponse> getOrder(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = purchasingFacade.getOrder(userId, orderId);
        return ApiResponse.success(PurchasingV1Dto.OrderResponse.from(orderInfo));
    }
}


