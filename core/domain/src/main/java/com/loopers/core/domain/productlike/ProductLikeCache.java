package com.loopers.core.domain.productlike;

import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.user.vo.UserId;

public record ProductLikeCache(ProductId productId, UserId userId, Long timestamp) {

}
