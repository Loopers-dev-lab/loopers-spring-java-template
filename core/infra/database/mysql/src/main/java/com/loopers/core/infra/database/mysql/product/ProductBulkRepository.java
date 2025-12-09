package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.ProductEntity;

import java.util.List;

public interface ProductBulkRepository {

    void bulkSaveOrUpdate(List<ProductEntity> products);
}
