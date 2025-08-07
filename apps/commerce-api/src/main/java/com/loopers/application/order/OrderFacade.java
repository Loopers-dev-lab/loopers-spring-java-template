package com.loopers.application.order;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.domain.order.OpderRepository;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductOptionModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class OrderFacade {
    private final UserFacade userFacade;
    private final ProductFacade productFacde;
    private final OrderService orderService;
    private final OpderRepository opderRepository;

    public OrderFacade(UserFacade userFacade, ProductFacade productFacde, OrderService orderService, OpderRepository opderRepository) {
        this.userFacade = userFacade;
        this.productFacde = productFacde;
        this.orderService = orderService;
        this.opderRepository = opderRepository;
    }

    public OrderInfo.OrderItem createOrder(OrderCommand.Request.Create request) {
        if (request.orderItems() == null || request.orderItems().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 아이템이 비어있습니다.");
        }

        UserCommand.UserResponse user = Optional.ofNullable(userFacade.getUserById(request.userId()))
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 사용자 입니다."));
        
        OrderModel orderModel = orderService.createOrderWithRetry(user.userId());
        OrderModel saveOrder =opderRepository.save(orderModel);

        for (OrderCommand.Request.Create.OrderItem orderItem : request.orderItems()) {
            ProductModel productModel = getProductModelById(orderItem.productId());

            ProductOptionModel option = getProductOptionModel(orderItem.optionId());

            OrderCommand.OrderItemData orderItemData
                    = orderService.processOrderItem(orderItem.quantity(), productModel, option);

            saveOrder.addItem(
                    orderItemData.productId(),
                    orderItemData.optionId(),
                    orderItemData.quantity(),
                    orderItemData.totalPrice(),
                    orderItemData.productName(),
                    orderItem.optionName(),
                    orderItemData.imageUrl()
            );
        }
        
        OrderModel resultOrder = opderRepository.save(saveOrder);

        return convertToOrderItem(resultOrder);
    }
    private ProductModel getProductModelById(Long productModelId) {
            return productFacde.getProductModelById(productModelId);
    }
    private ProductOptionModel getProductOptionModel(Long optionId) {
            return productFacde.getProductOptionByOptionId(optionId);
    }
    @Transactional(readOnly = true)
    public OrderInfo.ListResponse getOrderList(OrderCommand.Request.GetList request) {
        Optional.ofNullable(userFacade.getUserById(request.userId()))
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다."));
        
        if (request.page() < 0 || request.size() <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "페이지 정보가 유효하지 않습니다.");
        }
        
        Pageable pageable = PageRequest.of(request.page(), request.size());
        
        Page<OrderModel> orderPage;
        if (request.status() != null && !request.status().trim().isEmpty()) {
            orderPage = opderRepository.findByUserIdAndStatus(request.userId(), request.status(), pageable);
        } else {
            orderPage = opderRepository.findByUserId(request.userId(), pageable);
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
    @Transactional(readOnly = true)
    public OrderInfo.OrderDetail getOrderDetail(OrderCommand.Request.GetDetail request) {
        OrderModel order = opderRepository.findById(request.orderId())
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
