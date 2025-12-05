package com.loopers.performance.data;

import com.github.javafaker.Faker;
import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.product.ProductCreateRequest;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.supply.vo.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class ProductDataGenerator {
    private final BrandFacade brandFacade;
    private final ProductFacade productFacade;
    private final Faker faker = new Faker();
    private final Random random = new Random();

    @Autowired
    public ProductDataGenerator(BrandFacade brandFacade, ProductFacade productFacade) {
        this.brandFacade = brandFacade;
        this.productFacade = productFacade;
    }

    @Transactional
    public List<Long> generateProducts(int totalCount, int brandCount, int batchSize) {
        System.out.println("상품 데이터 생성 시작: 총 " + totalCount + "개, 브랜드 " + brandCount + "개");
        long startTime = System.currentTimeMillis();

        // 1. 브랜드 생성 (없는 경우)
        List<Long> brandIds = ensureBrandsExist(brandCount, batchSize);

        // 2. 배치로 상품 생성
        List<Long> productIds = new ArrayList<>();
        List<ProductCreateRequest> batch = new ArrayList<>();

        for (int i = 0; i < totalCount; i++) {
            Long brandId = brandIds.get(random.nextInt(brandIds.size()));
            String productName = generateProductName();
            Price price = generatePrice();
            Stock stock = generateStock();
            Integer likeCount = generateLikeCount();

            ProductCreateRequest request = new ProductCreateRequest(productName, brandId, price, stock, likeCount);
            batch.add(request);
            // 배치 크기 도달 시 저장
            if (batch.size() >= batchSize) {
                List<ProductInfo> saved = productFacade.createProductBulk(batch);
                productIds.addAll(saved.stream().map(ProductInfo::id).toList());
                batch.clear();

                if (productIds.size() % 10_000 == 0) {
                    System.out.println("진행 상황: " + productIds.size() + " / " + totalCount + " 완료");
                }
            }
        }

        // 남은 배치 저장
        if (!batch.isEmpty()) {
            List<ProductInfo> saved = productFacade.createProductBulk(batch);
            productIds.addAll(saved.stream().map(ProductInfo::id).toList());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("상품 데이터 생성 완료: " + productIds.size() + "개 (소요 시간: " + elapsed + "ms, 평균: " + (elapsed / (double) productIds.size()) + "ms/개)");

        return productIds;
    }

    private List<Long> ensureBrandsExist(int brandCount, int batchSize) {
        Page<BrandInfo> allBrands = brandFacade.getAllBrands(Pageable.ofSize(brandCount));
        List<Long> brandIds = new ArrayList<>();
        brandIds.addAll(allBrands.map(BrandInfo::id).toList());
        int existingCount = allBrands.getNumberOfElements();
        if (existingCount == brandCount) {
            return brandIds;
        }

        int needToCreate = brandCount - existingCount;
        System.out.println("브랜드 " + needToCreate + "개 부족, " + needToCreate + "개 생성 시작");

        List<String> newBrandNames = new ArrayList<>();
        for (int i = 0; i < needToCreate; i++) {
            String brandName = faker.company().name();
            newBrandNames.add(brandName);

            if (newBrandNames.size() >= batchSize) {
                List<BrandInfo> createdBrands = brandFacade.createBrandInfoBulk(newBrandNames);
                brandIds.addAll(createdBrands.stream().map(BrandInfo::id).toList());
                newBrandNames.clear();
            }
        }
        // 남은 브랜드 생성
        if (!newBrandNames.isEmpty()) {
            List<BrandInfo> createdBrands = brandFacade.createBrandInfoBulk(newBrandNames);
            brandIds.addAll(createdBrands.stream().map(BrandInfo::id).toList());
        }
        System.out.println("브랜드 " + brandIds.size() + "개 준비 완료");
        return brandIds;
    }

    /**
     * Faker를 사용하여 상품명 생성
     */
    private String generateProductName() {
        return faker.commerce().productName();
    }

    /**
     * 다양한 가격 분포 생성
     * 로그 정규 분포를 사용하여 실제 이커머스와 유사한 분포 생성
     *
     * @return 1, 000원 ~ 1,000,000원 범위의 가격
     */
    private Price generatePrice() {
        int minPrice = 1_000;
        int maxPrice = 1_000_000;
        double logMin = Math.log10(minPrice);
        double logMax = Math.log10(maxPrice);
        double logPrice = logMin + random.nextDouble() * (logMax - logMin);
        return new Price((int) Math.pow(10, logPrice));
    }

    private Stock generateStock() {
        double rand = random.nextDouble();
        if (rand < 0.01) {
            return new Stock(0); // 1%: 품절
        } else if (rand < 0.7) {
            return new Stock(random.nextInt(100) + 1); // 69%: 1~100
        } else if (rand < 0.95) {
            return new Stock(100 + random.nextInt(400)); // 25%: 100~500
        } else {
            return new Stock(500 + random.nextInt(500)); // 5%: 500~1000
        }
    }

    private Integer generateLikeCount() {
        double rand = random.nextDouble();
        if (rand < 0.5) {
            return random.nextInt(10); // 50%: 0~9
        } else if (rand < 0.8) {
            return 10 + random.nextInt(40); // 30%: 10~49
        } else if (rand < 0.95) {
            return 50 + random.nextInt(150); // 15%: 50~199
        } else {
            return 200 + random.nextInt(800); // 5%: 200~999
        }
    }

}

