package com.loopers.application.metrics;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.domain.eventhandled.EventHandledDomainType;
import com.loopers.domain.eventhandled.EventHandledService;
import com.loopers.domain.metrics.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsFacade {

    private final ProductMetricsService productMetricsService;
    private final EventHandledService eventHandledService;
    private final RankingFacade rankingFacade;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Clock clock;

    private static final EventHandledDomainType DOMAIN_TYPE = EventHandledDomainType.METRICS;
    private static final String PRODUCT_CACHE_KEY_PATTERN = "product:*:detail:%d";
    private static final String PRODUCT_LIST_CACHE_KEY_PATTERN = "product:*:list:*";

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @Transactional
    public void processLikeMetrics(ProductMetricsCommand command) {
        if (eventHandledService.isEventHandled(command.eventId(), DOMAIN_TYPE)) {
            return;
        }

        LocalDate date = today();

        // 좋아요 메트릭 처리
        productMetricsService.processLikeMetrics(command.productId(), command.likeType(), date);

        // 랭킹 점수 업데이트
        boolean isLike = "LIKED".equals(command.likeType());
        rankingFacade.processLikeEvent(command.productId(), isLike);

        eventHandledService.saveEventHandled(command.eventId(), DOMAIN_TYPE, command.metricsType().toString());
    }

    @Transactional
    public void processStockMetrics(ProductMetricsCommand command) {
        if (eventHandledService.isEventHandled(command.eventId())) {
            return;
        }

        LocalDate date = today();

        // 재고 메트릭 처리
        productMetricsService.processStockMetrics(command.productId(), command.stock(), command.changedType(), date);

        // 랭킹 점수 업데이트 (재고 감소 시)
        if ("DECREASED".equals(command.changedType())) {
            rankingFacade.processOrderEvent(command.productId(), command.stock());
        }

        eventHandledService.saveEventHandled(command.eventId(), DOMAIN_TYPE, command.metricsType().toString());

        // 재고 변경 시 캐시 무효화
        invalidateProductCache(command.productId());
    }

    @Transactional
    public void processViewMetrics(ProductMetricsCommand command) {
        if (eventHandledService.isEventHandled(command.eventId())) {
            return;
        }

        LocalDate date = today();

        // 조회 메트릭 처리
        productMetricsService.processViewMetrics(command.productId(), date);

        // 랭킹 점수 업데이트
        rankingFacade.processViewEvent(command.productId());

        eventHandledService.saveEventHandled(command.eventId(), DOMAIN_TYPE, command.metricsType().toString());
    }

    /**
     * 상품 캐시 무효화
     * - 상품 상세 캐시 삭제
     * - 상품 목록 캐시 삭제 (해당 상품이 포함된 목록 캐시)
     */
    private void invalidateProductCache(Long productId) {
        try {
            // 상품 상세 캐시 삭제 (버전별로 삭제)
            String detailKeyPattern = String.format("product:*:detail:%d", productId);
            Set<String> detailKeys = redisTemplate.keys(detailKeyPattern);
            if (detailKeys != null && !detailKeys.isEmpty()) {
                redisTemplate.delete(detailKeys);
                log.info("상품 상세 캐시 무효화 완료: productId={}, keys={}", productId, detailKeys.size());
            }

            // 상품 목록 캐시 삭제
            Set<String> listKeys = redisTemplate.keys("product:*:list:*");
            if (listKeys != null && !listKeys.isEmpty()) {
                redisTemplate.delete(listKeys);
                log.info("상품 목록 캐시 무효화 완료: productId={}, keys={}", productId, listKeys.size());
            }
        } catch (Exception e) {
            log.error("캐시 무효화 실패: productId={}", productId, e);
            // 캐시 무효화 실패는 비즈니스 로직에 영향을 주지 않도록 예외 전파하지 않음
        }
    }
}
