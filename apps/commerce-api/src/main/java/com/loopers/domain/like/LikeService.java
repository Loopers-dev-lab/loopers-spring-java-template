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
     * 신규 생성 또는 복원 시에만 Product의 좋아요 카운트를 증가시키고 저장합니다.
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
                        product.increaseLikeCount();
                        productRepository.save(product); // Product 변경사항 저장
                    }
                    // 활성 좋아요인 경우: 카운트 변경 없음 (중복 방지)
                    return like;
                })
                // 좋아요가 없는 경우 새로 생성
                .orElseGet(() -> {
                    product.increaseLikeCount();
                    productRepository.save(product); // Product 변경사항 저장
                    return likeRepository.save(LikeEntity.createEntity(user.getId(), product.getId()));
                });
    }

    /**
     * 좋아요를 취소합니다 (소프트 삭제).
     * <p>
     * 좋아요를 삭제하고 상품의 좋아요 카운트를 감소시킵니다.
     *
     * @param user    사용자 엔티티
     * @param product 상품 엔티티
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
                    product.decreaseLikeCount();
                    productRepository.save(product);
                });
    }
}
