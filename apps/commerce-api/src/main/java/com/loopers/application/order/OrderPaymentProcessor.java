package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.infrastructure.dataplatform.OrderDataPlatformClient;
import com.loopers.infrastructure.payment.PgPaymentClient;
import com.loopers.infrastructure.payment.dto.PgPaymentV1Dto;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentProcessor.class);

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ProductService productService;
    private final PointService pointService;
    private final PgPaymentClient pgPaymentClient;
    private final OrderDataPlatformClient orderDataPlatformClient;

    @Transactional
    public void handlePaymentCallback(String orderReference, PgPaymentV1Dto.TransactionStatus status, String transactionKey, String reason) {
        Payment payment = paymentService.findByOrderReference(orderReference)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (payment.getTransactionKey() != null && payment.getTransactionKey().equals(transactionKey)) {
            return;
        }

        Order order = orderService.findById(payment.getOrderId());
        applyPaymentResult(order, payment, status, transactionKey, reason);
    }

    @Transactional
    public void syncPayment(Long orderId) {
        Payment payment = paymentService.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
        Order order = orderService.findById(orderId);
        ApiResponse<PgPaymentV1Dto.OrderResponse> response = pgPaymentClient.getPayments(payment.getUserId(), payment.getOrderReference());
        PgPaymentV1Dto.OrderResponse data = response != null ? response.data() : null;

        if (data == null || data.transactions() == null || data.transactions().isEmpty()) {
            log.warn("결제 내역을 찾을 수 없습니다. orderId={}, orderReference={}", orderId, payment.getOrderReference());
            return;
        }

        PgPaymentV1Dto.TransactionRecord record = data.transactions().get(data.transactions().size() - 1);

        if (payment.getTransactionKey() != null && payment.getTransactionKey().equals(record.transactionKey())) {
            log.info("이미 처리된 결제입니다. orderId={}, transactionKey={}", orderId, record.transactionKey());
            return;
        }
        applyPaymentResult(order, payment, record.status(), record.transactionKey(), record.reason());
    }

    @Transactional
    public void handlePaymentResult(Long orderId, PgPaymentV1Dto.TransactionStatus status, String transactionKey, String reason) {
        Payment payment = paymentService.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
        Order order = orderService.findById(orderId);
        applyPaymentResult(order, payment, status, transactionKey, reason);
    }

    private void applyPaymentResult(
            Order order,
            Payment payment,
            PgPaymentV1Dto.TransactionStatus status,
            String transactionKey,
            String reason
    ) {
        OrderStatus newStatus = OrderPaymentSupport.mapOrderStatus(status);
        payment.updateStatus(OrderPaymentSupport.mapPaymentStatus(status), transactionKey, reason);
        paymentService.save(payment);

        if (newStatus == OrderStatus.COMPLETE) {
            order.updateStatus(OrderStatus.COMPLETE);
        } else if (newStatus == OrderStatus.FAIL) {
            revertOrder(order, payment.getUserId());
        } else {
            order.updateStatus(OrderStatus.PENDING);
        }

        orderDataPlatformClient.send(order, payment);
    }

    private void revertOrder(Order order, String userId) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = productService.getProduct(item.getProductId());
            product.increaseStock(item.getQuantity());
        }
        Point point = pointService.findPointByUserId(userId);
        if (point != null) {
            point.refund(order.getTotalAmount());
        }
        order.updateStatus(OrderStatus.FAIL);
    }
}
