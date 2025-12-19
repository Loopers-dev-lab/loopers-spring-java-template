package com.loopers.application.payment;

import com.loopers.application.product.ProductCacheService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentApproveInfo;
import com.loopers.domain.payment.PaymentApproveResponse;
import com.loopers.domain.payment.PaymentApproveResponse.Meta.Result;
import com.loopers.domain.payment.PaymentEvent.PaymentPaid;
import com.loopers.domain.payment.PaymentEvent.PaymentFailed;
import com.loopers.domain.payment.PaymentEventPublisher;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.TransactionStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class PaymentFacade {

    private static final String MALL_NAME = "LOOPERS_MALL";
    private static final String PREFIX_PAYMENT_ID_KEY = "tx_id_";
    private static final String PREFIX_CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback";
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final PaymentGateway paymentGateway;
    private final ProductCacheService productCacheService;
    private final PointService pointService;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public PaymentInfo requestPaidPayment(Long userId, Long paymentId) {
        User user = userService.getUser(userId);
        Payment payment = paymentService.getPendingPayment(paymentId);

        try {
            PaymentApproveResponse response = approvePayment(payment);
            if (response.meta().result() == Result.SUCCESS) {
                payment.success(response.data().transactionKey());
            } else {
                log.error("Payment failed, paymentId={}, response={}", paymentId, response);
                paymentFail(payment, response.data().reason());
            }
        } catch (Exception e) {
            log.error("Payment failed, paymentId={}", paymentId, e);
            paymentFail(payment, e.getMessage());
            throw new CoreException(ErrorType.INTERNAL_ERROR);
        }
        return PaymentInfo.from(payment);
    }

    @Transactional
    public void callback(final TransactionInfo transactionInfo) {
        String paymentId = transactionInfo.transactionKey().substring(PREFIX_PAYMENT_ID_KEY.length());
        Payment payment = paymentService.findById(Long.parseLong(paymentId));

        if (transactionInfo.status() == TransactionStatus.FAILED) {
            paymentFail(payment, transactionInfo.reason());
            payment.fail("PG 승인 중 오류 발생: " + transactionInfo.reason());
        }
        if (transactionInfo.status().equals(TransactionStatus.SUCCESS)) {
            paymentSuccess(payment);
        }
    }

    private PaymentApproveResponse approvePayment(Payment payment) {
        PaymentApproveInfo paymentApproveInfo = PaymentApproveInfo.from(PREFIX_PAYMENT_ID_KEY, PREFIX_CALLBACK_URL, payment);
        return paymentGateway.approvePayment(MALL_NAME, paymentApproveInfo);
    }

    private void paymentSuccess(final Payment payment) {
        payment.paid();
        paymentEventPublisher.publish(PaymentPaid.from(payment));
        orderService.paid(payment.getOrderId());
        Order order = orderService.findById(payment.getOrderId());
        List<OrderItem> orderItems = orderService.getOrderItemsByOrderId(order.getId());

        List<Long> productIdList = orderItems.stream()
                .map(OrderItem::getProductId)
                .toList();

        List<Product> products = productService.getProductListWithLock(productIdList);

        Map<Long, Integer> itemQuantityMap = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));

        products.forEach(product -> {
            int quantity = itemQuantityMap.get(product.getId());
            productCacheService.deleteDetailCache(product.getId());
            product.decreaseStock(quantity);
        });

        Long totalPrice = orderItems.stream()
                .mapToLong(item -> item.getProductPrice() * item.getQuantity())
                .sum();

        Point point = pointService.getPointByUserIdWithLock(order.getUserId());
        point.usePoint(totalPrice);
    }
    private void paymentFail(final Payment payment, final String message) {
        payment.fail("PG 승인 중 오류 발생: " + message);
        paymentEventPublisher.publish(PaymentFailed.from(payment));
        orderService.fail(payment.getOrderId());
    }
}
