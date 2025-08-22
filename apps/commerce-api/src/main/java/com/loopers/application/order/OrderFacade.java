package com.loopers.application.order;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponService;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentEvent;
import com.loopers.domain.payment.PaymentEventPublisher;
import com.loopers.domain.points.PointsService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductOptionModel;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class OrderFacade {
    private final UserFacade userFacade;
    private final ProductFacade productFacde;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final CouponFacade couponFacade;
    private final CouponService couponService;
    private final ProductService productFacade;
    private final PointsService pointsService;
    private final PaymentEventPublisher paymentEventPublisher;

    public OrderFacade(UserFacade userFacade, ProductFacade productFacde, OrderService orderService,
                       OrderRepository orderRepository, CouponFacade couponFacade, CouponService couponService,
                       ProductService productService, PointsService pointsService, PaymentEventPublisher paymentEventPublisher) {
        this.userFacade = userFacade;
        this.productFacde = productFacde;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.couponFacade = couponFacade;
        this.couponService = couponService;
        this.productFacade = productService;
        this.pointsService = pointsService;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional(rollbackFor = {CoreException.class, Exception.class})
    public OrderInfo.OrderItem createOrder(OrderCommand.Request.Create request) {

        UserCommand.UserResponse user = Optional.ofNullable(userFacade.getUserById(request.userId()))
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 사용자 입니다."));
        
        OrderModel orderModel = orderService.createOrderWithRetry(user.userId());
        OrderModel currentOrder = orderRepository.save(orderModel);
        log.info("주문 생성 완료. Order ID: {}", currentOrder.getId());

        for (OrderCommand.Request.Create.OrderItem orderItem : request.orderItems()) {
            ProductModel productModel = getProductModelById(orderItem.productId());
            ProductOptionModel option = getProductOptionModel(orderItem.optionId());
            
            if (orderItem.quantity() <= 0){
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다.");
            }

            productFacde.decreaseStock(orderItem.productId(), BigDecimal.valueOf(orderItem.quantity()));

            OrderCommand.OrderItemData orderItemData =
                    orderService.processOrderItem(orderItem.quantity(), productModel, option);

            currentOrder.addItem(
                    orderItemData.productId(),
                    orderItemData.optionId(),
                    orderItemData.quantity(),
                    orderItemData.totalPrice(),
                    orderItemData.productName(),
                    orderItem.optionName(),
                    orderItemData.imageUrl()
            );
        }
        
        currentOrder = orderRepository.save(currentOrder);
        log.info("주문 아이템 저장 완료. 총 {}개 아이템", currentOrder.getOrderItems().size());
        
        if (request.couponId() != null) {
            currentOrder = applyCouponToOrder(currentOrder, request.couponId(), request.userId());
            log.info("쿠폰 적용 완료. 최종 금액: {}", currentOrder.getTotalPrice().getValue());
        }

        return processPayment(currentOrder, request);
    }

    private OrderModel applyCouponToOrder(OrderModel order, Long couponId, Long userId) {
        CouponModel couponModel = couponService.getUserCoupons(couponId, userId);;

        applyCouponByType(order, couponModel);

        return orderRepository.save(order);
    }

    private void applyCouponByType(OrderModel order, CouponModel couponModel) {
        try {
            if (couponModel.getType().isFixed()) {
                order.applyFixedCoupon(couponModel);
            } else if (couponModel.getType().isRate()) {
                order.applyRateCoupon(couponModel);
            } else {
                throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 쿠폰 타입입니다.");
            }
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, e.getMessage());
        }
    }

    private OrderInfo.OrderItem processPayment(OrderModel orderModel, OrderCommand.Request.Create request) {
        if ("point".equals(request.payType())) {
            return processPointPayment(orderModel, request.userId());
        } else {
            paymentEventPublisher.processPaymentRequest(
                    PaymentEvent.CardPaymentCreated.from(
                            orderModel.getId(),
                            "SAMSUNG",
                            request.cardNumber(),
                            orderModel.getTotalPrice().getValue()
                    ));
            OrderModel resultOrder = orderRepository.save(orderModel);
            return convertToOrderItem(resultOrder);
        }
    }

    private OrderInfo.OrderItem processPointPayment(OrderModel order, Long userId) {
        BigDecimal orderTotal = order.calculateTotal();
        
        if (!pointsService.hasEnoughPoints(userId, orderTotal)) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "포인트가 부족합니다. 필요: " + orderTotal + ", 보유: " + pointsService.getPointBalance(userId));
        }

        pointsService.deductPoints(userId, orderTotal);
        OrderModel resultOrder = orderRepository.save(order);
        
        return convertToOrderItem(resultOrder);
    }
    private ProductModel getProductModelById(Long productModelId) {
            return productFacde.getProductModelById(productModelId);
    }

    private ProductOptionModel getProductOptionModel(Long optionId) {
            return productFacde.getProductOptionByOptionId(optionId);
    }

    public OrderInfo.ListResponse getOrderList(OrderCommand.Request.GetList request) {
        Optional.ofNullable(userFacade.getUserById(request.userId()))
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다."));
        
        if (request.page() < 0 || request.size() <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "페이지 정보가 유효하지 않습니다.");
        }
        
        Pageable pageable = PageRequest.of(request.page(), request.size());
        
        Page<OrderModel> orderPage;
        if (request.status() != null && !request.status().trim().isEmpty()) {
            orderPage = orderRepository.findByUserIdAndStatus(request.userId(), request.status(), pageable);
        } else {
            orderPage = orderRepository.findByUserId(request.userId(), pageable);
        }
        
        List<OrderInfo.OrderItem> orderItems = orderPage.getContent().stream()
                .map(this::convertToOrderItem)
                .toList();
        
        return new OrderInfo.ListResponse(
                orderItems,
                orderPage.getTotalPages(),
                orderPage.getNumber(),
                orderPage.getSize()
        );
    }

    public OrderInfo.OrderDetail getOrderDetail(OrderCommand.Request.GetDetail request) {
        OrderModel order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
        
        if (!order.belongsToUser(request.userId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "접근 권한이 없는 주문입니다.");
        }
        
        return convertToOrderDetail(order);
    }
    
    private OrderInfo.OrderItem convertToOrderItem(OrderModel order) {
        return new OrderInfo.OrderItem(
                order.getId(),
                order.getOrderNumber().getValue(),
                order.getUserId().getValue(),
                order.getStatus().getValue(),
                order.getTotalPrice().getValue(),
                order.getCreatedAt().toLocalDateTime()
        );
    }
    
    private OrderInfo.OrderDetail convertToOrderDetail(OrderModel order) {
        List<OrderInfo.OrderDetail.OrderItemDetail> orderItemDetails = order.getOrderItems().stream()
                .map(orderItem -> new OrderInfo.OrderDetail.OrderItemDetail(
                        orderItem.getProductId().getValue(),
                        orderItem.getProductSnapshot().getProductName(),
                        orderItem.getOptionId().getValue(),
                        orderItem.getProductSnapshot().getOptionName(),
                        orderItem.getQuantity().getValue(),
                        orderItem.getOrderItemPrice().getValue(),
                        orderItem.getProductSnapshot().getImageUrl()
                ))
                .toList();
        
        return new OrderInfo.OrderDetail(
                order.getId(),
                order.getOrderNumber().getValue(),
                order.getUserId().getValue(),
                order.getStatus().getValue(),
                order.getTotalPrice().getValue(),
                order.getCreatedAt().toLocalDateTime(),
                orderItemDetails
        );
    }
}
