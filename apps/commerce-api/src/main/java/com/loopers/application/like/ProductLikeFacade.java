package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeModel;
import com.loopers.domain.like.ProductLikeRepository;
import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductLikeFacade {
    
    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final ProductLikeService productLikeService;
    
    public ProductLikeFacade(ProductLikeRepository productLikeRepository,
                            ProductRepository productRepository,
                            ProductLikeService productLikeService) {
        this.productLikeRepository = productLikeRepository;
        this.productRepository = productRepository;
        this.productLikeService = productLikeService;
    }
    
    public void toggleLike(Long userId, Long productId) {
        ProductModel product = getProductById(productId);
        var existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        var result = productLikeService.toggleLike(product, userId, existingLike);

        if (result.isAdded()) {
            productLikeRepository.save(result.getLike());
        } else {
            productLikeRepository.delete(result.getLike());
        }
        productRepository.save(product);
    }
    
    public ProductLikeModel addProductLike(Long userId, Long productId) {
        if (productLikeRepository.existsByUserIdAndProductId(userId, productId)) {
            return productLikeRepository.findByUserIdAndProductId(userId, productId).get();
        }

        ProductModel product = getProductById(productId);
        var newLike = productLikeService.addLike(product, userId);
        productLikeRepository.save(newLike);
        productRepository.save(product);
        return newLike;
    }
    
    public void removeProductLike(Long userId, Long productId) {
        var existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isPresent()) {
            ProductModel product = getProductById(productId);
            productLikeService.removeLike(product, existingLike.get());
            productLikeRepository.delete(existingLike.get());
            productRepository.save(product);
        }
    }
    
    @Transactional(readOnly = true)
    public boolean isProductLiked(Long userId, Long productId) {
        return productLikeRepository.existsByUserIdAndProductId(userId, productId);
    }
    
    private ProductModel getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
}
