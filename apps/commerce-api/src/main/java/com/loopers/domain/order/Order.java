package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Getter
    @Column(name = "member_id", nullable = false, length = 10)
    private String memberId;

    @Getter
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_price"))
    private Money totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    private Order(String memberId, List<OrderItem> items, Money totalPrice) {
        validateMemberId(memberId);
        validateItems(items);
        validateTotalPrice(totalPrice);

        this.memberId = memberId;
        this.totalPrice = totalPrice;
        this.items = new ArrayList<>();
        items.forEach(this::addItem);
    }

    public static Order create(String memberId, List<OrderItem> items) {
        validateMemberId(memberId);
        validateItems(items);
        Money totalPrice = calculateTotalPrice(items);
        return new Order(memberId, items, totalPrice);
    }

    private void addItem(OrderItem item) {
        this.items.add(item);
        item.assignOrder(this);
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private static Money calculateTotalPrice(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(Money.zero(), Money::plus);
    }

    private static void validateMemberId(String memberId) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }

    private static void validateItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 필수입니다");
        }
    }

    private static void validateTotalPrice(Money totalPrice) {
        if (totalPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "총 금액은 필수입니다.");
        }
    }
}
