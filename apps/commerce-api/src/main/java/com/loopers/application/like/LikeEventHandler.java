package com.loopers.application.like;

import com.loopers.domain.like.event.LikeCreatedEvent;
import com.loopers.domain.like.event.LikeDeletedEvent;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler {

    private final ProductRepository productRepository;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLikeCreated(LikeCreatedEvent event) {
        try {
            productRepository.findByIdForUpdate(event.productId())
                    .ifPresent(product -> {
                        product.increaseLikeCount();
                        productRepository.save(product);
                    });

            log.info("좋아요 집계 증가 완료 - productId={}, userId={}",
                    event.productId(), event.userId());
        } catch (Exception e) {
            log.error("좋아요 집계 증가 실패 - productId={}, error={}",
                    event.productId(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLikeDeleted(LikeDeletedEvent event) {
        try {
            productRepository.findByIdForUpdate(event.productId())
                    .ifPresent(product -> {
                        product.decreaseLikeCount();
                        productRepository.save(product);
                    });

            log.info("좋아요 집계 감소 완료 - productId={}, userId={}",
                    event.productId(), event.userId());
        } catch (Exception e) {
            log.error("좋아요 집계 감소 실패 - productId={}, error={}",
                    event.productId(), e.getMessage());
        }
    }
}
