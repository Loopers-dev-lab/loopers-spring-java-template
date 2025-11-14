package com.loopers.application.order;

import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UserOrderProductFacade {
    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public boolean preOrder(Long userPkId, List<OrderLineCommand> orderLines) {
        productService.markCurrentStockStatus(orderLines);
        UserModel userModel = userService.getUser(userPkId);
        Integer total = productService.getTotalAmountOfAvailStock(orderLines);
        return userModel.hasEnoughPoint(total);
    }

    @Transactional
    public void placeOrder(Long userPkId, List<OrderLineCommand> orderLines) {
        OrderModel order = orderService.putOrder(orderLines);
        StockDecreaseResult stockResult = decreaseAllStocks(orderLines);
        // TODO 클린 아키텍처 고려하기
        orderService.putFailStatus(order, stockResult.failedLines());
        Integer totalAmountPoint = stockResult.totalAmount();

        // 포인트 부족 시 예외 → 전체 롤백
        userService.decreaseUserPoint(userPkId, totalAmountPoint);
    }

    @Transactional
    protected StockDecreaseResult decreaseAllStocks(List<OrderLineCommand> lines) {
        List<OrderLineCommand> success = new ArrayList<>();
        List<OrderLineCommand> failed = new ArrayList<>();
        int total = 0;

        for (OrderLineCommand line : lines) {
            // TODO 엔티티 클래스에서 예외 발생시 포인트 계산 제대로 되는지 확인 필요
            boolean ok = productService.decreaseStock(line.productId(), line.quantity());
            if (ok) {
                success.add(line);
                total += productService.getPrice(line.productId(), line.quantity());
            } else {
                failed.add(line);
            }
        }

        return StockDecreaseResult.of(success, failed, total);
    }
}
