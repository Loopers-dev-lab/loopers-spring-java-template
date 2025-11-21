package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderLineCommand;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping("/orders")
    public ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @Valid @RequestBody OrderV1Dto.PlaceOrderRequest request,
            @RequestHeader("X-USER-ID") String memberId
    ) {
        List<OrderLineCommand> orderLines = request.orderLines().stream()
                .map(line -> OrderLineCommand.of(line.productId(), line.quantity()))
                .toList();

        OrderCommand command = OrderCommand.of(memberId, orderLines);
        OrderInfo orderInfo = orderFacade.placeOrder(command);
        
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);
        return ApiResponse.success(response);
    }
}