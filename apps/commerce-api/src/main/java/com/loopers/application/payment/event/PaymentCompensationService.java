package com.loopers.application.payment.event;

import com.loopers.application.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 실패 시 보상 트랜잭션을 처리하는 서비스
 * - 재고 복구
 * - 쿠폰 복구
 * - 포인트 환불
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompensationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CouponService couponService;
    private final PointService pointService;

    /**
     * 결제 실패 시 보상 트랜잭션 실행
     * - 독립적인 트랜잭션으로 실행
     * - 부분 실패 허용 (일부 보상이 실패해도 나머지는 계속 진행)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(Long orderId, String userId) {
        log.info("보상 트랜잭션 시작 - orderId: {}, userId: {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        int successCount = 0;
        int failureCount = 0;

        // 1. 재고 복구
        try {
            restoreStock(order);
            successCount++;
            log.info("재고 복구 완료 - orderId: {}", orderId);
        } catch (Exception e) {
            failureCount++;
            log.error("재고 복구 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
        }

        // 2. 쿠폰 복구
        if (order.getUserCouponId() != null) {
            try {
                restoreCoupon(order.getUserCouponId());
                successCount++;
                log.info("쿠폰 복구 완료 - orderId: {}, userCouponId: {}",
                    orderId, order.getUserCouponId());
            } catch (Exception e) {
                failureCount++;
                log.error("쿠폰 복구 실패 - orderId: {}, userCouponId: {}, error: {}",
                    orderId, order.getUserCouponId(), e.getMessage(), e);
            }
        }

        // 3. 포인트 환불
        try {
            refundPoint(userId, order.getFinalAmount());
            successCount++;
            log.info("포인트 환불 완료 - orderId: {}, userId: {}, amount: {}",
                orderId, userId, order.getFinalAmount());
        } catch (Exception e) {
            failureCount++;
            log.error("포인트 환불 실패 - orderId: {}, userId: {}, error: {}",
                orderId, userId, e.getMessage(), e);
        }

        log.info("보상 트랜잭션 완료 - orderId: {}, 성공: {}, 실패: {}",
            orderId, successCount, failureCount);

        if (failureCount > 0) {
            log.error("⚠️ 보상 트랜잭션 부분 실패 - orderId: {}, 관리자 확인 필요!", orderId);
        }
    }

    /**
     * 재고 복구
     */
    private void restoreStock(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = productRepository.findById(orderItem.getProductId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                    "상품을 찾을 수 없습니다 - productId: " + orderItem.getProductId()));

            product.restoreStock(orderItem.getQuantity());
            productRepository.save(product);

            log.debug("상품 재고 복구 - productId: {}, quantity: {}",
                orderItem.getProductId(), orderItem.getQuantity());
        }
    }

    /**
     * 쿠폰 복구
     */
    private void restoreCoupon(Long userCouponId) {
        couponService.restoreCoupon(userCouponId);
        log.debug("쿠폰 복구 - userCouponId: {}", userCouponId);
    }

    /**
     * 포인트 환불
     */
    private void refundPoint(String userId, BigDecimal amount) {
        pointService.refundPoint(userId, amount);
        log.debug("포인트 환불 - userId: {}, amount: {}", userId, amount);
    }
}
