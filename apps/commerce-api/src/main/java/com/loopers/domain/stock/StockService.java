package com.loopers.domain.stock;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public Optional<Stock> findByProductId(Long productId) {
        return stockRepository.findByProductId(productId);
    }

    @Transactional
    public Optional<Stock> saveStock(Stock stock) {
        return stockRepository.save(stock);
    }

    /**
     * 재고 감소 (동시성 안전)
     * @param productId 상품 ID
     * @param decreaseQuantity 감소할 수량
     * @throws CoreException 감소량이 0 이하이거나, 재고가 부족한 경우
     */
    @Transactional
    public void decreaseQuantity(Long productId, Long decreaseQuantity) {
        log.info("재고 차감 시도 - productId: {}, decreaseQuantity: {}", productId, decreaseQuantity);
        
        // 감소량 유효성 검사
        if (decreaseQuantity == null || decreaseQuantity <= 0L) {
            log.error("재고 감소량이 유효하지 않습니다 - productId: {}, decreaseQuantity: {}", productId, decreaseQuantity);
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "재고 감소량은 0보다 커야 합니다."
            );
        }

        // QueryDSL로 동시성 안전하게 업데이트
        long updatedRows = stockRepository.decreaseQuantity(productId, decreaseQuantity);
        log.info("재고 차감 업데이트 결과 - productId: {}, updatedRows: {}", productId, updatedRows);
        
        if (updatedRows == 0L) {
            log.warn("재고 차감 실패 (updatedRows=0) - productId: {}, decreaseQuantity: {}", productId, decreaseQuantity);
            // 재고 조회하여 상세 메시지 제공
            Optional<Stock> stockOpt = stockRepository.findByProductId(productId);
            if (stockOpt.isEmpty()) {
                log.error("Stock을 찾을 수 없습니다 - productId: {}", productId);
                throw new CoreException(
                        ErrorType.NOT_FOUND,
                        "[productId = " + productId + "] Stock을 찾을 수 없습니다."
                );
            }
            
            Stock stock = stockOpt.get();
            log.error("재고가 부족합니다 - productId: {}, 현재 재고: {}, 요청 수량: {}", 
                    productId, stock.getQuantity(), decreaseQuantity);
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "재고가 부족합니다. (현재 재고: " + stock.getQuantity() + ", 요청 수량: " + decreaseQuantity + ")"
            );
        }
        
        log.info("재고 차감 성공 - productId: {}, decreaseQuantity: {}", productId, decreaseQuantity);
    }

    /**
     * 재고 증가 (동시성 안전)
     * @param productId 상품 ID
     * @param increaseQuantity 증가할 수량
     * @throws CoreException 증가량이 0 이하이거나, Stock을 찾을 수 없는 경우
     */
    @Transactional
    public void increaseQuantity(Long productId, Long increaseQuantity) {
        // 증가량 유효성 검사
        if (increaseQuantity == null || increaseQuantity <= 0L) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "재고 증가량은 0보다 커야 합니다."
            );
        }

        // QueryDSL로 동시성 안전하게 업데이트
        long updatedRows = stockRepository.increaseQuantity(productId, increaseQuantity);
        
        if (updatedRows == 0L) {
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "[productId = " + productId + "] 재고를 증가할 Stock 객체를 찾을 수 없습니다."
            );
        }
    }
}

