package com.loopers.core.service.product;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

@Component
@Profile("local")
public class ProductFakeDataInitializer {

    private static final int BATCH_SIZE = 20_000;           // 배치 크기: 더 큼 = 더 적은 왕복
    private static final int TOTAL_COUNT = 100_000;
    private static final int NUM_THREADS = 8;               // 스레드: 로컬 MySQL은 과도한 동시성 피함
    private static final int ITEMS_PER_THREAD = TOTAL_COUNT / NUM_THREADS;
    private static final int PROGRESS_LOG_INTERVAL = 100_000; // 100,000개마다 로그 출력
    private static final String[] PRODUCT_NAMES = {
            "노트북", "마우스", "키보드", "모니터", "헤드폰",
            "스피커", "충전기", "케이블", "거치대", "패드",
            "카메라", "렌즈", "삼각대", "조명", "반사판",
            "마이크", "인터페이스", "케이스", "가방", "스탠드"
    };

    private final ProductRepository productRepository;
    private final Random random = new Random();
    private volatile long totalProcessed = 0;

    public ProductFakeDataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void initializeBulkData() {
        long startTime = System.currentTimeMillis();
        System.out.println("🚀 대량 상품 데이터 초기화 시작 (" + TOTAL_COUNT + "건)...");

        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int threadIndex = 0; threadIndex < NUM_THREADS; threadIndex++) {
            int startIndex = threadIndex * ITEMS_PER_THREAD + 1;
            int endIndex = (threadIndex == NUM_THREADS - 1) ? TOTAL_COUNT : (threadIndex + 1) * ITEMS_PER_THREAD;

            Thread.ofVirtual().start(() -> {
                try {
                    processProductRange(startIndex, endIndex);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ 초기화 작업이 중단되었습니다: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("✅ 대량 상품 데이터 초기화 완료! (" + (endTime - startTime) / 1000.0 + "초)");
    }

    private void processProductRange(int startIndex, int endIndex) {
        List<Product> batch = new ArrayList<>();

        for (int i = startIndex; i <= endIndex; i++) {
            batch.add(createProduct(i));

            if (batch.size() == BATCH_SIZE) {
                productRepository.bulkSaveOrUpdate(batch);
                totalProcessed += batch.size();

                if (totalProcessed % PROGRESS_LOG_INTERVAL == 0) {
                    System.out.println("✓ " + totalProcessed + "/" + TOTAL_COUNT + " 진행 완료");
                }
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            productRepository.bulkSaveOrUpdate(batch);
            totalProcessed += batch.size();
        }
    }

    private Product createProduct(int index) {
        return Product.mappedBy(
                ProductId.empty(),
                createRandomBrandId(),
                createRandomProductName(index),
                new ProductPrice(new BigDecimal(generateRandomPrice())),
                new ProductStock(generateRandomStock()),
                new ProductLikeCount(generateRandomLikeCount()),
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
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

    private long generateRandomLikeCount() {
        // 10 ~ 10,000 범위의 좋아요
        return random.nextInt(10_000);
    }
}
