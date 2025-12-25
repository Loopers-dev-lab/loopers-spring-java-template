package com.loopers.domain.stock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockThresholdChecker {
    @Value("${cache.stock.threshold.quantity}")
    private int thresholdQuantity;

    /**
     * 재고가 임계값 이하인지 확인
     *
     * 조건 (하나라도 만족하면 true):
     * 1. 재고가 0개 (품절)
     * 2. 재고가 절대 임계값 이하 (기본: 10개)
     *
     * @param currentStock 현재 재고
     * @return 임계값 도달 여부
     */
    public boolean isBelowThreshold(int currentStock) {
        if (currentStock == 0) {
            log.info("재고 품절 감지 - 현재 재고: {}", currentStock);
            return true;
        }

        if (currentStock <= thresholdQuantity) {
            log.info("재고 임계값 도달 - 현재 재고: {}, 임계값: {}",
                    currentStock, thresholdQuantity);
            return true;
        }

        return false;
    }
}
