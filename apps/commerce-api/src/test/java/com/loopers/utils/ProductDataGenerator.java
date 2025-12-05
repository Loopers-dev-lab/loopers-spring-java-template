package com.loopers.utils;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@Slf4j
public class ProductDataGenerator {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final Faker faker;

    public ProductDataGenerator(ProductRepository productRepository,
                                BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.faker = new Faker(Locale.ENGLISH);
    }

    @Transactional
    public void generate100KProducts() {

        // 1. 브랜드 먼저 생성
        List<Brand> brands = generateBrands(100);

        int totalProducts = 100_000;
        int batchSize = 1000;


        for (int i = 0; i < totalProducts; i += batchSize) {
            List<Product> batch = new ArrayList<>();

            for (int j = 0; j < batchSize && (i + j) < totalProducts; j++) {
                int currentIndex = i + j;

                Brand brand = selectBrandWithDistribution(brands, currentIndex);
                Long price = generateDistributedPrice();

                Product product = Product.create(
                        generateUniqueProductName(currentIndex),
                        price,
                        faker.number().numberBetween(0, 1000),
                        brand
                );

                int likeCount = generateRealisticLikeCount();
                for (int k = 0; k < likeCount; k++) {
                    product.incrementLikeCount();
                }

                batch.add(product);
            }

            productRepository.saveAll(batch);

            if ((i + batchSize) % 10_000 == 0) {
                System.out.printf("  진행: %d / %d (%.1f %%)\n",
                        i + batchSize,
                        totalProducts,
                        (i + batchSize) * 100.0 / totalProducts
                );
            }
        }

        System.out.println("=== 데이터 생성 완료 ===");
        logDataDistribution();
    }

    private List<Brand> generateBrands(int count) {
        List<Brand> brands = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String brandName;
            int attempts = 0;

            do {
                if (attempts > 5) {
                    brandName = faker.company().name() + " Inc. " + i;
                } else {
                    brandName = faker.company().name();
                }
                attempts++;
            } while (!usedNames.add(brandName) && attempts < 10);

            Brand brand = Brand.create(brandName);
            brands.add(brandRepository.save(brand));
        }

        return brands;
    }

    private Brand selectBrandWithDistribution(List<Brand> brands, int productIndex) {
        int random = faker.number().numberBetween(1, 100);

        if (random <= 80) {
            int topBrandCount = brands.size() / 5;
            return brands.get(faker.number().numberBetween(0, topBrandCount));
        } else {
            int topBrandCount = brands.size() / 5;
            return brands.get(faker.number().numberBetween(topBrandCount, brands.size()));
        }
    }

    private Long generateDistributedPrice() {
        int tier = faker.number().numberBetween(1, 100);

        if (tier <= 30) {
            // 저가 (1천원 ~ 1만원)
            return (long) faker.number().numberBetween(1_000, 10_000);
        } else if (tier <= 70) {
            // 중가 (1만원 ~ 10만원)
            return (long) faker.number().numberBetween(10_000, 100_000);
        } else if (tier <= 95) {
            // 고가 (10만원 ~ 100만원)
            return (long) faker.number().numberBetween(100_000, 1_000_000);
        } else {
            // 프리미엄 (100만원 ~ 500만원)
            return (long) faker.number().numberBetween(1_000_000, 5_000_000);
        }
    }

    private int generateRealisticLikeCount() {
        int random = faker.number().numberBetween(1, 100);

        if (random <= 60) {
            // 60%: 좋아요 거의 없음 (0-10)
            return faker.number().numberBetween(0, 10);
        } else if (random <= 85) {
            // 25%: 보통 인기 (10-100)
            return faker.number().numberBetween(10, 100);
        } else if (random <= 95) {
            // 10%: 인기 상품 (100-1000)
            return faker.number().numberBetween(100, 1000);
        } else {
            // 5%: 매우 인기 (1000-10000)
            return faker.number().numberBetween(1000, 10000);
        }
    }

    private String generateUniqueProductName(int index) {
        String[] categories = {"Electronics", "Fashion", "Home", "Sports", "Beauty", "Books"};
        String[] prefixes = {"Premium", "Ultra", "Pro", "Max", "Super", "Deluxe", "Classic", "Modern"};

        String category = categories[index % categories.length];
        String prefix = prefixes[index % prefixes.length];

        return String.format("%s %s %s #%d",
                prefix,
                category,
                faker.commerce().productName(),
                index
        );
    }

    /**
     * 데이터 분포 확인용 로그
     */
    private void logDataDistribution() {
        System.out.println("=== 데이터 분포 확인 ===");

        // 브랜드별 상품 수 확인 (상위 5개)
        System.out.println("브랜드별 상품 수 (상위 5개):");
        // SQL: SELECT brand_id, COUNT(*) FROM products GROUP BY brand_id ORDER BY COUNT(*) DESC LIMIT 5

        // 가격대별 상품 수
        System.out.println("가격대별 분포:");
        // SQL: SELECT
        //   CASE
        //     WHEN price_value < 10000 THEN '저가'
        //     WHEN price_value < 100000 THEN '중가'
        //     WHEN price_value < 1000000 THEN '고가'
        //     ELSE '프리미엄'
        //   END as tier,
        //   COUNT(*) as count
        // FROM products GROUP BY tier

        // 좋아요 수 분포
        System.out.println("좋아요 수 분포:");
        // SQL: SELECT
        //   CASE
        //     WHEN like_count < 10 THEN '0-10'
        //     WHEN like_count < 100 THEN '10-100'
        //     WHEN like_count < 1000 THEN '100-1000'
        //     ELSE '1000+'
        //   END as tier,
        //   COUNT(*) as count
        // FROM products GROUP BY tier
    }
}
