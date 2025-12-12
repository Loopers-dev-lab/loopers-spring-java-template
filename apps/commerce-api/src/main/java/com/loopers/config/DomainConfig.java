package com.loopers.config;

import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductDetailService;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.brand.BrandRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 도메인 서비스 빈 등록
 */
@Configuration
public class DomainConfig {

    @Bean
    public CouponService couponService(CouponRepository couponRepository) {
        return new CouponService(couponRepository);
    }

    @Bean
    public OrderService orderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            com.loopers.domain.user.UserRepository userRepository,
            PointService pointService
    ) {
        return new OrderService(productRepository, userRepository, orderRepository, pointService);
    }

    @Bean
    public ProductDetailService productDetailService(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            LikeService likeService
    ) {
        return new ProductDetailService(productRepository, brandRepository, likeService);
    }

    @Bean
    public LikeService likeService(
            LikeRepository likeRepository,
            ProductRepository productRepository
    ) {
        return new LikeService(likeRepository, productRepository);
    }

    @Bean
    public PointService pointService(
            PointRepository pointRepository,
            com.loopers.domain.user.UserRepository userRepository
    ) {
        return new PointService(pointRepository, userRepository);
    }
}
