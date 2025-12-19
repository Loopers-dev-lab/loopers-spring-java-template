package com.loopers.domain.product.event;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * ProductView 관련 이벤트 핸들러
 * ProductView Read Model의 CRUD 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewEventHandler {

    private final ProductViewRepository productViewRepository;
    private final BrandRepository brandRepository;

    @Transactional
    public void handleCreated(ProductEvents.Created event) {
        log.info("ProductViewEventHandler: ProductEvents.Created 처리 - productId: {}", event.productId());

        // Brand 조회하여 brandName 가져오기
        String brandName = brandRepository.findById(event.brandId())
                .map(Brand::getName)
                .orElse(null);

        // ProductView 생성
        ProductView productView = ProductView.builder()
                .id(event.productId())
                .name(event.name())
                .price(event.price())
                .brandId(event.brandId())
                .brandName(brandName)
                .status(event.status())
                .likeCount(0L)
                .createdAt(ZonedDateTime.of(event.getOccurredAt(), ZoneId.systemDefault()))
                .build();

        productViewRepository.save(productView);
        log.debug("ProductView 생성 완료 - productId: {}", event.productId());
    }

    @Transactional
    public void handleUpdated(ProductEvents.Updated event) {
        log.info("ProductViewEventHandler: ProductEvents.Updated 처리 - productId: {}", event.productId());

        // Brand 조회하여 brandName 가져오기
        String brandName = brandRepository.findById(event.brandId())
                .map(Brand::getName)
                .orElse(null);

        // ProductView 업데이트
        productViewRepository.update(
                event.productId(),
                event.name(),
                event.price(),
                event.brandId(),
                brandName,
                event.status()
        );
        log.debug("ProductView 업데이트 완료 - productId: {}", event.productId());
    }

    @Transactional
    public void handleDeleted(ProductEvents.Deleted event) {
        log.info("ProductViewEventHandler: ProductEvents.Deleted 처리 - productId: {}", event.productId());

        productViewRepository.deleteById(event.productId());
        log.debug("ProductView 삭제 완료 - productId: {}", event.productId());
    }
}

