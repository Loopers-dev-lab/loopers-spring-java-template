package com.loopers.application.order;

import com.loopers.domain.order.OrderItemModel;
import com.loopers.domain.order.OrderItemStatus;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserOrderProductFacade {
    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public OrderResult.PreOrderResult preOrder(OrderCommand.Order orderCommand) {
        UserModel userModel = userService.getUser(orderCommand.userId());
        StockResult result = readAllStocks(orderCommand.orderLineRequests());
        if(!userModel.hasEnoughPoint(result.requiringPrice())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.");
        }

        return OrderResult.PreOrderResult.of(
                userModel.getUserId(),
                result.requiringPrice(),
                result.successLines(),
                result.failedLines()
        );
    }

    /**
     * retry-wrapper
     * @param orderCommand
     * @return
     */
    @Transactional
    public OrderResult.PlaceOrderResult placeOrderWithRetry(OrderCommand.Order orderCommand) {
        int maxRetry = 3;

        for (int i = 0; i < maxRetry; i++) {
            try {
                return placeOrder(orderCommand);
            } catch (CannotAcquireLockException e) {
                log.warn("Deadlock detected. retry={}", i + 1);
                try {
                    Thread.sleep(10L * (i + 1)); // backoff
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e2);
                }
            }
        }

        throw new CoreException(ErrorType.BAD_REQUEST, "일시적인 오류입니다. 다시 시도해주세요.");
    }

    /**
     * [요구사항] 주문 전체 흐름에 대한 원자성이 보장
     * - 재고가 존재하지 않거나 부족할 경우 주문은 실패
     * - 주문 시 유저의 포인트 잔액이 부족할 경우 주문은 실패
     * - 쿠폰, 재고, 포인트 처리 등 하나라도 작업이 실패하면 모두 롤백처리
     * - 주문 성공 시, 모든 처리는 정상 반영
     * @param orderCommand
     */
    @Transactional
    public OrderResult.PlaceOrderResult placeOrder(OrderCommand.Order orderCommand) {

        List<OrderItemModel> orderItems = toDomainOrderItem(orderCommand.orderLineRequests());
        // OrderModel orderModel = orderService.createPendingOrder(userModel, orderItems);

        UserModel userModel = userService.getUser(orderCommand.userId());
        StockResult stockResult = decreaseAllStocks(orderItems);

        Integer requiringPoints = stockResult.requiringPrice();
        // 포인트 부족 시 롤백
        if(!userModel.hasEnoughPoint(requiringPoints)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다. 다시 확인해주세요");
        }

        userService.decreaseUserPoint(userModel.getId(), requiringPoints);

        boolean hasOutOfStockCase = !stockResult.failedLines().isEmpty();
        if(hasOutOfStockCase) {
            // orderService.updateOrderAsPartialSuccess(orderModel, stockResult.requiringPrice() , stockResult.errorPrice());
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다. 다시 확인해주세요");
        }

        // orderService.updateOrderAsSuccess(orderModel, stockResult.requiringPrice());
        OrderModel orderModel = orderService.createSuccessOrder(userModel, orderItems, stockResult.requiringPrice());

        return OrderResult.PlaceOrderResult.of(
                userModel.getUserId(),
                orderModel.getId(),
                stockResult.requiringPrice(),
                stockResult.errorPrice(),
                stockResult.successLines(),
                stockResult.failedLines()
        );
    }

    @Transactional
    protected StockResult decreaseAllStocks(List<OrderItemModel> items) {
        List<OrderCommand.OrderLine> success = new ArrayList<>();
        List<OrderCommand.OrderLine> failed = new ArrayList<>();
        int total = 0, fail = 0;

        /* productId 순서로 정렬 - productService 내부에서 다시 조회하거나 flush 타이밍에 UPDATE가 걸릴 수도 있음.*/
        /*List<OrderItemModel> sortedItems = items.stream()
                .sorted(Comparator.comparing(o -> o.getProduct().getId()))
                .toList();
        List<Long> productIds = sortedItems.stream()
                .map(i -> i.getProduct().getId())
                .toList();*/

        for (OrderItemModel item : items) {
            ProductModel p = item.getProduct();
            boolean ok = productService.decreaseStock(p.getId(), item.getQuantity());
            OrderCommand.OrderLine line = OrderCommand.OrderLine.from(item);
            int requiringPoint = productService.getPrice(p.getId(), item.getQuantity());
            if (ok) {
                item.setStatus(OrderItemStatus.SUCCESS);
                total += requiringPoint;
                success.add(line);
            } else {
                item.setStatus(OrderItemStatus.FAILED);
                fail += requiringPoint;
                failed.add(line);
            }
        }

        return StockResult.of(success, failed, total, fail);
    }

    protected StockResult readAllStocks(List<OrderCommand.OrderLine> lines) {
        List<OrderCommand.OrderLine> success = new ArrayList<>();
        List<OrderCommand.OrderLine> failed = new ArrayList<>();
        int requiringPrice = 0, errorPrice = 0;

        for (OrderCommand.OrderLine line : lines) {
            // TODO 엔티티 클래스에서 예외 발생시 포인트 계산 제대로 되는지 확인 필요
            boolean ok = productService.hasStock(line.productId(), line.quantity());
            int point = productService.getPrice(line.productId(), line.quantity());
            if (ok) {
                success.add(line);
                requiringPrice += point;
            } else {
                failed.add(line);
                errorPrice += point;
            }
        }
        return StockResult.of(success, failed, requiringPrice, errorPrice);
    }


    public List<OrderItemModel> toDomainOrderItem(List<OrderCommand.OrderLine> lines) {
        return lines.stream()
                .map(l -> {
                    ProductModel p = productService.getProductDetail(l.productId());
                    return OrderItemModel.of(p, l.quantity());
                })
                .toList();

    }
}
