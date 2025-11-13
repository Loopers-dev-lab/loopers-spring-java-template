package com.loopers.application.order;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.common.Money;
import java.util.List;
import com.loopers.domain.order.OrderItemModel;

public record OrderInfo(Long id, UserModel user, Money totalPrice, List<OrderItemModel> orderItems) {

}
