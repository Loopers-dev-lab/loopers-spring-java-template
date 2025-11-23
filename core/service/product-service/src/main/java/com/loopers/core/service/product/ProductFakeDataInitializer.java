package com.loopers.core.service.product;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Profile("local")
public class ProductFakeDataInitializer {

    private static final int BATCH_SIZE = 5000;
    private static final int TOTAL_COUNT = 100_000;
    // 상품명 목록
    private static final String[] PRODUCT_NAMES = {
            "노트북", "마우스", "키보드", "모니터", "헤드폰",
            "스피커", "충전기", "케이블", "거치대", "패드",
            "카메라", "렌즈", "삼각대", "조명", "반사판",
            "마이크", "인터페이스", "케이스", "가방", "스탠드"
    };
    private final ProductRepository productRepository;
    private final Random random = new Random();

    public ProductFakeDataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void initializeBulkData() {
        long startTime = System.currentTimeMillis();
        System.out.println("🚀 대량 상품 데이터 초기화 시작 (" + TOTAL_COUNT + "건)...");

        List<Product> batch = new ArrayList<>();

        for (int i = 1; i <= TOTAL_COUNT; i++) {
            Product product = Product.create(
                    createRandomBrandId(),
                    createRandomProductName(i),
                    new ProductPrice(new BigDecimal(generateRandomPrice())),
                    new ProductStock(generateRandomStock())
            );

            batch.add(product);

            if (i % BATCH_SIZE == 0) {
                productRepository.bulkSaveOrUpdate(batch);
                batch.clear();
                System.out.println("✓ " + i + "/" + TOTAL_COUNT + " 진행 완료");
            }
        }

        if (!batch.isEmpty()) {
            productRepository.bulkSaveOrUpdate(batch);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("✅ 대량 상품 데이터 초기화 완료! (" + (endTime - startTime) / 1000.0 + "초)");
    }

    private BrandId createRandomBrandId() {
        // 10개의 브랜드 중 랜덤 선택
        int brandIndex = random.nextInt(10) + 1;
        return new BrandId(Integer.toString(brandIndex));
    }

    private ProductName createRandomProductName(int index) {
        String baseName = PRODUCT_NAMES[index % PRODUCT_NAMES.length];
        return new ProductName(baseName + " #" + index);
    }

    private long generateRandomPrice() {
        // 10,000 ~ 5,000,000 범위의 가격
        return 10_000L + (long) random.nextInt(4_990_000);
    }

    private long generateRandomStock() {
        // 10 ~ 10,000 범위의 재고
        return 10L + random.nextInt(9_990);
    }
}
