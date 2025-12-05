package com.loopers.application.order;

import com.loopers.domain.payment.CardType;
import java.util.List;

public record OrderCreateInfo(
        CardType cardType,
        String cardNo,
        List<OrderCreateItemInfo> orderCreateItemInfos
) {
}
