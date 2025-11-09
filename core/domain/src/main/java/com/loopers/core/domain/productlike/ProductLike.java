package com.loopers.core.domain.productlike;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.vo.ProductLikeId;
import com.loopers.core.domain.user.vo.UserId;
import lombok.Getter;

@Getter
public class ProductLike {

    private final ProductLikeId id;

    private final UserId userId;

    private final ProductId productId;

    private final CreatedAt createdAt;

    private ProductLike(ProductLikeId id, UserId userId, ProductId productId, CreatedAt createdAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public static ProductLike create(UserId userId, ProductId productId) {
        return new ProductLike(
                ProductLikeId.empty(),
                userId,
                productId,
                CreatedAt.now()
        );
    }

    public static ProductLike mappedBy(ProductLikeId id, UserId userId, ProductId productId, CreatedAt createdAt) {
        return new ProductLike(id, userId, productId, createdAt);
    }
}
