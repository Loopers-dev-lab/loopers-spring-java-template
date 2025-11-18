package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderResult;

import java.util.List;

public class OrderV1Dto {

    /**
     * 유저의 주문에 대한 재고, 포인트 사전검증/주문 실행시 요청 DTO 입니다.
     * @param userId
     * @param orderLineRequests
     */
    public record OrderRequest(
            String userId,
            List<OrderLineRequest> orderLineRequests
    ) {

        /**
         * interfaces 레이어의 DTO에서 application 레이어의 DTO로 변환합니다.
         * @return
         */
        public OrderCommand.Order toCommand() {
            List<OrderCommand.OrderLine> lineCommands = orderLineRequests.stream()
                    .map(OrderLineRequest::toCommand)
                    .toList();

            return new OrderCommand.Order(
                    userId,
                    lineCommands
            );
        }
    }


    public record OrderResponse(
            String userId, // 이용자ID
            Integer normalPrice,
            Integer errorPrice,
            Long orderId, // 실 주문의 경우에만 값 존재
            String status, // 주문 상태
            int successCount,
            int failureCount,
            List<OrderLineResponse> successLines, // 정상 주문 가능한 라인
            List<OrderLineResponse> failedLines // 재고 부족 등의 이유로 실패한 라인
    ) {

        /**
         * 사전 검증(preOrder)용 응답 팩토리
         */
        public static OrderResponse fromPreOrder(
                OrderResult.PreOrderResult preOrderResult
        ) {

            List<OrderLineResponse> successLines = preOrderResult.successLines()
                    .stream()
                    .map(OrderLineResponse::from)
                    .toList();
            List<OrderLineResponse> failedLines = preOrderResult.successLines()
                    .stream()
                    .map(OrderLineResponse::from)
                    .toList();

            return new OrderResponse(
                    preOrderResult.userId(),
                    preOrderResult.requiringPoints(),
                    null,
                    null,          // 선조회 시점에는 orderId 없음
                    "PRE_CHECK",   // 상태 구분
                    preOrderResult.successLines().size(),
                    preOrderResult.failedLines().size(),
                    successLines,      // 실패 라인 상세는 아직 없음
                    failedLines      // 실패 라인 상세는 아직 없음
            );
        }

        /**
         * 주문 실행(placeOrder)용 응답 팩토리
         */
        public static OrderResponse fromOrderPlacement(
                OrderResult.PlaceOrderResult placeOrderResult
        ) {
            List<OrderLineResponse> successLines = placeOrderResult.successLines()
                    .stream()
                    .map(OrderLineResponse::from)
                    .toList();
            List<OrderLineResponse> failedLines = placeOrderResult.successLines()
                    .stream()
                    .map(OrderLineResponse::from)
                    .toList();

            return new OrderResponse(
                    placeOrderResult.userId(),
                    placeOrderResult.normalPrice(),  // 실제 사용 포인트
                    placeOrderResult.errorPrice(),
                    placeOrderResult.orderId(),                          // 여기서는 포인트 부족이면 예외로 롤백 처리했다고 가정
                    "ORDERED",
                    successLines.size(),
                    failedLines.size(),
                    successLines,
                    failedLines
            );
        }

    }

    /**
     * interfaces layerd에서 정의하는 개별 주문 라인 DTO
     * @param productId
     * @param quantity
     */
    public record OrderLineRequest(
            Long productId,
            Integer quantity
    ) {
        public OrderCommand.OrderLine toCommand() {
            return new OrderCommand.OrderLine(productId, quantity);
        }
    }

    /**
     * application 레이어에서 interfaces 레이어로 응답할 때 사용
     * - API 응답으로 이 타입을 그대로 노출하면 외부에 내부 모델을 노출하게 되는 위험이 있음
     * - 예컨대 내부 구현이 바뀌면 외부 스펙이 의도치 않게 바뀔 수 있고, 외부 클라이언트에 영향이 있을 수 있다.
     * - (의존 방향이 깨질 수 있음)
     *
     * @param productId
     * @param quantity
     */
    public record OrderLineResponse(
            Long productId,
            Integer quantity
    ) {
        public static OrderLineResponse from(OrderCommand.OrderLine orderLine) {
            return new OrderLineResponse(orderLine.productId(), orderLine.quantity());
        }
    }
}
