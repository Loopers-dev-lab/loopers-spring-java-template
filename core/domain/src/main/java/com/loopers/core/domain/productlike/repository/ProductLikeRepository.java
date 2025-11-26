package com.loopers.core.domain.productlike.repository;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.productlike.LikeProductListView;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.domain.user.vo.UserId;

import java.util.List;

public interface ProductLikeRepository {

    ProductLike save(ProductLike productLike);

    LikeProductListView findLikeProductsListWithCondition(
            UserId userId,
            BrandId brandId,
            OrderSort createdAtSort,
            OrderSort priceSort,
            OrderSort likeCountSort,
            int pageNo,
            int pageSize
    );

    void bulkSaveOrUpdate(List<ProductLike> productLikes);

    void bulkDelete(List<ProductLikeCache> productLikes);
}
