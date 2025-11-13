package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 좋아요 관리 파사드.
 * <p>
 * 좋아요 추가, 삭제, 목록 조회 유즈케이스를 처리하는 애플리케이션 서비스입니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class LikeFacade {
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 상품에 좋아요를 추가합니다.
     * <p>
     * 멱등성을 보장합니다. 이미 좋아요가 존재하는 경우 아무 작업도 수행하지 않습니다.
     * </p>
     *
     * @param userId 사용자 ID (String)
     * @param productId 상품 ID
     * @throws CoreException 사용자 또는 상품을 찾을 수 없는 경우
     */
    @Transactional
    public void addLike(String userId, Long productId) {
        User user = loadUser(userId);
        loadProduct(productId);

        Optional<Like> existingLike = likeRepository.findByUserIdAndProductId(user.getId(), productId);
        if (existingLike.isPresent()) {
            return;
        }

        Like like = Like.of(user.getId(), productId);
        likeRepository.save(like);
    }

    /**
     * 상품의 좋아요를 취소합니다.
     * <p>
     * 멱등성을 보장합니다. 좋아요가 존재하지 않는 경우 아무 작업도 수행하지 않습니다.
     * </p>
     *
     * @param userId 사용자 ID (String)
     * @param productId 상품 ID
     * @throws CoreException 사용자 또는 상품을 찾을 수 없는 경우
     */
    @Transactional
    public void removeLike(String userId, Long productId) {
        User user = loadUser(userId);
        loadProduct(productId);

        Optional<Like> like = likeRepository.findByUserIdAndProductId(user.getId(), productId);
        if (like.isEmpty()) {
            return;
        }

        likeRepository.delete(like.get());
    }

    /**
     * 사용자가 좋아요한 상품 목록을 조회합니다.
     *
     * @param userId 사용자 ID (String)
     * @return 좋아요한 상품 목록
     * @throws CoreException 사용자를 찾을 수 없는 경우
     */
    public List<LikedProduct> getLikedProducts(String userId) {
        User user = loadUser(userId);

        // 사용자의 좋아요 목록 조회
        List<Like> likes = likeRepository.findAllByUserId(user.getId());

        if (likes.isEmpty()) {
            return List.of();
        }

        // 상품 ID 목록 추출
        List<Long> productIds = likes.stream()
            .map(Like::getProductId)
            .toList();

        // 상품 정보 조회
        List<Product> products = productIds.stream()
            .map(productId -> productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                    String.format("상품을 찾을 수 없습니다. (상품 ID: %d)", productId))))
            .toList();

        // 좋아요 수 집계
        Map<Long, Long> likesCountMap = likeRepository.countByProductIds(productIds);

        // 좋아요 목록을 상품 정보와 좋아요 수와 함께 변환
        return likes.stream()
            .map(like -> {
                Product product = products.stream()
                    .filter(p -> p.getId().equals(like.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        String.format("상품을 찾을 수 없습니다. (상품 ID: %d)", like.getProductId())));
                Long likesCount = likesCountMap.getOrDefault(like.getProductId(), 0L);
                return LikedProduct.from(product, like, likesCount);
            })
            .toList();
    }

    private User loadUser(String userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    private Product loadProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                String.format("상품을 찾을 수 없습니다. (상품 ID: %d)", productId)));
    }

    /**
     * 좋아요한 상품 정보.
     *
     * @param productId 상품 ID
     * @param name 상품 이름
     * @param price 상품 가격
     * @param stock 상품 재고
     * @param brandId 브랜드 ID
     * @param likesCount 좋아요 수
     */
    public record LikedProduct(
        Long productId,
        String name,
        Integer price,
        Integer stock,
        Long brandId,
        Long likesCount
    ) {
        /**
         * Product와 Like로부터 LikedProduct를 생성합니다.
         *
         * @param product 상품 엔티티
         * @param like 좋아요 엔티티
         * @param likesCount 좋아요 수
         * @return 생성된 LikedProduct
         */
        public static LikedProduct from(Product product, Like like, Long likesCount) {
            return new LikedProduct(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getBrandId(),
                likesCount
            );
        }
    }
}

