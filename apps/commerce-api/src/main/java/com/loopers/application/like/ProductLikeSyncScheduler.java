package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * ProductLike 배치 동기화 스케줄러
 *
 * 이벤트 유실로 인한 데이터 불일치 처리(Eventual consistency)
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeSyncScheduler {

    private final ProductLikeSyncService productLikeSyncService;

    /**
     * 좋아요 수 동기화 배치 (5분마다 실행)
     *
     * fixedDelay: 이전 작업 완료 후 5분 대기
     * initialDelay: 애플리케이션 시작 후 10초 대기
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 10000)
    public void syncProductLikeCounts() {
        log.debug("좋아요 수 동기화 배치 시작");

        try {
            int syncedCount = productLikeSyncService.syncAllProductLikeCounts();

            if (syncedCount > 0) {
                log.warn("좋아요 수 불일치 복구 - 처리 상품 수: {}", syncedCount);
                // 불일치에 대한 후처리가 필요하다면? 추가적으로 로직 구현
            } else {
                log.debug("좋아요 수 동기화 배치 완료 - 불일치 없음");
            }

        } catch (Exception e) {
            log.error("좋아요 수 동기화 배치 실패", e);
        }
    }
}
