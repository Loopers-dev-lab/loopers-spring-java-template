package com.loopers.interfaces.api.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderListDto;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.payment.CardType;
import com.loopers.interfaces.api.support.PageInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;

public class OrderDto {

  private OrderDto() {
  }

  public record OrderCreateRequest(
      @NotNull(message = "주문 항목은 null일 수 없습니다.")
      @NotEmpty(message = "주문 항목은 비어있을 수 없습니다.")
      List<OrderItemRequest> items,

      @Valid
      PaymentInfo paymentInfo,

      Long couponId
  ) {
  }

  public record PaymentInfo(
      @NotNull(message = "카드 종류는 null일 수 없습니다.")
      CardType cardType,

      @NotBlank(message = "카드 번호는 비어있을 수 없습니다.")
      String cardNo
  ) {
  }

  public record OrderItemRequest(
      @Positive(message = "상품 ID는 양수여야 합니다.")
      Long productId,

      @Positive(message = "수량은 양수여야 합니다.")
      Long quantity
  ) {
  }

  public record OrderListResponse(
      List<OrderListItemResponse> orders,
      PageInfo pageInfo
  ) {

    public static OrderListResponse from(Page<OrderListDto> page) {
      Objects.requireNonNull(page, "page는 null일 수 없습니다.");
      List<OrderListItemResponse> orders = page.getContent().stream()
          .map(OrderListItemResponse::from)
          .toList();

      PageInfo pageInfo = PageInfo.from(page);

      return new OrderListResponse(orders, pageInfo);
    }
  }

  public record OrderListItemResponse(
      Long orderId,
      LocalDateTime orderedAt,
      Long totalAmount,
      OrderStatus status,
      Integer itemCount
  ) {

    public static OrderListItemResponse from(OrderListDto dto) {
      Objects.requireNonNull(dto, "dto는 null일 수 없습니다.");
      return new OrderListItemResponse(
          dto.id(),
          dto.orderedAt(),
          dto.totalAmount(),
          dto.status(),
          dto.itemCount()
      );
    }
  }

  public record OrderDetailResponse(
      Long orderId,
      LocalDateTime orderedAt,
      OrderStatus status,
      List<OrderItemResponse> items,
      Long totalAmount,
      Long discountAmount,
      Long couponId
  ) {

    public static OrderDetailResponse from(Order order) {
      Objects.requireNonNull(order, "order는 null일 수 없습니다.");
      List<OrderItemResponse> items = order.getItems().stream()
          .map(OrderItemResponse::from)
          .toList();

      return new OrderDetailResponse(
          order.getId(),
          order.getOrderedAt(),
          order.getStatus(),
          items,
          order.getTotalAmountValue(),
          order.getDiscountAmountValue(),
          order.getCouponId()
      );
    }
  }

  public record OrderItemResponse(
      Long productId,
      String productName,
      Long quantity,
      Long price
  ) {

    public static OrderItemResponse from(OrderItem item) {
      Objects.requireNonNull(item, "item은 null일 수 없습니다.");
      return new OrderItemResponse(
          item.getProductId(),
          item.getProductName(),
          item.getQuantityValue(),
          item.getOrderPriceValue()
      );
    }
  }
}
