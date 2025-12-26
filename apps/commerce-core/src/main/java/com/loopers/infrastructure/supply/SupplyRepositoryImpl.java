package com.loopers.infrastructure.supply;

import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class SupplyRepositoryImpl implements SupplyRepository {
    private final SupplyJpaRepository supplyJpaRepository;

    @Override
    public Optional<Supply> findByProductId(Long productId) {
        return supplyJpaRepository.findByProductId(productId);
    }

    @Override
    public List<Supply> findAllByProductIdIn(Collection<Long> productIds) {
        return supplyJpaRepository.findAllByProductIdIn(productIds);
    }

    @Override
    public Optional<Supply> findByProductIdForUpdate(Long productId) {
        return supplyJpaRepository.findByProductIdForUpdate(productId);
    }

    @Override
    public Supply save(Supply supply) {
        return supplyJpaRepository.save(supply);
    }

    @Override
    public List<Supply> saveAll(List<Supply> list) {
        return supplyJpaRepository.saveAll(list);
    }
}
