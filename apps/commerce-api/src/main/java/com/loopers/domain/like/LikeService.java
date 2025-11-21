package com.loopers.domain.like;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    //상품 좋아요
    @Transactional
    public void likeProduct(String userId, Long productId){
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        if (likeRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            return;
        }

        try {
            likeRepository.save(new Like(userId, productId));
            product.increaseLikeCount();
            productRepository.save(product);
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    @Transactional
    public void cancleLikeProduct(String useId, Long productId){
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        likeRepository.findByUserIdAndProductId(useId, productId)
                .ifPresent(like -> {
                    likeRepository.delete(like);
                    product.decreaseLikeCount();
                    productRepository.save(product);
                });

    }

    @Transactional(readOnly = true)
    public Long getLikeCount(Long productId){
        return likeRepository.countByProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<Like> getUserLikeProduct(String userId){
        return likeRepository.findAllByUserId(userId);
    }
}
