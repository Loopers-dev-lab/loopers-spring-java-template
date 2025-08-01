package com.loopers.domain.product;

import java.util.Optional;

public interface ProductOptionRepository {

    ProductOptionModel save(ProductOptionModel productOptionModel);

    void deleteAll();

    Optional<ProductOptionModel> findById(Long optionId);

    boolean existsById(Long optionId);
}
