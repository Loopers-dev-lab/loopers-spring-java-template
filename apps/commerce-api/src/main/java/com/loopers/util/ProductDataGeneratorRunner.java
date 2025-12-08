package com.loopers.util;

import net.datafaker.Faker;
import java.math.BigDecimal;
import java.util.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandDomainCreateRequest;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.product.ProductEntity;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 및 브랜드 데이터 생성 Runner
 * 애플리케이션 시작 시 자동으로 브랜드 및 상품 데이터를 생성합니다
 * <p>
 * 생성 규칙:
 * <p>
 * 상품: 정확히 100,000개
 * 브랜드: 유니크 이름으로 필요한 만큼 자동 생성
 * <p>
 * <p>
 * 실행 방법:
 * --args='--spring.profiles.active=local --product.data.generate=true'
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "product.data.generate", havingValue = "true")
public class ProductDataGeneratorRunner implements CommandLineRunner {

    private final ProductJpaRepository productRepository;
    private final BrandJpaRepository brandRepository;
    private final Faker faker = new Faker(new Locale("ko"));
    private final Random random = new Random();

    private static final int BATCH_SIZE = 1000;
    private static final int TOTAL_PRODUCTS = 100; //  정확히 10만 개
    private static final int INITIAL_BRAND_COUNT = 5; // 초기 브랜드 개수

    private final Set<String> generatedBrandNames = new HashSet<>();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            log.info("데이터 생성 시작");

            // 1. 브랜드 데이터 생성
            List<BrandEntity> brands = generateAndSaveBrands();
            log.info("브랜드 생성 완료: {}개", brands.size());

            // 2. 정확히 100,000개의 상품 생성
            generateAndSaveProducts(brands);

            long duration = System.currentTimeMillis() - startTime;
            log.info(" 데이터 생성 완료 - 소요 시간: {}ms ({}초)", duration, duration / 1000);

        } catch (Exception e) {
            log.error("데이터 생성 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 브랜드 데이터를 생성하고 저장합니다.
     * 중복된 브랜드명을 방지합니다.
     */
    private List<BrandEntity> generateAndSaveBrands() {
        List<BrandEntity> brands = new ArrayList<>();

        int createdCount = 0;
        int attemptCount = 0;
        int maxAttempts = INITIAL_BRAND_COUNT * 10; // 무한 루프 방지

        while (createdCount < INITIAL_BRAND_COUNT && attemptCount < maxAttempts) {
            attemptCount++;

            //  유니크한 브랜드명 생성
            String brandName = generateUniqueBrandName();

            if (brandName == null) {
                log.warn("유니크한 브랜드명 생성에 실패했습니다. 시도: {}/{}", attemptCount, maxAttempts);
                continue;
            }

            // 브랜드 생성
            BrandEntity brand = BrandEntity.createBrandEntity(
                    new BrandDomainCreateRequest(
                            brandName,
                            faker.lorem().paragraph()
                    )
            );

            brands.add(brand);
            createdCount++;

            // 배치 단위로 저장
            if (brands.size() >= BATCH_SIZE) {
                brandRepository.saveAll(brands);
                log.info("브랜드 배치 저장: {}개 (누적: {}개)", brands.size(), createdCount);
                brands.clear();
            }
        }

        // 남은 데이터 저장
        if (!brands.isEmpty()) {
            brandRepository.saveAll(brands);
            log.info("브랜드 배치 저장: {}개 (누적: {}개)", brands.size(), createdCount);
        }

        if (createdCount < INITIAL_BRAND_COUNT) {
            log.warn("요청한 브랜드 개수만큼 생성되지 않았습니다. 생성: {}개, 요청: {}개",
                    createdCount, INITIAL_BRAND_COUNT);
        }

        //  저장된 모든 브랜드 반환
        return brandRepository.findAll();
    }

    /**
     * 유니크한 브랜드명을 생성합니다.
     * <p>
     * 중복된 이름이 생성되면 재시도합니다.
     *
     * @return 유니크한 브랜드명 (생성 실패 시 null)
     */
    private String generateUniqueBrandName() {
        int maxRetries = 50;

        for (int i = 0; i < maxRetries; i++) {
            String brandName = faker.company().name();

            //  중복 검사 (메모리 + 데이터베이스)
            if (!generatedBrandNames.contains(brandName) &&
                    !brandRepository.existsByNameAndDeletedAtNull(brandName)) {

                generatedBrandNames.add(brandName);
                return brandName;
            }
        }

        log.debug("{}번 시도 후에도 유니크한 브랜드명을 생성하지 못했습니다.", maxRetries);
        return null;
    }

    /**
     * 정확히 100,000개의 상품 데이터를 생성하고 저장합니다.
     * 브랜드 리스트에서 라운드로빈으로 선택하여 모든 브랜드에 상품이 분배됩니다.
     */
    private void generateAndSaveProducts(List<BrandEntity> brands) {
        if (brands.isEmpty()) {
            log.error("생성할 브랜드가 없습니다. 상품 생성을 중단합니다.");
            return;
        }

        List<ProductEntity> products = new ArrayList<>();
        int totalProducts = 0;
        int brandIndex = 0;

        log.info("상품 생성 시작: {}개 (브랜드별로 분배됨)", TOTAL_PRODUCTS);

        //  정확히 100,000개 생성
        for (int i = 0; i < TOTAL_PRODUCTS; i++) {
            // 라운드로빈으로 브랜드 선택 (모든 브랜드에 균등 분배)
            BrandEntity brand = brands.get(brandIndex % brands.size());
            brandIndex++;

            ProductEntity product = createRandomProduct(brand.getId());
            products.add(product);

            // 배치 단위로 저장
            if (products.size() >= BATCH_SIZE) {
                productRepository.saveAll(products);
                totalProducts += products.size();

                // 진행 상황 표시 (10% 단위)
                int progressPercent = (totalProducts * 100) / TOTAL_PRODUCTS;
                if (progressPercent % 10 == 0) {
                    log.info("상품 생성 진행률: {}% ({}/{}개)",
                            progressPercent, totalProducts, TOTAL_PRODUCTS);
                }

                products.clear();
            }
        }

        // 남은 데이터 저장
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
            totalProducts += products.size();
        }

        //  최종 생성 개수 검증
        long dbProductCount = productRepository.count();
        log.info(" 상품 생성 완료: {}개 (DB 확인: {}개)", totalProducts, dbProductCount);

        if (dbProductCount != TOTAL_PRODUCTS) {
            log.warn("⚠️ 상품 개수 불일치 - 요청: {}개, DB: {}개", TOTAL_PRODUCTS, dbProductCount);
        }
    }

    /**
     * 무작위 상품을 생성합니다.
     */
    private ProductEntity createRandomProduct(Long brandId) {
        String productName = faker.commerce().productName();
        String description = faker.lorem().sentence();
        BigDecimal originPrice = generateRandomPrice(10000, 500000);
        BigDecimal discountPrice = random.nextDouble() > 0.5
                ? originPrice.multiply(BigDecimal.valueOf(random.nextDouble() * 0.8 + 0.1))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                : null;
        int stockQuantity = random.nextInt(1000) + 1;

        return new ProductEntity(
                brandId,
                productName,
                description,
                originPrice,
                discountPrice,
                stockQuantity
        );
    }

    /**
     * 범위 내의 무작위 가격을 생성합니다.
     */
    private BigDecimal generateRandomPrice(int minPrice, int maxPrice) {
        return BigDecimal.valueOf(random.nextInt(maxPrice - minPrice + 1) + minPrice);
    }
}
