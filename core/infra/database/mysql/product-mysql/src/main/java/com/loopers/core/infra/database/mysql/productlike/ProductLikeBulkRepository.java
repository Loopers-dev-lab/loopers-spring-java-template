package com.loopers.core.infra.database.mysql.productlike;

import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.infra.database.mysql.productlike.entity.ProductLikeEntity;

import java.util.List;

public interface ProductLikeBulkRepository {

    void bulkSaveOrUpdate(List<ProductLikeEntity> entities);

    void bulkDelete(List<ProductLikeCache> caches);
}
