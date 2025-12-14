package com.loopers.domain.like;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.user.UserEntity;

import lombok.RequiredArgsConstructor;

/**
 * 좋아요 도메인 서비스
 * <p>
 * 좋아요 도메인의 비즈니스 로직을 처리합니다.
 * 좋아요와 상품 간의 협력을 통해 좋아요 카운트를 관리합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@Component
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    /**
     * 사용자와 상품의 좋아요 관계를 조회합니다.
     *
     * @param userId    사용자 ID
     * @param productId 상품 ID
     * @return 좋아요 엔티티 (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<LikeEntity> findLike(Long userId, Long productId) {
        return likeRepository.findByUserIdAndProductId(userId, productId);
    }


    /**
     * 좋아요를 등록하거나 복원합니다 (Upsert).
     * <p>
     * - 좋아요 관계가 없으면: 새로 생성하고 도메인 이벤트 발행
     * - 삭제된 좋아요가 있으면: 복원하고 도메인 이벤트 발행
     * - 활성 좋아요가 있으면: 기존 엔티티 반환 (이벤트 발행 없음 - 중복 방지)
     *
     * @param user    사용자 엔티티
     * @param product 상품 엔티티
     * @return 좋아요 등록 결과 (엔티티와 실제 변경 여부)
     */
    @Transactional
    public LikeResult upsertLike(UserEntity user, ProductEntity product) {
        Optional<LikeEntity> existingLike = likeRepository.findByUserIdAndProductId(user.getId(), product.getId());

        if (existingLike.isPresent()) {
            LikeEntity like = existingLike.get();
            // 삭제된 좋아요인 경우만 복원 및 이벤트 발행
            if (like.getDeletedAt() != null) {
                like.restoreWithEvent(); // 도메인 이벤트 발행
                likeRepository.save(like);
                return new LikeResult(like, true); // 복원됨 - 이벤트 발행됨
            }
            // 활성 좋아요인 경우: 이벤트 발행 없음 (중복 방지)
            return new LikeResult(like, false); // 이미 존재 - 이벤트 발행 안됨
        }

        // 좋아요가 없는 경우 새로 생성 및 이벤트 발행
        LikeEntity newLike = likeRepository.save(LikeEntity.createWithEvent(user.getId(), product.getId()));
        return new LikeResult(newLike, true); // 새로 생성됨 - 이벤트 발행됨
    }

    /**
     * 좋아요를 취소합니다 (소프트 삭제).
     * <p>
     * 좋아요를 삭제하고 도메인 이벤트를 발행합니다.
     *
     * @param user    사용자 엔티티
     * @param product 상품 엔티티
     * @return 실제로 삭제가 발생했는지 여부 (이벤트 발행 여부)
     */
    @Transactional
    public boolean unlikeProduct(UserEntity user, ProductEntity product) {
        Optional<LikeEntity> existingLike = likeRepository.findByUserIdAndProductId(user.getId(), product.getId());

        if (existingLike.isEmpty()) {
            return false; // 좋아요가 없음 - 이벤트 발행 안됨
        }

        LikeEntity like = existingLike.get();

        // 이미 삭제된 좋아요인 경우 무시 (멱등성 보장)
        if (like.getDeletedAt() != null) {
            return false; // 이미 삭제됨 - 이벤트 발행 안됨
        }

        like.deleteWithEvent(); // 도메인 이벤트 발행
        likeRepository.save(like);
        return true; // 삭제됨 - 이벤트 발행됨
    }

    /**
     * 상품의 좋아요 수를 조회합니다.
     *
     * @param product
     * @return
     */
    @Transactional(readOnly = true)
    public Long countByProduct(ProductEntity product) {
        return likeRepository.countByProductIdAndDeletedAtIsNull(product.getId());
    }
}
