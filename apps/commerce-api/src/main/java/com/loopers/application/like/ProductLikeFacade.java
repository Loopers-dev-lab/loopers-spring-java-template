package com.loopers.application.like;

import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.product.ProductLikeModel;
import com.loopers.domain.like.product.ProductLikeRepository;
import com.loopers.domain.like.product.ProductLikeService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductLikeFacade {
    
    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductLikeService productLikeService;

    public ProductLikeFacade(ProductLikeRepository productLikeRepository,
                            ProductRepository productRepository,
                            BrandRepository brandRepository,
                            ProductLikeService productLikeService) {
        this.productLikeRepository = productLikeRepository;
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.productLikeService = productLikeService;
    }
    @Transactional
    public void toggleLike(Long userId, Long productId) {
        ProductModel product = getProductById(productId);
        ProductLikeModel existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        
        ProductLikeService.LikeToggleResult result = productLikeService.toggleLike(product, userId, existingLike);
        
        if (result.isAdded()) {
            productLikeRepository.save(result.getLike());
        } else {
            productLikeRepository.delete(result.getLike());
        }
        
        productRepository.save(product);
    }
    @Transactional
    public ProductLikeModel addProductLike(Long userId, Long productId) {
        ProductModel product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        
        if (productLikeRepository.existsByUserIdAndProductId(userId, productId)) {
            return productLikeRepository.findByUserIdAndProductId(userId, productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 좋아요를 찾을 수 없습니다."));
        }

        ProductLikeModel newLike = productLikeService.addLike(product, userId);
        productLikeRepository.save(newLike);
        productRepository.save(product);
        return newLike;
    }
    @Transactional
    public void removeProductLike(Long userId, Long productId) {
        Optional<ProductLikeModel> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isPresent()) {
            ProductModel product = getProductById(productId);
            productLikeService.removeLike(product, existingLike.get());
            productLikeRepository.delete(existingLike.get());
            productRepository.save(product);
        }
    }
    
    public boolean isProductLiked(Long userId, Long productId) {
        return productLikeRepository.existsByUserIdAndProductId(userId, productId);
    }

    public ProductLikeCommand.LikedProductsData getUserLikedProducts(ProductLikeCommand.Request.GetLikedProducts request) {
        Pageable pageable = PageRequest.of(request.page(), request.size());
        Page<ProductLikeModel> productLikes = productLikeRepository.findByUserIdOrderByLikedAtDesc(request.userId(), pageable);
        
        if (productLikes.isEmpty()) {
            return new ProductLikeCommand.LikedProductsData(productLikes, List.of());
        }

        List<Long> productIds = productLikes.getContent().stream()
                .map(ProductLikeModel::getProductId)
                .toList();
        
        Map<Long, ProductModel> productMap = productRepository.findByIdIn(productIds).stream()
                .collect(Collectors.toMap(ProductModel::getId, Function.identity()));

        List<Long> brandIds = productMap.values().stream()
                .map(product -> product.getBrandId().getValue())
                .distinct()
                .toList();
        
        Map<Long, String> brandNameMap = brandRepository.findByIdIn(brandIds).stream()
                .collect(Collectors.toMap(
                        brand -> brand.getId(),
                        brand -> brand.getBrandName().getValue()
                ));

        List<ProductLikeCommand.LikedProductItem> likedProductItems = productLikes.getContent().stream()
                .map(productLike -> {
                    ProductModel product = productMap.get(productLike.getProductId());
                    if (product == null) {
                        return null; // 삭제된 상품의 경우 제외
                    }
                    String brandName = brandNameMap.get(product.getBrandId().getValue());
                    return ProductLikeCommand.LikedProductItem.of(productLike, product, brandName);
                })
                .filter(item -> item != null)
                .toList();

        return new ProductLikeCommand.LikedProductsData(productLikes, likedProductItems);
    }

    @Transactional
    public ProductLikeCommand.LikeToggleResult toggleLikeWithResult(Long userId, Long productId) {
        ProductModel product = getProductById(productId);
        ProductLikeModel existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        
        ProductLikeService.LikeToggleResult result = productLikeService.toggleLike(product, userId, existingLike);
        
        if (result.isAdded()) {
            productLikeRepository.save(result.getLike());
        } else {
            productLikeRepository.delete(result.getLike());
        }
        
        productRepository.save(product);
        
        return new ProductLikeCommand.LikeToggleResult(result.isAdded(), result.isAdded());
    }

    @CacheEvict(value = "userLikedProducts", allEntries = true, beforeInvocation = true)
    @Transactional
    public void addProductLikeIdempotent(Long userId, Long productId) {
        addProductLike(userId, productId);
    }

    @CacheEvict(value = "userLikedProducts", allEntries = true, beforeInvocation = true)
    @Transactional
    public void removeProductLikeIdempotent(Long userId, Long productId) {
        removeProductLike(userId, productId);
    }
    
    private ProductModel getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
}
