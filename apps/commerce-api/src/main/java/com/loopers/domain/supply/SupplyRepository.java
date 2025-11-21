package com.loopers.domain.supply;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupplyRepository {
    Optional<Supply> findByProductId(Long productId);

    List<Supply> findAllByProductIdIn(Collection<Long> productIds);

    Optional<Supply> findByProductIdForUpdate(Long productId);

    Supply save(Supply supply);
}
