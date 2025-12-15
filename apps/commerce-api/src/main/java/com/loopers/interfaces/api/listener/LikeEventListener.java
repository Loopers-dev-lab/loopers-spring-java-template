package com.loopers.interfaces.api.listener;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.like.LikeEvent;
import com.loopers.domain.user.UserActionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventListener {

    private final LikeFacade likeFacade;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 좋아요 등록/취소 후 상품 likeCount 집계 업데이트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLikeCountUpdate(LikeEvent event) {
        log.info("좋아요 집계 업데이트: loginId={}, productId={}, action={}",
                event.userId(), event.productId(), event.action());

        try {
            switch (event.action()) {
                case ADDED -> likeFacade.incrementLikeCount(event.productId());
                case REMOVED -> likeFacade.decrementLikeCount(event.productId());
            }
            log.info("좋아요 집계 업데이트 완료: productId={}", event.productId());
        } catch (Exception e) {
            log.error("좋아요 집계 업데이트 실패: productId={}, reason={}",
                    event.productId(), e.getMessage());
        }
    }

    /**
     * 좋아요 등록/취소 후 캐시 무효화
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheEviction(LikeEvent event) {
        log.debug("좋아요 관련 캐시 무효화: productId={}", event.productId());

        try {
            likeFacade.evictLikeRelatedCache(event.productId());
        } catch (Exception e) {
            log.error("캐시 무효화 실패: productId={}", event.productId(), e);
        }
    }

    /**
     * 좋아요 유저 행동 이벤트 발행
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserActionEvent(LikeEvent event) {
        log.debug("좋아요 유저 행동 이벤트 발행: userId={}, productId={}, action={}",
                event.userId(), event.productId(), event.action());

        try {
            UserActionEvent userActionEvent = switch (event.action()) {
                case ADDED -> UserActionEvent.productLike(event.userId(), event.productId());
                case REMOVED -> UserActionEvent.productUnlike(event.userId(), event.productId());
            };

            eventPublisher.publishEvent(userActionEvent);
        } catch (Exception e) {
            log.error("유저 행동 이벤트 발행 실패: userId={}, productId={}",
                    event.userId(), event.productId(), e);
        }
    }
}
