package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeSyncService {
    private final ProductLikeRepository productLikeRepository;
    private final ProductService productService;
    /**
     * 단일 상품의 좋아요 수 동기화
     *
     * @param productId 동기화할 상품 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncProductLikeCount(Long productId) {
        try {
            // 1. Product 조회
            Product product = productService.getProductById(productId);

            // 2. ProductLike 테이블에서 실제 좋아요 수 집계
            long actualCount = productLikeRepository.countByProduct(product);

            // 3. 불일치 시 동기화
            if (product.getLikeCount() != actualCount) {
                log.info("좋아요 수 동기화 - ProductId: {}, 이전: {}, 실제: {}",
                        productId, product.getLikeCount(), actualCount);
                product.syncLikeCount(actualCount);
            }

        } catch (Exception e) {
            log.error("좋아요 수 동기화 실패 - ProductId: {}", productId, e);
            // 개별 실패는 로깅만 하고 다음 상품 처리 계속
        }
    }

    /**
     * 모든 상품의 좋아요 수 동기화
     *
     * @return 동기화된 상품 수
     */
    public int syncAllProductLikeCounts() {
        log.info("전체 상품 좋아요 수 동기화 시작");

        // 좋아요가 존재하는 상품 ID 목록 조회
        List<Long> productIds = productLikeRepository.findDistinctProductIds();

        int batchSize = 100;
        int syncedCount = 0;

        for (int i = 0; i < productIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, productIds.size());
            List<Long> batch = productIds.subList(i, end);

            for (Long productId : batch) {
                // 각 상품마다 독립적인 트랜잭션으로 처리
                // 한 상품의 실패가 다른 상품에 영향을 주지 않음
                syncProductLikeCount(productId);
                syncedCount++;
            }

            // 배치 간 짧은 대기 (DB 부하 완화)
            if (end < productIds.size()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // 인터럽트 발생 시에도 다음 배치 계속 진행
                    Thread.currentThread().interrupt();
                    log.warn("배치 간 대기 중 인터럽트 발생 - 다음 배치 계속 진행", e);
                }
            }
        }

        log.info("전체 상품 좋아요 수 동기화 완료 - 처리 상품 수: {}", syncedCount);
        return syncedCount;
    }
}
