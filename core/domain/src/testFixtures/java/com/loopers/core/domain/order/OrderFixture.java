package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.user.vo.UserId;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public class OrderFixture {

    public static Order create() {
        return Instancio.of(Order.class)
                .set(field(Order::getId), OrderId.empty())
                .set(field(Order::getUserId), Instancio.create(UserId.class))
                .set(field(Order::getOrderKey), OrderKey.generate())
                .set(field(Order::getCreatedAt), CreatedAt.now())
                .set(field(Order::getUpdatedAt), UpdatedAt.now())
                .set(field(Order::getDeletedAt), DeletedAt.empty())
                .create();
    }

    public static Order createWith(UserId userId) {
        return Instancio.of(Order.class)
                .set(field(Order::getId), OrderId.empty())
                .set(field(Order::getUserId), userId)
                .set(field(Order::getOrderKey), OrderKey.generate())
                .set(field(Order::getCreatedAt), CreatedAt.now())
                .set(field(Order::getUpdatedAt), UpdatedAt.now())
                .set(field(Order::getDeletedAt), DeletedAt.empty())
                .create();
    }
}
