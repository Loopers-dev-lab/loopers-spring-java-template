package com.loopers.infrastructure.init;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Profile("data-init")
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    private static final int BRAND_COUNT = 100;
    private static final int PRODUCT_COUNT = 100_000;
    private static final int BATCH_SIZE = 1000;

    public DataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println("========== 테스트 데이터 생성 시작 ==========");

        long startTime = System.currentTimeMillis();

        initBrands();
        initProducts();

        long endTime = System.currentTimeMillis();
        System.out.println("========== 테스트 데이터 생성 완료 ==========");
        System.out.println("소요 시간: " + (endTime - startTime) / 1000.0 + "초");
    }

    private void initBrands() {
        System.out.println("브랜드 " + BRAND_COUNT + "개 생성 중...");

        String sql = "INSERT INTO brands (name, active, created_at, updated_at) VALUES (?, ?, ?, ?)";
        Timestamp now = Timestamp.from(ZonedDateTime.now().toInstant());

        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 1; i <= BRAND_COUNT; i++) {
            batchArgs.add(new Object[]{
                    "브랜드_" + i,
                    true,
                    now,
                    now
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
        System.out.println("브랜드 생성 완료!");
    }

    private void initProducts() {
        System.out.println("상품 " + PRODUCT_COUNT + "개 생성 중...");

        String sql = "INSERT INTO products (name, description, price, brand_id, stock, total_likes, version, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int totalInserted = 0;
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            Timestamp createdAt = randomTimestamp();

            batchArgs.add(new Object[]{
                    "상품_" + i,
                    "상품 설명 " + i,
                    randomPrice(),
                    randomBrandId(),
                    randomStock(),
                    randomLikes(),
                    0L,
                    createdAt,
                    createdAt
            });

            if (batchArgs.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                totalInserted += batchArgs.size();
                batchArgs.clear();

                if (totalInserted % 10000 == 0) {
                    System.out.println("  진행률: " + totalInserted + " / " + PRODUCT_COUNT);
                }
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
            totalInserted += batchArgs.size();
        }

        System.out.println("상품 생성 완료! 총 " + totalInserted + "개");
    }

    private long randomPrice() {
        // 1,000원 ~ 1,000,000원 (다양한 분포)
        int[] priceRanges = {1000, 5000, 10000, 30000, 50000, 100000, 300000, 500000, 1000000};
        int basePrice = priceRanges[random.nextInt(priceRanges.length)];
        return basePrice + random.nextInt(10000);
    }

    private long randomBrandId() {
        // 1 ~ BRAND_COUNT
        return random.nextInt(BRAND_COUNT) + 1;
    }

    private long randomStock() {
        // 0 ~ 1000 (일부는 품절)
        if (random.nextInt(100) < 5) {
            return 0; // 5% 확률로 품절
        }
        return random.nextInt(1000) + 1;
    }

    private long randomLikes() {
        // 0 ~ 10000 (롱테일 분포)
        double rand = random.nextDouble();
        if (rand < 0.7) {
            return random.nextInt(100);        // 70%: 0~99
        } else if (rand < 0.9) {
            return random.nextInt(1000);       // 20%: 0~999
        } else {
            return random.nextInt(10000);      // 10%: 0~9999
        }
    }

    private Timestamp randomTimestamp() {
        // 최근 1년 내 랜덤 날짜
        long now = System.currentTimeMillis();
        long oneYearAgo = now - (365L * 24 * 60 * 60 * 1000);
        long randomTime = oneYearAgo + (long) (random.nextDouble() * (now - oneYearAgo));
        return new Timestamp(randomTime);
    }
}
