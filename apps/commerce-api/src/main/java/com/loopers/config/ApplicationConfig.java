package com.loopers.config;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.product.ProductFacade;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductDetailService;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.cache.ProductCacheService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 파사드 빈 등록
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public ProductFacade productFacade(
            ProductDetailService productDetailService,
            ProductCacheService productCacheService,
            ProductRepository productRepository
    ) {
        return new ProductFacade(productDetailService, productCacheService, productRepository);
    }

    @Bean
    public LikeFacade likeFacade(
            LikeService likeService,
            ProductCacheService productCacheService,
            ApplicationEventPublisher eventPublisher
    ) {
        return new LikeFacade(likeService, productCacheService, eventPublisher);
    }
}
