package com.loopers.application.payment;

import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentApproveInfo;
import com.loopers.domain.payment.PaymentApproveResponse;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class PaymentFacade {

    private static final String MALL_NAME = "LOOPERS_MALL";
    private static final String PREFIX_PAYMENT_ID_KEY = "tx_id_";
    private static final String PREFIX_CALLBACK_URL = "http://localhost:8080/api/v1/approve";
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final PaymentGateway paymentGateway;

    @Transactional
    public void paidPayment(Long userId, Long paymentId) {
        User user = userService.getUser(userId);
        Payment payment = paymentService.getPendingPayment(paymentId);

        PaymentApproveResponse response = approvePayment(payment);

//        Order order = orderService.getPendingOrder(userId, payment.getOrderId());
//        List<OrderItem> orderItems = orderService.getOrderItemsByOrderId(order.getId());
//
//        List<Long> productIdList = orderItems.stream()
//                .map(OrderItem::getProductId)
//                .toList();
//
//        List<Product> products = productService.getProductListWithLock(productIdList);
//
//        Map<Long, Integer> itemQuantityMap = orderItems.stream()
//                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));
//
//        products.forEach(product -> {
//            int quantity = itemQuantityMap.get(product.getId());
//            productCacheService.deleteDetailCache(product.getId());
//            product.decreaseStock(quantity);
//        });
//
//        Long totalPrice = orderItems.stream()
//                .mapToLong(item -> item.getProductPrice() * item.getQuantity())
//                .sum();
//
//        Point point = pointService.getPointByUserIdWithLock(userId);
//        point.usePoint(totalPrice);
    }

    private PaymentApproveResponse approvePayment(Payment payment) {
        PaymentApproveInfo paymentApproveInfo = PaymentApproveInfo.from(PREFIX_PAYMENT_ID_KEY, PREFIX_CALLBACK_URL, payment);
        return paymentGateway.approvePayment(MALL_NAME, paymentApproveInfo);
    }
}
