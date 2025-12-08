package com.loopers.core.domain.productlike;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.vo.ProductLikeId;
import com.loopers.core.domain.user.vo.UserId;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public class ProductLikeFixture {

    public static ProductLike create() {
        return Instancio.of(ProductLike.class)
                .set(field(ProductLike::getId), ProductLikeId.empty())
                .set(field(ProductLike::getUserId), Instancio.create(UserId.class))
                .set(field(ProductLike::getProductId), Instancio.create(ProductId.class))
                .set(field(ProductLike::getCreatedAt), CreatedAt.now())
                .create();
    }

    public static ProductLike createWith(UserId userId, ProductId productId) {
        return Instancio.of(ProductLike.class)
                .set(field(ProductLike::getId), ProductLikeId.empty())
                .set(field(ProductLike::getUserId), userId)
                .set(field(ProductLike::getProductId), productId)
                .set(field(ProductLike::getCreatedAt), CreatedAt.now())
                .create();
    }
}
