package com.loopers.domain.like.service;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.repository.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 좋아요 도메인 서비스
 * - DB 저장/삭제만 처리
 * - Redis 캐시 업데이트는 이벤트 리스너(LikeAggregationEventListener)에서 비동기 처리
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    /**
     * 좋아요 추가
     * - DB에는 Like 레코드만 저장
     * - 좋아요 카운트는 이벤트 리스너에서 Redis에 업데이트
     * - 스케줄러가 주기적으로 Redis → DB 동기화
     *
     * @return 좋아요한 상품 (이벤트 발행용 brandId 포함)
     */
    public Product like(Long memberId, Long productId) {
        // 중복 좋아요 방지 (멱등성)
        if (likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            log.debug("[LikeService] 이미 좋아요한 상품 - memberId: {}, productId: {}", memberId, productId);
            return productRepository.findById(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        }

        // 1. 상품 존재 확인 (비관적 락 제거)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        // 2. DB에 Like 레코드만 저장 (카운트는 Redis에서 관리)
        likeRepository.save(new Like(memberId, productId));

        log.info("[LikeService] 좋아요 저장 완료 - memberId: {}, productId: {}", memberId, productId);

        return product;
    }

    /**
     * 좋아요 취소
     * - DB에서 Like 레코드만 삭제
     * - 좋아요 카운트는 이벤트 리스너에서 Redis에 업데이트
     * - 스케줄러가 주기적으로 Redis → DB 동기화
     *
     * @return 좋아요 취소한 상품 (이벤트 발행용 brandId 포함)
     */
    public Product unlike(Long memberId, Long productId) {
        // 좋아요 없으면 스킵 (멱등성)
        if (!likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            log.debug("[LikeService] 좋아요하지 않은 상품 - memberId: {}, productId: {}", memberId, productId);
            return productRepository.findById(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        }

        // 1. 상품 존재 확인 (비관적 락 제거)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        // 2. DB에서 Like 레코드만 삭제 (카운트는 Redis에서 관리)
        likeRepository.deleteByMemberIdAndProductId(memberId, productId);

        log.info("[LikeService] 좋아요 취소 완료 - memberId: {}, productId: {}", memberId, productId);

        return product;
    }
}
