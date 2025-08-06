package com.loopers.application.like;

import com.loopers.application.brand.BrandFacade;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.BrandLikeModel;
import com.loopers.domain.like.BrandLikeRepository;
import com.loopers.domain.like.BrandLikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BrandLikeFacade {
    
    private final BrandLikeRepository brandLikeRepository;
    private final BrandRepository brandRepository;
    private final BrandFacade brandFacde;
    private final BrandLikeService brandLikeService;

    public BrandLikeFacade(BrandLikeRepository brandLikeRepository,
                           BrandRepository brandRepository, BrandFacade brandFacde,
                           BrandLikeService brandLikeService) {
        this.brandLikeRepository = brandLikeRepository;
        this.brandRepository = brandRepository;
        this.brandFacde = brandFacde;
        this.brandLikeService = brandLikeService;
    }
    
    public void toggleBrandLike(Long userId, Long brandId) {
        BrandModel brand = getBrandById(brandId);
        var existingLike = brandLikeRepository.findByUserIdAndBrandId(userId, brandId).orElse(null);
        var result = brandLikeService.toggleLike(brand, userId, existingLike);

        if (result.isAdded()) {
            brandLikeRepository.save(result.getLike());
        } else {
            brandLikeRepository.delete(result.getLike());
        }
        brandRepository.save(brand);
    }
    
    public BrandLikeModel addBrandLike(Long userId, Long brandId) {
        if (brandLikeRepository.existsByUserIdAndBrandId(userId, brandId)) {
            return brandLikeRepository.findByUserIdAndBrandId(userId, brandId).get();
        }
        
        BrandModel brand = getBrandById(brandId);
        var newLike = brandLikeService.addLike(brand, userId);
        brandLikeRepository.save(newLike);
        brandRepository.save(brand);
        return newLike;
    }
    
    public void removeBrandLike(Long userId, Long brandId) {
        var existingLike = brandLikeRepository.findByUserIdAndBrandId(userId, brandId);
        if (existingLike.isPresent()) {
            BrandModel brand = getBrandById(brandId);
            brandLikeService.removeLike(brand, existingLike.get());
            brandLikeRepository.delete(existingLike.get());
            brandRepository.save(brand);
        }
    }
    
    @Transactional(readOnly = true)
    public boolean isBrandLiked(Long userId, Long brandId) {
        return brandLikeRepository.existsByUserIdAndBrandId(userId, brandId);
    }
    
    private BrandModel getBrandById(Long brandId) {
        return brandFacde.getByIdOrThrow(brandId);
    }
}
