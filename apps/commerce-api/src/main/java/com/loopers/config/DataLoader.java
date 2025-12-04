package com.loopers.config;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local") // local 프로파일에서만 실행
public class DataLoader implements CommandLineRunner {

    private final BrandJpaRepository brandRepository;
    private final ProductJpaRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터가 있으면 스킵
        if (brandRepository.count() > 0) {
            log.info("데이터가 이미 존재합니다. 더미 데이터 생성을 스킵합니다.");
            return;
        }

        log.info("더미 데이터 생성을 시작합니다...");

        // 1. 브랜드 10개 생성
        List<Brand> brands = createBrands();
        log.info("브랜드 {}개 생성 완료", brands.size());

        // 2. 상품 50만개 생성 (배치로 처리)
        createProducts(brands);
        log.info("상품 생성 완료");

        log.info("더미 데이터 생성이 완료되었습니다.");
    }

    private List<Brand> createBrands() {
        List<Brand> brands = new ArrayList<>();
        String[] brandNames = {"Brand A", "Brand B", "Brand C", "Brand D", "Brand E",
                "Brand F", "Brand G", "Brand H", "Brand I", "Brand J"};

        for (String brandName : brandNames) {
            Brand brand = Brand.createBrand(brandName);
            brands.add(brand);
        }

        return brandRepository.saveAll(brands);
    }

    private void createProducts(List<Brand> brands) {
        int totalProducts = 500000; // 50만개
        int batchSize = 1000; // 배치 크기
        Random random = new Random();

        for (int i = 0; i < totalProducts; i += batchSize) {
            List<Product> products = new ArrayList<>();

            int end = Math.min(i + batchSize, totalProducts);
            for (int j = i; j < end; j++) {
                String productCode = String.format("P%07d", j + 1);
                String productName = "Product " + (j + 1);
                int stock = random.nextInt(1000); // 0 ~ 999
                BigDecimal price = BigDecimal.valueOf(random.nextInt(100000) + 100); // 100 ~ 100100
                Brand randomBrand = brands.get(random.nextInt(brands.size()));

                Product product = Product.createProduct(
                        productCode,
                        productName,
                        Money.of(price),
                        stock,
                        randomBrand
                );
                products.add(product);
            }

            productRepository.saveAll(products);

            if ((i + batchSize) % 10000 == 0) {
                log.info("상품 {}개 생성 중...", i + batchSize);
            }
        }
    }
}
