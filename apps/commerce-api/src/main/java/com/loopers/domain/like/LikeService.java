package com.loopers.domain.like;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;
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
    private final ProductRepository productRepository;

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
     * - 좋아요 관계가 없으면: 새로 생성하고 카운트 증가
     * - 삭제된 좋아요가 있으면: 복원하고 카운트 증가
     * - 활성 좋아요가 있으면: 기존 엔티티 반환 (카운트 변경 없음 - 중복 방지)
     * <p>
     * 좋아요 카운트는 DB 원자적 연산(UPDATE 쿼리)으로 처리하여 동시성을 보장합니다.
     *
     * @param user    사용자 엔티티
     * @param product 상품 엔티티
     * @return 생성 또는 복원된 좋아요 엔티티
     */
    @Transactional
    public LikeEntity upsertLike(UserEntity user, ProductEntity product) {
        return likeRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .map(like -> {
                    // 삭제된 좋아요인 경우만 복원 및 카운트 증가
                    if (like.getDeletedAt() != null) {
                        like.restore();
                        // DB 원자적 연산으로 카운트 증가
                        productRepository.incrementLikeCount(product.getId());
                    }
                    // 활성 좋아요인 경우: 카운트 변경 없음 (중복 방지)
                    return like;
                })
                // 좋아요가 없는 경우 새로 생성
                .orElseGet(() -> {
                    // DB 원자적 연산으로 카운트 증가
                    productRepository.incrementLikeCount(product.getId());
                    return likeRepository.save(LikeEntity.createEntity(user.getId(), product.getId()));
                });
    }

    /**
     * Soft-deletes a user's like for the given product and decrements the product's like count.
     *
     * If the like is already deleted this method is idempotent and performs no action.
     * The product's like count is updated using an atomic database operation to preserve concurrency.
     */
    @Transactional
    public void unlikeProduct(UserEntity user, ProductEntity product) {
        likeRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .ifPresent(like -> {
                    // 이미 삭제된 좋아요인 경우 무시 (멱등성 보장)
                    if (like.getDeletedAt() != null) {
                        return;
                    }
                    like.delete();
                    // DB 원자적 연산으로 카운트 감소
                    productRepository.decrementLikeCount(product.getId());
                });
    }
}