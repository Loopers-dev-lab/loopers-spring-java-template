package com.loopers.domain.supply;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class SupplyService {
    private final SupplyRepository supplyRepository;

    public Supply getSupplyByProductId(Long productId) {
        return supplyRepository.findByProductId(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품의 재고 정보를 찾을 수 없습니다."));
    }

    public Map<Long, Supply> getSupplyMapByProductIds(Collection<Long> productIds) {
        return supplyRepository.findAllByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Supply::getProductId, supply -> supply));
    }

    @Transactional
    public void checkAndDecreaseStock(Long productId, Integer quantity) {
        Supply supply = supplyRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품의 재고 정보를 찾을 수 없습니다."));
        supply.decreaseStock(quantity);
        supplyRepository.save(supply);
    }

    public Supply save(Supply supply) {
        return supplyRepository.save(supply);
    }

    @Transactional
    public List<Supply> saveAll(List<Supply> list) {
        return supplyRepository.saveAll(list);
    }
}
