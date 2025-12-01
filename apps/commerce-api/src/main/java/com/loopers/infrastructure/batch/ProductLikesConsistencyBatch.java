package com.loopers.infrastructure.batch;

import com.loopers.infrastructure.product.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikesConsistencyBatch {
    private final ProductJpaRepository productRepository;

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void validateAndFixProductLikesConsistency() {
        List<Map<String, Object>> inconsistencies = productRepository.findLikeCountInconsistencies();

        if (inconsistencies.isEmpty()) {
            return;
        }

        log.warn("Found {} product(s) with inconsistent totalLikes count", inconsistencies.size());

        for (Map<String, Object> item : inconsistencies) {
            Long productId = ((Number) item.get("product_id")).longValue();
            Long totalLikes = ((Number) item.get("total_likes")).longValue();
            Long actualLikes = ((Number) item.get("actual_likes")).longValue();

            log.warn("Product ID: {}, DB totalLikes: {}, Actual likes: {}",
                    productId, totalLikes, actualLikes);

            productRepository.updateProductTotalLikes(productId, actualLikes);
        }

        log.info("Consistency check and fix completed");
    }
}