package com.loopers.fixtures;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;

/**
 * 상품 테스트 픽스처
 *
 * 테스트에서 사용할 상품 엔티티 및 요청 객체를 생성합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public class ProductTestFixture {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    /**
     * ProductDomainCreateRequest를 생성합니다 (할인가 없음).
     *
     * @param brandId 브랜드 ID
     * @param name 상품명
     * @param description 상품 설명
     * @param originPrice 정가
     * @param stockQuantity 재고 수량
     * @return ProductDomainCreateRequest
     */
    public static ProductDomainCreateRequest createRequest(
            Long brandId,
            String name,
            String description,
            BigDecimal originPrice,
            Integer stockQuantity
    ) {
        return ProductDomainCreateRequest.of(brandId, name, description, originPrice, stockQuantity);
    }

    /**
     * ProductDomainCreateRequest를 생성합니다 (할인가 포함).
     *
     * @param brandId 브랜드 ID
     * @param name 상품명
     * @param description 상품 설명
     * @param originPrice 정가
     * @param discountPrice 할인가
     * @param stockQuantity 재고 수량
     * @return ProductDomainCreateRequest
     */
    public static ProductDomainCreateRequest createRequest(
            Long brandId,
            String name,
            String description,
            BigDecimal originPrice,
            BigDecimal discountPrice,
            Integer stockQuantity
    ) {
        return ProductDomainCreateRequest.of(brandId, name, description, originPrice, discountPrice, stockQuantity);
    }

    /**
     * 브랜드 엔티티로 상품을 생성합니다.
     *
     * @param brand 브랜드 엔티티
     * @return ProductEntity
     */
    public static ProductEntity createEntity(BrandEntity brand) {
        return createEntity(
                brand.getId(),
                "상품" + ID_GENERATOR.getAndIncrement(),
                "상품 설명",
                new BigDecimal("10000"),
                100
        );
    }

    /**
     * ProductEntity를 생성합니다.
     *
     * @param brandId 브랜드 ID
     * @param name 상품명
     * @param description 상품 설명
     * @param price 정가
     * @param stock 재고 수량
     * @return ProductEntity
     */
    public static ProductEntity createEntity(
            Long brandId,
            String name,
            String description,
            BigDecimal price,
            int stock
    ) {
        ProductDomainCreateRequest request = createRequest(brandId, name, description, price, stock);
        return ProductEntity.createEntity(request);
    }

    /**
     * 상품을 생성하고 저장합니다.
     *
     * @param productRepository 상품 레포지토리
     * @param brand 브랜드 엔티티
     * @param name 상품명
     * @param description 상품 설명
     * @param price 정가
     * @param stock 재고 수량
     * @return 저장된 ProductEntity
     */
    public static ProductEntity createAndSave(
            ProductRepository productRepository,
            BrandEntity brand,
            String name,
            String description,
            BigDecimal price,
            int stock
    ) {
        ProductEntity product = createEntity(brand.getId(), name, description, price, stock);
        return productRepository.save(product);
    }

    /**
     * 상품을 생성하고 저장합니다 (기본값 사용).
     *
     * @param productRepository 상품 레포지토리
     * @param brand 브랜드 엔티티
     * @return 저장된 ProductEntity
     */
    public static ProductEntity createAndSave(
            ProductRepository productRepository,
            BrandEntity brand
    ) {
        return createAndSave(
                productRepository,
                brand,
                "상품" + ID_GENERATOR.getAndIncrement(),
                "상품 설명",
                new BigDecimal("10000"),
                100
        );
    }

    /**
     * 브랜드와 상품을 생성하는 헬퍼 메서드
     *
     * @param brandRepository 브랜드 리포지토리
     * @param productRepository 상품 리포지토리
     * @param brandCount 브랜드 수
     * @param productCountPerBrand 브랜드당 상품 수
     */
    public static void createBrandsAndProducts(
            BrandRepository brandRepository,
            ProductRepository productRepository,
            int brandCount,
            int productCountPerBrand
    ) {
        List<BrandEntity> brands = BrandTestFixture.createEntities(brandCount)
                .stream()
                .map(brandRepository::save)
                .toList();

        brands.forEach(brand -> {
            IntStream.range(0, productCountPerBrand).forEach(i -> {
                ProductTestFixture.createAndSave(productRepository, brand);
            });
        });
    }
}
