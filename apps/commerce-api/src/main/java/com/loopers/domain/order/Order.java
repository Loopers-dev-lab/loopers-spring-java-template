package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Entity
@Table(name = "tb_order")
@Getter
public class Order extends BaseEntity {
    private Long userId;
    @ElementCollection
    @CollectionTable(
            name = "tb_order_item",
            joinColumns = @JoinColumn(name = "order_id")
    )
    private List<OrderItem> orderItems;
    @Convert(converter = Price.Converter.class)
    private Price totalPrice;

    protected Order() {
    }

    private Order(Long userId, List<OrderItem> orderItems) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalPrice = new Price(orderItems.stream().map(OrderItem::getTotalPrice).reduce(Math::addExact).get());
    }

    public static Order create(Long userId, List<OrderItem> orderItems) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 1 이상이어야 합니다.");
        }
        if (orderItems == null || orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.");
        }
        return new Order(userId, orderItems);
    }
}
