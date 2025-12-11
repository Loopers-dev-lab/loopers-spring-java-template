package com.loopers.application.like.event;

import com.loopers.domain.like.event.LikeCreatedEvent;
import com.loopers.domain.like.event.LikeDeletedEvent;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좋아요 이벤트 핸들러
 * - 좋아요 수 집계 처리
 * - Eventual Consistency: 집계 실패해도 좋아요는 유지됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler {

    private final ProductRepository productRepository;

    /**
     * 좋아요 생성 이벤트 처리
     * - 상품의 좋아요 수 증가
     * - 트랜잭션 커밋 후 실행 (동기)
     * - 실패해도 좋아요는 유지됨
     * - REQUIRES_NEW: 새로운 트랜잭션에서 집계 수행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeCreated(LikeCreatedEvent event) {
        log.info("좋아요 생성 이벤트 처리 시작 - productId: {}, eventId: {}",
            event.productId(), event.eventId());

        try {
            // 비관적 락으로 상품 조회
            Product product = productRepository.findByIdWithLock(event.productId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

            // 좋아요 수 증가
            product.incrementLikeCount();
            log.info("좋아요 수 증가 완료 - productId: {}, likeCount: {}",
                event.productId(), product.getLikeCount());

        } catch (Exception e) {
            log.error("좋아요 집계 실패 - productId: {}, error: {}",
                event.productId(), e.getMessage(), e);
            // 집계 실패해도 좋아요는 유지됨 (Eventual Consistency)
            // TODO: 실패 로그 저장 후 재처리
        }
    }

    /**
     * 좋아요 삭제 이벤트 처리
     * - 상품의 좋아요 수 감소
     * - REQUIRES_NEW: 새로운 트랜잭션에서 집계 수행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeDeleted(LikeDeletedEvent event) {
        log.info("좋아요 삭제 이벤트 처리 시작 - productId: {}, eventId: {}",
            event.productId(), event.eventId());

        try {
            Product product = productRepository.findByIdWithLock(event.productId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

            product.decrementLikeCount();
            log.info("좋아요 수 감소 완료 - productId: {}, likeCount: {}",
                event.productId(), product.getLikeCount());

        } catch (Exception e) {
            log.error("좋아요 집계 실패 - productId: {}, error: {}",
                event.productId(), e.getMessage(), e);
        }
    }
}
