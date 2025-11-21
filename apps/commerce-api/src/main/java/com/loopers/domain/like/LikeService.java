package com.loopers.domain.like;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    //상품 좋아요
    @Transactional
    public void likeProduct(String userId, Long productId){
        if (likeRepository.findByUserIdAndProductId(userId, productId).isEmpty()) {
            Like like = new Like(userId, productId);
            try {
                likeRepository.save(like);
            } catch (DataIntegrityViolationException e) {
                // 동시성으로 인한 중복 삽입은 무시 (유니크 제약이 보장)
            }

        }
    }

    //상품 좋아요 취소
    @Transactional
    public void cancleLikeProduct(String useId, Long productId){
        likeRepository.findByUserIdAndProductId(useId, productId)
                .ifPresent(likeRepository::delete);

    }

    //상품 좋아요 수 조회
    @Transactional(readOnly = true)
    public Long getLikeCount(Long productId){
        return likeRepository.countByProductId(productId);
    }

    //유저가 좋아요한 상품 조회
    @Transactional(readOnly = true)
    public List<Like> getUserLikeProduct(String userId){
        return likeRepository.findAllByUserId(userId);
    }
}
