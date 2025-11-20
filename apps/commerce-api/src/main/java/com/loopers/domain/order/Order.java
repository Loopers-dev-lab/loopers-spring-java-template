package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "orders")
@Entity
public class Order extends BaseEntity {

    private Long userId;

    public static Order create(final Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유저는 비어있을 수 없습니다.");
        }
        Order order = new Order();
        order.userId = userId;
        return order;
    }
}
