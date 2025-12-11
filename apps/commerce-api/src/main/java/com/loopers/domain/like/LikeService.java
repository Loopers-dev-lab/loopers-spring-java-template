package com.loopers.domain.like;

import com.loopers.domain.like.entity.LikeTargetType;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 상품 좋아요 등록
     * 멱등성: 중복 저장하면 이전 것을 덮어씀
     * 동시성 안전: INSERT ... ON DUPLICATE KEY UPDATE를 사용하여 원자적으로 처리
     * 
     * 좋아요 저장은 완료됨. 집계 이벤트는 별도 리스너에서 처리됨.
     */
    @Transactional
    public void saveProductLike(User user, Long productId) {
        // INSERT ... ON DUPLICATE KEY UPDATE 실행
        // 반환값: 1 = 새로 생성됨, 2 = 이미 존재하여 UPDATE만 됨
        long affectedRows = likeRepository.insertOrIgnore(user.getId(), productId, LikeTargetType.PRODUCT);
        
        // 실제로 새로 생성된 경우에만 내부 이벤트 발행
        // 집계 이벤트는 LikeEventListener에서 트랜잭션 커밋 후 처리됨
        if (affectedRows == 1) {
            eventPublisher.publishEvent(new LikeEvents.ProductLikeSaved(productId));
        }
    }

    /**
     * 상품 좋아요 취소
     * 멱등성: Like가 없어도 예외 없이 처리
     * 
     * 좋아요 삭제는 완료됨. 집계 이벤트는 별도 리스너에서 처리됨.
     */
    @Transactional
    public void deleteProductLike(User user, Long productId) {
        // 삭제 실행 (영향받은 행 수 반환)
        long deletedRows = likeRepository.deleteByUserIdAndLikeTargetId(
                user.getId(), productId, LikeTargetType.PRODUCT
        );
        
        // 실제로 삭제된 경우에만 내부 이벤트 발행
        // 집계 이벤트는 LikeEventListener에서 트랜잭션 커밋 후 처리됨
        if (deletedRows > 0) {
            eventPublisher.publishEvent(new LikeEvents.ProductLikeDeleted(productId));
        }
    }
}

