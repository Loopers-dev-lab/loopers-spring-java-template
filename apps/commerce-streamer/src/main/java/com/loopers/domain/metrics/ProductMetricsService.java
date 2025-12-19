package com.loopers.domain.metrics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.event.EventRepository;
import com.loopers.domain.metrics.repository.MetricsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메트릭 트랜잭션 처리 서비스
 * <p>
 * Spring AOP Self-Invocation 문제를 해결하기 위해 분리된 트랜잭션 서비스입니다.
 * MetricsService에서 @Transactional 메서드를 호출할 때 발생하는 문제를 방지합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductMetricsService {

    private final MetricsRepository metricsRepository;
    private final EventRepository eventHandledRepository;

    /**
     * 조회수 증가 (트랜잭션 적용)
     */
    @Transactional
    public void incrementViewWithTransaction(Long productId, long occurredAtEpochMillis) {
        try {
            metricsRepository.incrementView(productId, occurredAtEpochMillis);
            log.debug("조회수 증가 완료: productId={}", productId);
        } catch (Exception e) {
            log.error("조회수 증가 실패: productId={}", productId, e);
            throw e;
        }
    }

    /**
     * 좋아요 수 변경 (트랜잭션 적용)
     */
    @Transactional
    public void applyLikeDeltaWithTransaction(Long productId, int delta, long occurredAtEpochMillis) {
        try {
            metricsRepository.applyLikeDelta(productId, delta, occurredAtEpochMillis);
            log.debug("좋아요 수 변경 완료: productId={}, delta={}", productId, delta);
        } catch (Exception e) {
            log.error("좋아요 수 변경 실패: productId={}, delta={}", productId, delta, e);
            throw e;
        }
    }

    /**
     * 판매량 증가 (트랜잭션 적용)
     */
    @Transactional
    public void addSalesWithTransaction(Long productId, int quantity, long occurredAtEpochMillis) {
        try {
            metricsRepository.addSales(productId, quantity, occurredAtEpochMillis);
            log.debug("판매량 증가 완료: productId={}, quantity={}", productId, quantity);
        } catch (Exception e) {
            log.error("판매량 증가 실패: productId={}, quantity={}", productId, quantity, e);
            throw e;
        }

    }


    /**
     * 재고 소진 이벤트 처리 (트랜잭션 적용)
     * 주로 캐시 갱신을 담당합니다.
     */
    @Transactional
    public void handleStockDepletedWithTransaction(Long productId, Long brandId, Integer remainingStock, long occurredAtEpochMillis) {
        try {
            // 재고 소진 시 캐시 갱신 처리
            metricsRepository.handleStockDepleted(productId, brandId, remainingStock, occurredAtEpochMillis);
            log.debug("재고 소진 처리 완료: productId={}, brandId={}, remainingStock={}", productId, brandId, remainingStock);
        } catch (Exception e) {
            log.error("재고 소진 처리 실패: productId={}, brandId={}, remainingStock={}", productId, brandId, remainingStock, e);
            throw e;
        }
    }

    /**
     * 이벤트 처리 완료 마킹 (트랜잭션 적용)
     * 예외 기반이 아닌 조회 기반으로 중복 체크를 수행합니다.
     */
    @Transactional
    public boolean saveEventHandled(String eventId) {
        try {
            // 트랜잭션 내에서 다시 한번 확인 (동시성 안전)
            if (eventHandledRepository.existsById(eventId)) {
                log.debug("트랜잭션 내 중복 확인: {}", eventId);
                return false;
            }

            eventHandledRepository.save(com.loopers.domain.event.EventEntity.create(eventId));
            log.debug("이벤트 처리 완료 저장: {}", eventId);
            return true;
        } catch (Exception e) {
            // 동시성으로 인한 중복 저장 시도 (Unique 제약 조건 위반 등)
            log.debug("동시성으로 인한 이벤트 저장 실패: {}", eventId, e);
            return false;
        }
    }
}
